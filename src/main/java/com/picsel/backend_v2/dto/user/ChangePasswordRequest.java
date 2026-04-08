package com.picsel.backend_v2.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank @jakarta.validation.constraints.Size(min = 6)
    private String newPassword;
}
