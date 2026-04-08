package com.picsel.backend_v2.controller;

import com.picsel.backend_v2.dto.price.PriceSearchRequest;
import com.picsel.backend_v2.dto.price.PriceSearchResponse;
import com.picsel.backend_v2.repository.ext.SearchLogRepository;
import com.picsel.backend_v2.security.PicselUserDetails;
import com.picsel.backend_v2.service.ext.PriceSearchOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "가격 검색 (Extension)", description = "브라우저 확장 프로그램용 다나와 최저가 검색 API. 인증 불필요.")
@RestController
@RequestMapping("/v1/price")
@RequiredArgsConstructor
public class PriceController {

    private final PriceSearchOrchestrator orchestrator;
    private final SearchLogRepository searchLogRepository;

    @Operation(
            summary = "최저가 검색",
            description = """
                    상품명으로 다나와 최저가를 검색합니다.

                    **검색 파이프라인 (최대 12초):**
                    1. Redis 캐시 조회 (0.5초)
                    2. DB 영구 캐시 조회
                    3. FastPath — HTTP + Jsoup 크롤링 (최대 8초)
                    4. SlowPath — 브라우저 크롤링 (미구현, 실패 처리)

                    **캐싱:** 성공 결과는 Redis에 6시간 보관됩니다.

                    **Request Body:**
                    - `product_name` (필수): 상품명 (1~500자)
                    - `current_price` (선택): 현재 보고 있는 가격 → `is_cheaper` 비교에 사용
                    - `current_url` (선택): 현재 페이지 URL → pcode 추출에 사용
                    - `selected_options` (선택): 선택된 옵션 목록

                    **Response:**
                    - `data.is_cheaper`: 최저가가 현재 가격보다 저렴한지 여부
                    - `data.price_diff`: 절약 금액 (원)
                    - `data.top_prices`: 쇼핑몰별 가격 TOP 3
                    - `data.source`: 결과 출처 (cache / fastpath / slowpath)
                    - `data.elapsed_ms`: 응답 시간 (ms)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공 또는 결과 없음 (status 필드로 구분)",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "status": "success",
                              "data": {
                                "product_name": "삼성 갤럭시 S24",
                                "lowest_price": 890000,
                                "is_cheaper": true,
                                "price_diff": 110000,
                                "top_prices": [
                                  {"rank":1,"mall":"쿠팡","price":890000,"free_shipping":true,"delivery":"내일도착","link":"..."}
                                ],
                                "source": "fastpath",
                                "elapsed_ms": 1230.5
                              },
                              "message": "검색 성공"
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "요청 값 오류 (상품명 누락 또는 길이 초과)")
    })
    @SecurityRequirements
    @PostMapping("/search")
    public ResponseEntity<PriceSearchResponse> search(
            @Valid @RequestBody PriceSearchRequest req,
            @AuthenticationPrincipal PicselUserDetails user) {
        String userUuid = user != null ? user.getUuid() : null;
        return ResponseEntity.ok(orchestrator.search(req, userUuid));
    }

    @Operation(
            summary = "검색 통계",
            description = "전체 검색 수, 캐시 히트 수, 히트율, 인기 검색어를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "통계 조회 성공")
    @SecurityRequirements
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics() {
        long total     = searchLogRepository.countTotal();
        long cacheHits = searchLogRepository.countCacheHits();
        double hitRate = total > 0 ? Math.round(cacheHits * 1000.0 / total) / 10.0 : 0.0;

        List<Object[]> popularRaw = searchLogRepository.findPopularQueries(5);
        List<Map<String, Object>> popularQueries = popularRaw.stream().map(r -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("query", r[0] != null ? r[0].toString() : "");
            m.put("count", r[1] != null ? ((Number) r[1]).longValue() : 0L);
            return m;
        }).toList();

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("totalSearches",  total);
        result.put("cacheHits",      cacheHits);
        result.put("hitRate",        hitRate);
        result.put("popularQueries", popularQueries);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "인기 검색어",
            description = "최근 검색 횟수 기준 인기 검색어 목록을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @SecurityRequirements
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> popular(
            @RequestParam(defaultValue = "10") int limit) {
        List<Object[]> rows = searchLogRepository.findPopularQueries(limit);
        List<Map<String, Object>> queries = rows.stream().map(r -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("query", r[0] != null ? r[0].toString() : "");
            m.put("count", r[1] != null ? ((Number) r[1]).longValue() : 0L);
            return m;
        }).toList();

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("popularQueries", queries);
        return ResponseEntity.ok(result);
    }
}
