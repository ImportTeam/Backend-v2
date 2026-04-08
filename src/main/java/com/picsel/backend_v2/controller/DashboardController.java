package com.picsel.backend_v2.controller;

import com.picsel.backend_v2.security.PicselUserDetails;
import com.picsel.backend_v2.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "대시보드", description = "SearchLog 기반 사용자 대시보드 API. 일부 엔드포인트는 공개, 나머지는 로그인 필요.")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "내 최근 가격 검색 이력",
            description = "로그인한 사용자가 Chrome 확장을 통해 검색한 최저가 검색 이력을 반환합니다.\n\n" +
                    "**Query Parameters:**\n" +
                    "- `page` (선택, 기본값 0): 페이지 번호\n" +
                    "- `size` (선택, 기본값 20): 페이지 당 항목 수\n\n" +
                    "**Response:** 검색어, 발견된 최저가, 원래 가격, 검색 상태, 검색 시각 목록"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    @GetMapping("/recent-searches")
    public ResponseEntity<List<Map<String, Object>>> recentSearches(
            @AuthenticationPrincipal PicselUserDetails user,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(dashboardService.getRecentSearches(user.getUuid(), page, size));
    }

    @Operation(
            summary = "내 잠재 절약 금액",
            description = "로그인한 사용자의 Chrome 확장 가격 검색 데이터를 기반으로 절약 가능 금액을 집계합니다.\n\n" +
                    "- `total_saved`: 현재가 - 최저가 합계 (원)\n" +
                    "- `search_count`: 총 검색 건수\n" +
                    "- `avg_saved`: 검색 건당 평균 절약액 (원)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    @GetMapping("/savings")
    public ResponseEntity<Map<String, Object>> savings(
            @AuthenticationPrincipal PicselUserDetails user) {
        return ResponseEntity.ok(dashboardService.getPotentialSavings(user.getUuid()));
    }

    @Operation(
            summary = "인기 검색 상품 (전체 공개)",
            description = "전체 사용자의 Chrome 확장 검색 이력을 집계하여 인기 상품 목록을 반환합니다.\n\n" +
                    "로그인 불필요 — 대시보드의 '트렌드' 섹션에 표시됩니다.\n\n" +
                    "**Query Parameters:**\n" +
                    "- `limit` (선택, 기본값 10): 반환할 상품 수"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @SecurityRequirements
    @GetMapping("/popular-products")
    public ResponseEntity<List<Map<String, Object>>> popularProducts(
            @Parameter(description = "반환할 상품 수") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getPopularProducts(limit));
    }

    @Operation(
            summary = "활성 카드 혜택 탐색 (전체 공개)",
            description = "현재 활성 상태인 모든 카드 혜택 목록을 반환합니다.\n\n" +
                    "로그인 불필요 — 대시보드의 '혜택 탐색' 섹션에 표시됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @SecurityRequirements
    @GetMapping("/benefit-offers")
    public ResponseEntity<List<Map<String, Object>>> benefitOffers() {
        return ResponseEntity.ok(dashboardService.getActiveBenefitOffers());
    }
}
