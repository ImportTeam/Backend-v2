package com.picsel.backend_v2.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenResponse {
    private String message;
    private String access_token;
    private String refresh_token;
    private String issued_at;
    private UserInfo user;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String uuid;
        private String email;
        private String name;
        private String provider;
    }
}
