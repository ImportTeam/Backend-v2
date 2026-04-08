package com.picsel.backend_v2.service.ext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picsel.backend_v2.domain.ext.SearchFailure;
import com.picsel.backend_v2.domain.ext.SearchLog;
import com.picsel.backend_v2.dto.price.PriceSearchRequest;
import com.picsel.backend_v2.dto.price.PriceSearchResponse;
import com.picsel.backend_v2.dto.price.TopPrice;
import com.picsel.backend_v2.repository.ext.SearchFailureRepository;
import com.picsel.backend_v2.repository.ext.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceSearchOrchestrator {

    private final CacheService cacheService;
    private final FastPathExecutor fastPathExecutor;
    private final TextNormalizationService textNormalizationService;
    private final SearchLogRepository searchLogRepository;
    private final SearchFailureRepository searchFailureRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern PCODE_FROM_URL = Pattern.compile("[?&]pcode=(\\d+)");

    public PriceSearchResponse search(PriceSearchRequest req, String userUuid) {
        long startMs = System.currentTimeMillis();
        String originalName = req.getProductName();
        String normalized   = textNormalizationService.normalize(originalName);

        // 0. 하드 스킵 (연속 3회 이상 실패)
        if (cacheService.shouldHardSkip(normalized)) {
            log.debug("[Hard Skip] {}", normalized);
            return errorResponse("검색 결과를 찾을 수 없습니다. 잠시 후 다시 시도해주세요.", "HARD_SKIP");
        }

        // 1. URL에서 pcode 추출
        String pcode = extractPcode(req.getCurrentUrl());

        // 2. Redis 캐시 조회
        Optional<Map<String, Object>> cached = cacheService.getFromRedis(normalized);
        if (cached.isEmpty()) {
            cached = cacheService.getFromDb(normalized);  // DB 영구 캐시
        }
        if (cached.isPresent()) {
            double elapsed = System.currentTimeMillis() - startMs;
            logAsync(normalized, req.getCurrentPrice(), toFoundPrice(cached.get()),
                    toProductId(cached.get()), "HIT", "cache", elapsed, cached.get(), userUuid);
            return buildSuccess(cached.get(), req, "cache", elapsed);
        }

        // 3. Negative 캐시 확인
        if (cacheService.isNegativeCached(normalized)) {
            return errorResponse("검색 결과를 찾을 수 없습니다.", "NOT_FOUND");
        }

        // 4. FastPath (HTTP + Jsoup)
        Optional<Map<String, Object>> fastResult = Optional.empty();
        try {
            fastResult = fastPathExecutor.execute(normalized);
        } catch (Exception e) {
            log.warn("[FastPath] Exception: {}", e.getMessage());
        }

        if (fastResult.isPresent()) {
            Map<String, Object> data = fastResult.get();
            cacheService.setToRedis(normalized, data);
            cacheService.upsertToDb(normalized, data);
            double elapsed = System.currentTimeMillis() - startMs;
            logAsync(normalized, req.getCurrentPrice(), toFoundPrice(data),
                    toProductId(data), "SUCCESS", "fastpath", elapsed, data, userUuid);
            return buildSuccess(data, req, "fastpath", elapsed);
        }

        // 5. SlowPath stub (Playwright 미구현 — 실패 처리)
        log.debug("[SlowPath] Not implemented — recording failure");
        int failCount = cacheService.incrementFailureCount(normalized);
        if (failCount >= 3) {
            cacheService.setNegativeCache(normalized);
        }
        double elapsed = System.currentTimeMillis() - startMs;
        recordFailureAsync(originalName, normalized, "FastPath 및 SlowPath 모두 실패");
        logAsync(normalized, req.getCurrentPrice(), null,
                pcode, "FAIL", "slowpath", elapsed, null, userUuid);

        return errorResponse("검색 결과를 찾을 수 없습니다.", "NOT_FOUND");
    }

    // ── 응답 빌더 ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private PriceSearchResponse buildSuccess(Map<String, Object> data,
                                             PriceSearchRequest req,
                                             String source,
                                             double elapsedMs) {
        Integer foundPrice  = toFoundPrice(data);
        Integer originPrice = req.getCurrentPrice();

        boolean isCheaper = originPrice != null && foundPrice != null && foundPrice < originPrice;
        int priceDiff = (originPrice != null && foundPrice != null) ? originPrice - foundPrice : 0;

        List<TopPrice> topPrices;
        Object rawTop = data.get("top_prices");
        if (rawTop instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof TopPrice) {
            topPrices = (List<TopPrice>) rawTop;
        } else if (rawTop instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
            topPrices = list.stream().map(o -> {
                Map<String, Object> m = (Map<String, Object>) o;
                return TopPrice.builder()
                        .rank(toInt(m.get("rank"), 1))
                        .mall(str(m.get("mall")))
                        .price(toInt(m.get("price"), 0))
                        .freeShipping(toBool(m.get("free_shipping")))
                        .delivery(str(m.get("delivery")))
                        .link(str(m.get("link")))
                        .build();
            }).toList();
        } else {
            topPrices = Collections.emptyList();
        }

        return PriceSearchResponse.builder()
                .status("success")
                .data(PriceSearchResponse.Data.builder()
                        .productName(str(data.get("product_name")))
                        .productId(str(data.get("product_id")))
                        .isCheaper(isCheaper)
                        .priceDiff(priceDiff)
                        .lowestPrice(foundPrice)
                        .link(str(data.get("product_url")))
                        .mall(str(data.get("mall")))
                        .freeShipping(toBool(data.get("free_shipping")))
                        .topPrices(topPrices)
                        .priceTrend(null)
                        .selectedOptions(req.getSelectedOptions())
                        .source(source)
                        .elapsedMs(elapsedMs)
                        .build())
                .message("검색 성공")
                .selectedOptions(req.getSelectedOptions())
                .build();
    }

    private PriceSearchResponse errorResponse(String message, String errorCode) {
        return PriceSearchResponse.builder()
                .status("error")
                .message(message)
                .errorCode(errorCode)
                .build();
    }

    // ── 비동기 로깅 ──────────────────────────────────────────────────────────

    @Async
    public void logAsync(String queryName, Integer originPrice, Integer foundPrice,
                         String productId, String status, String source,
                         double elapsedMs, Map<String, Object> data, String userUuid) {
        try {
            String topPricesJson = null;
            if (data != null && data.get("top_prices") != null) {
                topPricesJson = objectMapper.writeValueAsString(data.get("top_prices"));
            }
            searchLogRepository.save(SearchLog.builder()
                    .userUuid(userUuid)
                    .queryName(queryName)
                    .originPrice(originPrice)
                    .foundPrice(foundPrice)
                    .productId(productId)
                    .status(status)
                    .source(source)
                    .elapsedMs(elapsedMs)
                    .topPrices(topPricesJson)
                    .build());
        } catch (Exception e) {
            log.warn("[Log] Failed to save search log: {}", e.getMessage());
        }
    }

    @Async
    public void recordFailureAsync(String originalQuery, String normalizedQuery, String errorMessage) {
        try {
            SearchFailure existing = searchFailureRepository.findByOriginalQuery(originalQuery).orElse(null);
            if (existing != null) {
                existing.setAttemptedCount(existing.getAttemptedCount() + 1);
                existing.setErrorMessage(errorMessage);
                searchFailureRepository.save(existing);
            } else {
                searchFailureRepository.save(SearchFailure.builder()
                        .originalQuery(originalQuery)
                        .normalizedQuery(normalizedQuery)
                        .errorMessage(errorMessage)
                        .build());
            }
        } catch (Exception e) {
            log.warn("[Log] Failed to record failure: {}", e.getMessage());
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private String extractPcode(String url) {
        if (url == null) return null;
        Matcher m = PCODE_FROM_URL.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private Integer toFoundPrice(Map<String, Object> data) {
        Object v = data.get("lowest_price");
        if (v instanceof Integer i) return i;
        if (v instanceof Number n) return n.intValue();
        return null;
    }

    private String toProductId(Map<String, Object> data) {
        Object v = data.get("product_id");
        return v != null ? v.toString() : null;
    }

    private String str(Object o) { return o != null ? o.toString() : null; }

    private int toInt(Object o, int defaultVal) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return defaultVal; }
    }

    private Boolean toBool(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) return Boolean.parseBoolean(s);
        return null;
    }
}
