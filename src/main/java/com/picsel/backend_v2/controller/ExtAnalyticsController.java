package com.picsel.backend_v2.controller;

import com.picsel.backend_v2.service.ext.ExtAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Extension 분석", description = "브라우저 확장 프로그램 검색 로그 분석 API")
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class ExtAnalyticsController {

    private final ExtAnalyticsService analyticsService;

    // ── v2 엔드포인트 ────────────────────────────────────────────────────────

    @Operation(summary = "일별 스냅샷", description = "최근 N일간 검색 요약 (전체 건수, 성공률, 캐시 히트율, 평균 응답시간, 절약 금액)을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/v2/daily-snapshot")
    public ResponseEntity<Map<String, Object>> dailySnapshot(
            @Parameter(description = "조회 기간 (일수, 기본값 1)") @RequestParam(defaultValue = "1") int days) {
        return ResponseEntity.ok(analyticsService.getDailySnapshot(days));
    }

    @Operation(summary = "주간 리포트", description = "최근 N일간 검색 성과 리포트 (성공률, 소스별 통계, 평균 응답시간, 실패 수)를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/v2/weekly-report")
    public ResponseEntity<Map<String, Object>> weeklyReport(
            @Parameter(description = "조회 기간 (일수, 기본값 7)") @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getWeeklyReport(days));
    }

    @Operation(summary = "소스별 성공률", description = "cache / fastpath / slowpath 각 소스별 성공률과 평균 응답시간을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/v2/success-rate")
    public ResponseEntity<List<Map<String, Object>>> successRate(
            @Parameter(description = "조회 기간 (일수, 기본값 7)") @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getSuccessRateBySource(days));
    }

    @Operation(summary = "실패 쿼리 목록", description = "최근 N일간 검색 실패 횟수가 많은 쿼리 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/v2/failed-queries")
    public ResponseEntity<List<Map<String, Object>>> failedQueries(
            @Parameter(description = "조회 기간 (일수, 기본값 7)") @RequestParam(defaultValue = "7") int days,
            @Parameter(description = "최대 반환 수 (기본값 20)") @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(analyticsService.getFailedQueries(days, limit));
    }

    @Operation(summary = "인기 검색어 + 성공률", description = "최근 N일간 검색 횟수 기준 인기 쿼리와 각 쿼리의 성공률을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/v2/trending-queries")
    public ResponseEntity<List<Map<String, Object>>> trendingQueries(
            @Parameter(description = "조회 기간 (일수, 기본값 7)") @RequestParam(defaultValue = "7") int days,
            @Parameter(description = "최대 반환 수 (기본값 20)") @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(analyticsService.getTrendingQueries(days, limit));
    }

    @Operation(summary = "응답 시간 성능 지표", description = "최근 N일간 검색 응답 시간 통계 (평균, 최소, 최대, p50, p95, p99)를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/v2/performance")
    public ResponseEntity<Map<String, Object>> performance(
            @Parameter(description = "조회 기간 (일수, 기본값 7)") @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getPerformanceMetrics(days));
    }

    @Operation(summary = "가격 절약 분석", description = "최근 N일간 검색으로 절약한 총 금액, 평균 절약 금액, 절약 건수를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/v2/price-savings")
    public ResponseEntity<Map<String, Object>> priceSavings(
            @Parameter(description = "조회 기간 (일수, 기본값 7)") @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getPriceSavings(days));
    }

    @Operation(summary = "개선 추천", description = "FastPath 품질, 실패 패턴 분석 기반 개선 추천을 반환합니다. (데이터 축적 후 활성화)")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/v2/recommendations")
    public ResponseEntity<Map<String, Object>> recommendations() {
        return ResponseEntity.ok(analyticsService.getRecommendations());
    }

    // ── v1 레거시 엔드포인트 ─────────────────────────────────────────────────

    @Operation(summary = "대시보드 (legacy)", description = "주간 리포트와 동일합니다. `/analytics/v2/weekly-report?days=7` 사용을 권장합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(analyticsService.getWeeklyReport(7));
    }

    @Operation(summary = "자주 실패하는 검색어 (legacy)", description = "미해결 상태로 가장 많이 실패한 검색어 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/common-failures")
    public ResponseEntity<List<Map<String, Object>>> commonFailures(
            @Parameter(description = "최대 반환 수 (기본값 20)") @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(analyticsService.getCommonFailures(limit));
    }

    @Operation(summary = "실패 통계 (legacy)", description = "상태별·카테고리별 실패 건수 통계를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/category-analysis")
    public ResponseEntity<Map<String, Object>> categoryAnalysis() {
        return ResponseEntity.ok(analyticsService.getFailureStats());
    }

    @Operation(summary = "개선 제안 (legacy)", description = "`/analytics/v2/recommendations` 와 동일합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/improvements")
    public ResponseEntity<Map<String, Object>> improvements() {
        return ResponseEntity.ok(analyticsService.getRecommendations());
    }

    @Operation(summary = "실패 해결 처리", description = "검색 실패 기록을 해결 완료로 표시합니다.\n\n**Request Body:**\n- `status`: 해결 상태 (manual_fixed / auto_learned / not_product)\n- `correct_product_name`: 올바른 상품명\n- `correct_pcode`: 올바른 다나와 pcode")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "404", description = "해당 실패 기록 없음")
    })
    @PostMapping("/resolve/{failureId}")
    public ResponseEntity<Map<String, Object>> resolve(
            @Parameter(description = "실패 기록 ID") @PathVariable Long failureId,
            @RequestBody(required = false) Map<String, String> body) {
        String status              = body != null ? body.get("status") : null;
        String correctProductName  = body != null ? body.get("correct_product_name") : null;
        String correctPcode        = body != null ? body.get("correct_pcode") : null;
        return ResponseEntity.ok(analyticsService.resolveFailure(failureId, status, correctProductName, correctPcode));
    }
}
