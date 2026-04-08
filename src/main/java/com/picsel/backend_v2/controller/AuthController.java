package com.picsel.backend_v2.controller;

import com.picsel.backend_v2.dto.auth.LoginRequest;
import com.picsel.backend_v2.dto.auth.RefreshRequest;
import com.picsel.backend_v2.dto.auth.RegisterRequest;
import com.picsel.backend_v2.dto.auth.TokenResponse;
import com.picsel.backend_v2.security.PicselUserDetails;
import com.picsel.backend_v2.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "로그인/인증", description = "회원가입, 로그인, 토큰 갱신, 로그아웃 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 신규 계정을 생성하고 토큰을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공, accessToken / refreshToken 반환"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음", content = @Content(examples = @ExampleObject(value = "{\"statusCode\":400,\"message\":\"이메일 형식이 올바르지 않습니다.\",\"timestamp\":\"2026-04-07T00:00:00Z\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일", content = @Content(examples = @ExampleObject(value = "{\"statusCode\":409,\"message\":\"이미 사용 중인 이메일입니다.\",\"timestamp\":\"2026-04-07T00:00:00Z\"}")))
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(dto));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.\n\n**Request Body:**\n- `email` (필수): 이메일\n- `password` (필수): 비밀번호\n\n**Response:**\n- `accessToken`: 발급된 access token (유효기간 1시간)\n- `refreshToken`: 발급된 refresh token (유효기간 7일)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공, accessToken / refreshToken 반환"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음", content = @Content(examples = @ExampleObject(value = "{\"statusCode\":400,\"message\":\"요청 값이 올바르지 않습니다.\",\"timestamp\":\"2026-04-07T00:00:00Z\"}"))),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호가 일치하지 않음", content = @Content(examples = @ExampleObject(value = "{\"statusCode\":401,\"message\":\"이메일 또는 비밀번호가 올바르지 않습니다.\",\"timestamp\":\"2026-04-07T00:00:00Z\"}")))
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access Token과 Refresh Token을 재발급합니다.\n\n**Request Body:**\n- `refresh_token` (필수): 유효한 refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 유효하지 않거나 만료됨", content = @Content(examples = @ExampleObject(value = "{\"statusCode\":401,\"message\":\"유효하지 않은 리프레시 토큰입니다.\",\"timestamp\":\"2026-04-07T00:00:00Z\"}")))
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest dto) {
        return ResponseEntity.ok(authService.refreshTokens(dto.getRefreshToken()));
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 종료합니다.\n\n" +
            "- RefreshToken은 DB 세션에서 즉시 삭제됩니다.\n" +
            "- AccessToken은 Redis 블랙리스트에 등록되어 만료 전까지 재사용이 차단됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청")
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal PicselUserDetails user,
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody(required = false) RefreshRequest dto) {
        String refreshToken = dto != null ? dto.getRefreshToken() : null;

        // Authorization 헤더에서 AccessToken 추출
        String bearerToken = request.getHeader("Authorization");
        String accessToken = (bearerToken != null && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7) : null;

        authService.logout(refreshToken, accessToken);
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }

    @Operation(summary = "Google OAuth 로그인 시작", description = "Google OAuth 인증 URL을 반환합니다. (stub)")
    @SecurityRequirements
    @GetMapping("/google")
    public ResponseEntity<Map<String, String>> googleLogin() {
        return ResponseEntity.ok(Map.of("url", "https://accounts.google.com/o/oauth2/v2/auth", "message", "Google OAuth stub"));
    }

    @Operation(summary = "Google OAuth 콜백", description = "Google OAuth 인증 코드를 처리합니다. (stub)")
    @SecurityRequirements
    @GetMapping("/google/callback")
    public ResponseEntity<Map<String, String>> googleCallback(@RequestParam(required = false) String code) {
        return ResponseEntity.ok(Map.of("message", "Google OAuth callback stub", "code", code != null ? code : ""));
    }

    @Operation(summary = "Kakao OAuth 로그인 시작", description = "Kakao OAuth 인증 URL을 반환합니다. (stub)")
    @SecurityRequirements
    @GetMapping("/kakao")
    public ResponseEntity<Map<String, String>> kakaoLogin() {
        return ResponseEntity.ok(Map.of("url", "https://kauth.kakao.com/oauth/authorize", "message", "Kakao OAuth stub"));
    }

    @Operation(summary = "Kakao OAuth 콜백", description = "Kakao OAuth 인증 코드를 처리합니다. (stub)")
    @SecurityRequirements
    @GetMapping("/kakao/callback")
    public ResponseEntity<Map<String, String>> kakaoCallback(@RequestParam(required = false) String code) {
        return ResponseEntity.ok(Map.of("message", "Kakao OAuth callback stub", "code", code != null ? code : ""));
    }

    @Operation(summary = "Naver OAuth 로그인 시작", description = "Naver OAuth 인증 URL을 반환합니다. (stub)")
    @SecurityRequirements
    @GetMapping("/naver")
    public ResponseEntity<Map<String, String>> naverLogin() {
        return ResponseEntity.ok(Map.of("url", "https://nid.naver.com/oauth2.0/authorize", "message", "Naver OAuth stub"));
    }

    @Operation(summary = "Naver OAuth 콜백", description = "Naver OAuth 인증 코드를 처리합니다. (stub)")
    @SecurityRequirements
    @GetMapping("/naver/callback")
    public ResponseEntity<Map<String, String>> naverCallback(@RequestParam(required = false) String code) {
        return ResponseEntity.ok(Map.of("message", "Naver OAuth callback stub", "code", code != null ? code : ""));
    }
}
