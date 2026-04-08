package com.picsel.backend_v2.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank @Email
    private String email;

    /**
     * 비밀번호 복잡도 정책:
     * - 최소 8자
     * - 영문 대소문자 최소 1자 이상
     * - 숫자 최소 1자 이상
     * - 특수문자(!@#$%^&*) 최소 1자 이상
     */
    @NotBlank
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
            message = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다."
    )
    private String password;

    @NotBlank
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String name;
}
