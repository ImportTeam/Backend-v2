package com.picsel.backend_v2.service;

import com.picsel.backend_v2.domain.User;
import com.picsel.backend_v2.domain.UserSession;
import com.picsel.backend_v2.domain.UserSettings;
import com.picsel.backend_v2.dto.user.ChangePasswordRequest;
import com.picsel.backend_v2.dto.user.MeResponse;
import com.picsel.backend_v2.dto.user.UpdateUserRequest;
import com.picsel.backend_v2.exception.ApiException;
import com.picsel.backend_v2.repository.UserRepository;
import com.picsel.backend_v2.repository.UserSessionRepository;
import com.picsel.backend_v2.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    public MeResponse getCurrentUser(Long userSeq) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        return MeResponse.builder()
                .uuid(user.getUuid())
                .email(user.getEmail())
                .name(user.getName())
                .social_provider(user.getSocialProvider())
                .social_id(user.getSocialId())
                .created_at(user.getCreatedAt().toString())
                .updated_at(user.getUpdatedAt().toString())
                .build();
    }

    @Transactional
    public MeResponse updateCurrentUser(Long userSeq, UpdateUserRequest dto) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getEmail() != null) {
            String email = dto.getEmail().trim().toLowerCase();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
            }
            user.setEmail(email);
        }

        userRepository.save(user);

        if (dto.getSettings() != null) {
            UpdateUserRequest.SettingsDto s = dto.getSettings();
            UserSettings settings = userSettingsRepository.findByUserSeq(userSeq)
                    .orElseGet(() -> UserSettings.builder().userSeq(userSeq).user(user).build());

            if (s.getDarkMode() != null) settings.setDarkMode(s.getDarkMode());
            if (s.getNotificationEnabled() != null) settings.setNotificationEnabled(s.getNotificationEnabled());
            if (s.getCompareMode() != null) settings.setCompareMode(s.getCompareMode());
            if (s.getCurrencyPreference() != null) settings.setCurrencyPreference(s.getCurrencyPreference());
            userSettingsRepository.save(settings);
        }

        return getCurrentUser(userSeq);
    }

    @Transactional
    public Map<String, String> deleteCurrentUser(Long userSeq) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        userRepository.delete(user);
        return Map.of("message", "계정이 삭제되었습니다.");
    }

    @Transactional
    public Map<String, String> changePassword(Long userSeq, ChangePasswordRequest dto) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (user.getPasswordHash() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "비밀번호가 없는 계정입니다(소셜 로그인 계정).");
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        return Map.of("message", "비밀번호가 변경되었습니다.");
    }

    public List<Map<String, Object>> listSessions(Long userSeq) {
        List<UserSession> sessions = userSessionRepository.findByUserSeqOrderByCreatedAtDesc(userSeq);
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserSession s : sessions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("seq", s.getSeq());
            item.put("userSeq", s.getUserSeq());
            item.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            item.put("expiresAt", s.getExpiresAt() != null ? s.getExpiresAt().toString() : null);
            result.add(item);
        }
        return result;
    }

    @Transactional
    public Map<String, String> revokeAllSessions(Long userSeq) {
        List<UserSession> sessions = userSessionRepository.findByUserSeqOrderByCreatedAtDesc(userSeq);
        userSessionRepository.deleteAll(sessions);
        return Map.of("message", "모든 세션이 종료되었습니다.");
    }

    @Transactional
    public Map<String, String> revokeSession(Long userSeq, Long sessionSeq) {
        UserSession session = userSessionRepository.findBySeqAndUserSeq(sessionSeq, userSeq)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));
        userSessionRepository.delete(session);
        return Map.of("message", "세션이 종료되었습니다.");
    }
}
