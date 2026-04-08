package com.picsel.backend_v2.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequest {
    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("refreshToken")
    private String refreshToken2;

    public String getToken() {
        return refreshToken != null ? refreshToken : refreshToken2;
    }
}
