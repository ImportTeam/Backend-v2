package com.picsel.backend_v2.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @Size(min = 1, max = 50, message = "이름은 1~50자 이내여야 합니다.")
    private String name;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
    private String email;

    private SettingsDto settings;

    @Getter
    @Setter
    public static class SettingsDto {
        private Boolean darkMode;
        private Boolean notificationEnabled;

        @Size(max = 50)
        @Pattern(regexp = "^(default|side-by-side|overlay)?$", message = "지원하지 않는 compareMode 값입니다.")
        private String compareMode;

        @Size(max = 10)
        @Pattern(regexp = "^(KRW|USD|EUR|JPY)?$", message = "지원하지 않는 통화 코드입니다.")
        private String currencyPreference;
    }
}
