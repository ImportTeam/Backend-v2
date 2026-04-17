package com.picsel.backend_v2.repository.ext;

import com.picsel.backend_v2.domain.ext.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query("SELECT COUNT(l) FROM SearchLog l")
    long countTotal();

    @Query("SELECT COUNT(l) FROM SearchLog l WHERE l.source = 'cache'")
    long countCacheHits();

    // limit 파라미터를 실제로 적용하기 위해 Pageable 사용 (JPQL은 LIMIT :x 미지원)
    @Query("""
        SELECT l.queryName, COUNT(l) as cnt
        FROM SearchLog l
        GROUP BY l.queryName
        ORDER BY cnt DESC
        """)
    List<Object[]> findPopularQueries(Pageable pageable);

    // 주간 통계
    @Query(value = """
        SELECT
          COUNT(*) AS total,
          SUM(CASE WHEN status IN ('HIT','SUCCESS') THEN 1 ELSE 0 END) AS successCount,
          AVG(elapsed_ms) AS avgElapsed,
          SUM(CASE WHEN source = 'cache' THEN 1 ELSE 0 END) AS cacheHits,
          SUM(CASE WHEN source = 'fastpath' AND status = 'SUCCESS' THEN 1 ELSE 0 END) AS fastpathSuccess,
          SUM(CASE WHEN source = 'slowpath' AND status = 'SUCCESS' THEN 1 ELSE 0 END) AS slowpathSuccess,
          SUM(CASE WHEN status = 'FAIL' THEN 1 ELSE 0 END) AS failures
        FROM search_logs
        WHERE created_at >= NOW() - INTERVAL :days DAY
        """, nativeQuery = true)
    Object[] getWeeklyStats(@Param("days") int days);

    // 소스별 성공률
    @Query(value = """
        SELECT source,
               COUNT(*) AS total,
               SUM(CASE WHEN status IN ('HIT','SUCCESS') THEN 1 ELSE 0 END) AS success,
               AVG(elapsed_ms) AS avgElapsed
        FROM search_logs
        WHERE created_at >= NOW() - INTERVAL :days DAY
        GROUP BY source
        """, nativeQuery = true)
    List<Object[]> getSuccessRateBySource(@Param("days") int days);

    // 실패 쿼리
    @Query(value = """
        SELECT query_name, COUNT(*) AS failCount, MAX(created_at) AS lastAttempt
        FROM search_logs
        WHERE status = 'FAIL'
          AND created_at >= NOW() - INTERVAL :days DAY
        GROUP BY query_name
        ORDER BY failCount DESC
        LIMIT :lim
        """, nativeQuery = true)
    List<Object[]> getFailedQueries(@Param("days") int days, @Param("lim") int limit);

    // 인기 쿼리 + 성공률
    @Query(value = """
        SELECT query_name,
               COUNT(*) AS totalCount,
               SUM(CASE WHEN status IN ('HIT','SUCCESS') THEN 1 ELSE 0 END) AS successCount
        FROM search_logs
        WHERE created_at >= NOW() - INTERVAL :days DAY
        GROUP BY query_name
        ORDER BY totalCount DESC
        LIMIT :lim
        """, nativeQuery = true)
    List<Object[]> getTrendingQueries(@Param("days") int days, @Param("lim") int limit);

    // 응답 시간 통계 (avg/min/max)
    @Query(value = """
        SELECT AVG(elapsed_ms), MIN(elapsed_ms), MAX(elapsed_ms)
        FROM search_logs
        WHERE created_at >= NOW() - INTERVAL :days DAY
          AND status != 'FAIL'
        """, nativeQuery = true)
    Object[] getPerformanceMetrics(@Param("days") int days);

    // 백분위수 (p50, p95, p99)
    @Query(value = """
        SELECT MAX(elapsed_ms)
        FROM (
          SELECT elapsed_ms,
                 PERCENT_RANK() OVER (ORDER BY elapsed_ms) AS pr
          FROM search_logs
          WHERE created_at >= NOW() - INTERVAL :days DAY
            AND status != 'FAIL'
        ) t
        WHERE pr <= :pct
        """, nativeQuery = true)
    Double getPercentile(@Param("days") int days, @Param("pct") double pct);

    // 절약 금액 분석
    @Query(value = """
        SELECT
          SUM(origin_price - found_price) AS totalSaved,
          AVG(origin_price - found_price) AS avgSaved,
          COUNT(*) AS cnt
        FROM search_logs
        WHERE origin_price IS NOT NULL
          AND found_price IS NOT NULL
          AND origin_price > found_price
          AND created_at >= NOW() - INTERVAL :days DAY
        """, nativeQuery = true)
    Object[] getPriceSavingsAnalysis(@Param("days") int days);

    // 사용자별 검색 이력 (대시보드)
    List<SearchLog> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    // 사용자별 잠재 절약 금액
    @Query("SELECT SUM(s.originPrice - s.foundPrice) FROM SearchLog s WHERE s.userUuid = :uuid AND s.foundPrice IS NOT NULL AND s.originPrice IS NOT NULL AND s.originPrice > s.foundPrice")
    Long sumPotentialSavings(@Param("uuid") String userUuid);

    // 사용자별 검색 건수
    long countByUserUuid(String userUuid);

    // 일별 스냅샷 (단일 날짜)
    @Query(value = """
        SELECT
          COUNT(*) AS total,
          SUM(CASE WHEN status IN ('HIT','SUCCESS') THEN 1 ELSE 0 END) AS success,
          SUM(CASE WHEN source = 'cache' THEN 1 ELSE 0 END) AS cacheHits,
          AVG(elapsed_ms) AS avgElapsed,
          SUM(CASE WHEN origin_price > found_price THEN origin_price - found_price ELSE 0 END) AS totalSaved
        FROM search_logs
        WHERE created_at >= NOW() - INTERVAL :days DAY
        """, nativeQuery = true)
    Object[] getDailySnapshot(@Param("days") int days);
}
