package com.picsel.backend_v2.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MeResponse {
    private String uuid;
    private String email;
    private String name;
    private String social_provider;
    private String social_id;
    private String created_at;
    private String updated_at;
}
