package com.picsel.backend_v2.service;

import com.picsel.backend_v2.domain.ext.SearchLog;
import com.picsel.backend_v2.repository.ext.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SearchLogRepository searchLogRepository;
    private final BenefitService benefitService;

    /**
     * 로그인한 사용자의 최근 가격 검색 이력
     */
    public List<Map<String, Object>> getRecentSearches(String userUuid, int page, int size) {
        List<SearchLog> logs = searchLogRepository
                .findByUserUuidOrderByCreatedAtDesc(userUuid, PageRequest.of(page, size));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SearchLog log : logs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", log.getId());
            entry.put("query_name", log.getQueryName());
            entry.put("found_price", log.getFoundPrice());
            entry.put("origin_price", log.getOriginPrice());
            entry.put("status", log.getStatus());
            entry.put("source", log.getSource());
            entry.put("elapsed_ms", log.getElapsedMs());
            entry.put("created_at", log.getCreatedAt());
            result.add(entry);
        }
        return result;
    }

    /**
     * 로그인한 사용자의 잠재 절약 금액 요약
     */
    public Map<String, Object> getPotentialSavings(String userUuid) {
        Long totalSaved = searchLogRepository.sumPotentialSavings(userUuid);
        long searchCount = searchLogRepository.countByUserUuid(userUuid);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_saved", totalSaved != null ? totalSaved : 0L);
        result.put("search_count", searchCount);
        result.put("avg_saved", searchCount > 0 && totalSaved != null
                ? Math.round(totalSaved / (double) searchCount) : 0L);
        return result;
    }

    /**
     * 전체 인기 검색 상품 (public)
     */
    public List<Map<String, Object>> getPopularProducts(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<Object[]> rows = searchLogRepository.findPopularQueries(PageRequest.of(0, safeLimit));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("query_name", row[0] != null ? row[0].toString() : "");
            entry.put("count", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            result.add(entry);
        }
        return result;
    }

    /**
     * 활성 카드 혜택 목록 (public)
     */
    public List<Map<String, Object>> getActiveBenefitOffers() {
        return benefitService.getAllActiveOffers();
    }
}
