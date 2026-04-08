package com.picsel.backend_v2.controller;

import com.picsel.backend_v2.dto.user.ChangePasswordRequest;
import com.picsel.backend_v2.dto.user.MeResponse;
import com.picsel.backend_v2.dto.user.UpdateUserRequest;
import com.picsel.backend_v2.security.PicselUserDetails;
import com.picsel.backend_v2.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "사용자", description = "현재 로그인한 사용자 정보 조회·수정·탈퇴 및 세션 관리 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 프로필 정보를 반환합니다.\n\n**Response:**\n- `seq`: 사용자 PK (String)\n- `uuid`: 사용자 UUID\n- `email`: 이메일\n- `name`: 이름")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청")
    })
    @GetMapping("/current")
    public ResponseEntity<MeResponse> getMe(@AuthenticationPrincipal PicselUserDetails user) {
        return ResponseEntity.ok(userService.getCurrentUser(user.getSeq()));
    }

    @Operation(summary = "내 정보 수정", description = "이름, 이메일, 앱 설정(다크모드, 알림 등)을 수정합니다.\n\n**Request Body:**\n- `name`: 이름\n- `email`: 이메일\n- `settings.darkMode`: 다크모드 여부\n- `settings.notificationEnabled`: 알림 활성화 여부\n- `settings.compareMode`: 비교 모드\n- `settings.currencyPreference`: 통화 설정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일", content = @Content(examples = @ExampleObject(value = "{\"statusCode\":409,\"message\":\"이미 사용 중인 이메일입니다.\",\"timestamp\":\"2026-04-07T00:00:00Z\"}")))
    })
    @PatchMapping("/current")
    public ResponseEntity<MeResponse> updateMe(@AuthenticationPrincipal PicselUserDetails user,
                                                @RequestBody UpdateUserRequest dto) {
        return ResponseEntity.ok(userService.updateCurrentUser(user.getSeq(), dto));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 계정을 영구 삭제합니다. 복구가 불가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청")
    })
    @DeleteMapping("/current")
    public ResponseEntity<Map<String, String>> deleteMe(@AuthenticationPrincipal PicselUserDetails user) {
        return ResponseEntity.ok(userService.deleteCurrentUser(user.getSeq()));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인 후 새 비밀번호로 변경합니다.\n\n**Request Body:**\n- `currentPassword` (필수): 현재 비밀번호\n- `newPassword` (필수): 새 비밀번호")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "401", description = "현재 비밀번호 불일치", content = @Content(examples = @ExampleObject(value = "{\"statusCode\":401,\"message\":\"현재 비밀번호가 일치하지 않습니다.\",\"timestamp\":\"2026-04-07T00:00:00Z\"}"))),
            @ApiResponse(responseCode = "409", description = "소셜 로그인 계정은 비밀번호 없음")
    })
    @PatchMapping("/current/password")
    public ResponseEntity<Map<String, String>> changePassword(@AuthenticationPrincipal PicselUserDetails user,
                                                               @RequestBody ChangePasswordRequest dto) {
        return ResponseEntity.ok(userService.changePassword(user.getSeq(), dto));
    }

    @Operation(summary = "세션 목록 조회", description = "현재 로그인된 세션 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청")
    })
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> listSessions(@AuthenticationPrincipal PicselUserDetails user) {
        return ResponseEntity.ok(userService.listSessions(user.getSeq()));
    }

    @Operation(summary = "전체 세션 종료", description = "현재 계정의 모든 세션을 강제 종료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전체 세션 종료 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청")
    })
    @DeleteMapping("/sessions")
    public ResponseEntity<Map<String, String>> revokeAllSessions(@AuthenticationPrincipal PicselUserDetails user) {
        return ResponseEntity.ok(userService.revokeAllSessions(user.getSeq()));
    }

    @Operation(summary = "특정 세션 종료", description = "세션 ID로 특정 세션을 종료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "세션 종료 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @DeleteMapping("/sessions/{seq}")
    public ResponseEntity<Map<String, String>> revokeSession(@AuthenticationPrincipal PicselUserDetails user,
                                                              @PathVariable Long seq) {
        return ResponseEntity.ok(userService.revokeSession(user.getSeq(), seq));
    }
}
