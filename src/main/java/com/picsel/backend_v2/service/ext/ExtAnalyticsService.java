package com.picsel.backend_v2.service.ext;

import com.picsel.backend_v2.domain.ext.SearchFailure;
import com.picsel.backend_v2.repository.ext.SearchFailureRepository;
import com.picsel.backend_v2.repository.ext.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtAnalyticsService {

    private final SearchLogRepository searchLogRepository;
    private final SearchFailureRepository searchFailureRepository;

    // ── 일별 스냅샷 ──────────────────────────────────────────────────────────

    public Map<String, Object> getDailySnapshot(int days) {
        Object[] row = searchLogRepository.getDailySnapshot(days);
        if (row == null) return Collections.emptyMap();

        long total       = toLong(row[0]);
        long success     = toLong(row[1]);
        long cacheHits   = toLong(row[2]);
        double avgMs     = toDouble(row[3]);
        double totalSaved = toDouble(row[4]);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSearches", total);
        result.put("successRate",   total > 0 ? round1(success * 100.0 / total) : 0.0);
        result.put("cacheHitRate",  total > 0 ? round1(cacheHits * 100.0 / total) : 0.0);
        result.put("avgResponseMs", Math.round(avgMs));
        result.put("totalSaved",    (long) totalSaved);
        result.put("days",          days);
        return result;
    }

    // ── 주간 리포트 ──────────────────────────────────────────────────────────

    public Map<String, Object> getWeeklyReport(int days) {
        Object[] row = searchLogRepository.getWeeklyStats(days);
        if (row == null) return Collections.emptyMap();

        long total           = toLong(row[0]);
        long successCount    = toLong(row[1]);
        double avgElapsed    = toDouble(row[2]);
        long cacheHits       = toLong(row[3]);
        long failures        = toLong(row[6]);

        List<Map<String, Object>> sourceStats = buildSourceStats(
                searchLogRepository.getSuccessRateBySource(days));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period",        days + "일");
        result.put("totalSearches", total);
        result.put("successCount",  successCount);
        result.put("successRate",   total > 0 ? round1(successCount * 100.0 / total) : 0.0);
        result.put("cacheHitRate",  total > 0 ? round1(cacheHits * 100.0 / total) : 0.0);
        result.put("avgResponseMs", Math.round(avgElapsed));
        result.put("failures",      failures);
        result.put("sourceStats",   sourceStats);
        return result;
    }

    // ── 소스별 성공률 ────────────────────────────────────────────────────────

    public List<Map<String, Object>> getSuccessRateBySource(int days) {
        return buildSourceStats(searchLogRepository.getSuccessRateBySource(days));
    }

    private List<Map<String, Object>> buildSourceStats(List<Object[]> rows) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            long total   = toLong(r[1]);
            long success = toLong(r[2]);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("source",       r[0] != null ? r[0].toString() : "unknown");
            m.put("total",        total);
            m.put("success",      success);
            m.put("successRate",  total > 0 ? round1(success * 100.0 / total) : 0.0);
            m.put("avgElapsedMs", Math.round(toDouble(r[3])));
            list.add(m);
        }
        return list;
    }

    // ── 실패 쿼리 ────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getFailedQueries(int days, int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : searchLogRepository.getFailedQueries(days, limit)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("query",       r[0] != null ? r[0].toString() : "");
            m.put("failCount",   toLong(r[1]));
            m.put("lastAttempt", r[2] != null ? r[2].toString() : "");
            list.add(m);
        }
        return list;
    }

    // ── 인기 쿼리 ────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getTrendingQueries(int days, int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : searchLogRepository.getTrendingQueries(days, limit)) {
            long total   = toLong(r[1]);
            long success = toLong(r[2]);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("query",        r[0] != null ? r[0].toString() : "");
            m.put("totalCount",   total);
            m.put("successCount", success);
            m.put("successRate",  total > 0 ? round1(success * 100.0 / total) : 0.0);
            list.add(m);
        }
        return list;
    }

    // ── 성능 지표 ────────────────────────────────────────────────────────────

    public Map<String, Object> getPerformanceMetrics(int days) {
        Object[] row = searchLogRepository.getPerformanceMetrics(days);
        double avg = row != null ? toDouble(row[0]) : 0;
        double min = row != null ? toDouble(row[1]) : 0;
        double max = row != null ? toDouble(row[2]) : 0;

        Double p50 = searchLogRepository.getPercentile(days, 0.50);
        Double p95 = searchLogRepository.getPercentile(days, 0.95);
        Double p99 = searchLogRepository.getPercentile(days, 0.99);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avgMs", Math.round(avg));
        result.put("minMs", Math.round(min));
        result.put("maxMs", Math.round(max));
        result.put("p50Ms", p50 != null ? Math.round(p50) : 0L);
        result.put("p95Ms", p95 != null ? Math.round(p95) : 0L);
        result.put("p99Ms", p99 != null ? Math.round(p99) : 0L);
        result.put("days",  days);
        return result;
    }

    // ── 절약 금액 분석 ───────────────────────────────────────────────────────

    public Map<String, Object> getPriceSavings(int days) {
        Object[] row = searchLogRepository.getPriceSavingsAnalysis(days);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSaved", row != null ? (long) toDouble(row[0]) : 0L);
        result.put("avgSaved",   row != null ? Math.round(toDouble(row[1])) : 0L);
        result.put("count",      row != null ? toLong(row[2]) : 0L);
        result.put("days",       days);
        return result;
    }

    // ── 실패 분석 ────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getCommonFailures(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : searchFailureRepository.findCommonFailures(PageRequest.of(0, limit))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("query",       r[0] != null ? r[0].toString() : "");
            m.put("failCount",   toLong(r[1]));
            m.put("lastAttempt", r[2] != null ? r[2].toString() : "");
            list.add(m);
        }
        return list;
    }

    public Map<String, Object> getFailureStats() {
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (Object[] r : searchFailureRepository.getFailureStatsByStatus()) {
            statusMap.put(r[0] != null ? r[0].toString() : "unknown", toLong(r[1]));
        }
        Map<String, Long> categoryMap = new LinkedHashMap<>();
        for (Object[] r : searchFailureRepository.getFailureStatsByCategory()) {
            categoryMap.put(r[0] != null ? r[0].toString() : "unknown", toLong(r[1]));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byStatus",   statusMap);
        result.put("byCategory", categoryMap);
        result.put("total",      statusMap.values().stream().mapToLong(Long::longValue).sum());
        return result;
    }

    public Map<String, Object> resolveFailure(Long id, String status,
                                              String correctProductName, String correctPcode) {
        SearchFailure f = searchFailureRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("실패 기록을 찾을 수 없습니다. id=" + id));
        f.setIsResolved(status != null ? status : "manual_fixed");
        if (correctProductName != null) f.setCorrectProductName(correctProductName);
        if (correctPcode != null)       f.setCorrectPcode(correctPcode);
        searchFailureRepository.save(f);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "처리되었습니다.");
        result.put("id",      id);
        result.put("status",  f.getIsResolved());
        return result;
    }

    // ── 추천 (stub) ──────────────────────────────────────────────────────────

    public Map<String, Object> getRecommendations() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message",         "추천 분석 기능은 데이터 축적 후 활성화됩니다.");
        result.put("fastpathQuality", "GOOD");
        result.put("suggestions",     Collections.emptyList());
        return result;
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
