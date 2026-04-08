package com.picsel.backend_v2.repository.ext;

import com.picsel.backend_v2.domain.ext.SearchFailure;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SearchFailureRepository extends JpaRepository<SearchFailure, Long> {

    Optional<SearchFailure> findByOriginalQuery(String originalQuery);

    @Query("SELECT f FROM SearchFailure f WHERE f.createdAt >= :since ORDER BY f.createdAt DESC")
    List<SearchFailure> findRecent(@Param("since") LocalDateTime since);

    @Query(value = """
        SELECT original_query, COUNT(*) AS failCount, MAX(created_at) AS lastAttempt
        FROM search_failures
        WHERE is_resolved = 'pending'
        GROUP BY original_query
        ORDER BY failCount DESC
        """, nativeQuery = true)
    List<Object[]> findCommonFailures(Pageable pageable);

    @Query(value = """
        SELECT is_resolved, COUNT(*) AS cnt
        FROM search_failures
        GROUP BY is_resolved
        """, nativeQuery = true)
    List<Object[]> getFailureStatsByStatus();

    @Query(value = """
        SELECT category_detected, COUNT(*) AS cnt
        FROM search_failures
        WHERE category_detected IS NOT NULL
        GROUP BY category_detected
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<Object[]> getFailureStatsByCategory();
}
