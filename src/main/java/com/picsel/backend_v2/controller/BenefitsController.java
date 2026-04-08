package com.picsel.backend_v2.controller;

import com.picsel.backend_v2.service.BenefitService;
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

@Tag(name = "혜택/비교", description = "카드 혜택 비교 및 최적 카드 추천 API (로그인 불필요 — Chrome 확장에서 직접 호출)")
@RestController
@RequestMapping("/benefits")
@RequiredArgsConstructor
public class BenefitsController {

    private final BenefitService benefitService;

    @Operation(
            summary = "전체 카드 혜택 비교",
            description = "가맹점과 결제 금액을 기준으로 DB의 **모든** 활성 카드 혜택을 비교합니다.\n\n" +
                    "카드 등록 없이 사용 가능합니다. Chrome 확장에서 직접 호출하는 공개 API입니다.\n\n" +
                    "**Query Parameters:**\n" +
                    "- `merchant` (필수): 가맹점 이름 (예: 쿠팡, 네이버쇼핑)\n" +
                    "- `amount` (필수): 결제 금액 (숫자, 단위: 원)\n\n" +
                    "**Response:** 카드별 절약 금액(`saved`), 최종 결제 금액(`final_price`) — 절약 금액 내림차순 정렬"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비교 성공")
    })
    @GetMapping("/compare")
    public ResponseEntity<List<Map<String, Object>>> compare(
            @Parameter(description = "가맹점 이름", required = true) @RequestParam String merchant,
            @Parameter(description = "결제 금액 (원)", required = true) @RequestParam double amount) {
        return ResponseEntity.ok(benefitService.compareAll(merchant, amount));
    }

    @Operation(
            summary = "최적 카드 TOP 3 추천",
            description = "혜택이 가장 큰 카드 상위 3개를 반환합니다.\n\n" +
                    "카드 등록 없이 사용 가능합니다. Chrome 확장 팝업의 빠른 추천에 사용됩니다.\n\n" +
                    "**Query Parameters:**\n" +
                    "- `merchant` (필수): 가맹점 이름\n" +
                    "- `amount` (필수): 결제 금액"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 성공")
    })
    @GetMapping("/top3")
    public ResponseEntity<List<Map<String, Object>>> top3(
            @Parameter(description = "가맹점 이름", required = true) @RequestParam String merchant,
            @Parameter(description = "결제 금액 (원)", required = true) @RequestParam double amount) {
        return ResponseEntity.ok(benefitService.top3(merchant, amount));
    }

    @Operation(
            summary = "HTML에서 혜택 추출 후 TOP 3 추천",
            description = "카드사 웹페이지 HTML에서 혜택 정보를 추출하여 DB 혜택과 합산한 TOP 3를 반환합니다.\n\n" +
                    "**Request Body:**\n" +
                    "- `html` (필수): 카드사 혜택 페이지 HTML 또는 텍스트\n\n" +
                    "**Query Parameters:**\n" +
                    "- `merchant` (필수): 가맹점 이름\n" +
                    "- `amount` (필수): 결제 금액"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 성공")
    })
    @PostMapping("/extract-and-compare")
    public ResponseEntity<List<Map<String, Object>>> fromHtml(
            @Parameter(description = "가맹점 이름", required = true) @RequestParam String merchant,
            @Parameter(description = "결제 금액 (원)", required = true) @RequestParam double amount,
            @RequestBody Map<String, String> body) {
        String html = body.get("html");
        List<Map<String, Object>> extra = benefitService.extractFromHtml(html);
        return ResponseEntity.ok(benefitService.top3WithExtraOffers(merchant, amount, extra));
    }

    @Operation(
            summary = "HTML에서 혜택 정보 추출",
            description = "카드사 혜택 페이지 HTML 텍스트에서 할인율·할인액 정보를 정규식으로 추출합니다.\n\n" +
                    "**Query Parameters:**\n" +
                    "- `html` (필수): 분석할 HTML 또는 텍스트"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추출 성공 (빈 배열이면 추출된 혜택 없음)")
    })
    @GetMapping("/extractions")
    public ResponseEntity<List<Map<String, Object>>> extract(
            @Parameter(description = "분석할 HTML 텍스트", required = true) @RequestParam String html) {
        return ResponseEntity.ok(benefitService.extractFromHtml(html));
    }

    @Operation(
            summary = "활성 카드 혜택 전체 목록 조회",
            description = "현재 활성 상태인 모든 카드 혜택 목록을 반환합니다.\n\n" +
                    "대시보드의 '혜택 탐색' 섹션에 표시됩니다. 카드 등록 불필요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/offers")
    public ResponseEntity<List<Map<String, Object>>> offers() {
        return ResponseEntity.ok(benefitService.getAllActiveOffers());
    }
}
