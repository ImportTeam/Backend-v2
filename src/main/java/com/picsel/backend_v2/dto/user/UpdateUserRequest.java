package com.picsel.backend_v2.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private String name;
    private String email;
    private SettingsDto settings;

    @Getter
    @Setter
    public static class SettingsDto {
        private Boolean darkMode;
        private Boolean notificationEnabled;
        private String compareMode;
        private String currencyPreference;
    }
}
