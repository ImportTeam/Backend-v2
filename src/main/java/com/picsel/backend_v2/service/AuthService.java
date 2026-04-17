package com.picsel.backend_v2.service;

import com.picsel.backend_v2.domain.User;
import com.picsel.backend_v2.domain.UserSession;
import com.picsel.backend_v2.domain.UserSettings;
import com.picsel.backend_v2.dto.auth.LoginRequest;
import com.picsel.backend_v2.dto.auth.RegisterRequest;
import com.picsel.backend_v2.dto.auth.TokenResponse;
import com.picsel.backend_v2.exception.ApiException;
import com.picsel.backend_v2.repository.UserRepository;
import com.picsel.backend_v2.repository.UserSessionRepository;
import com.picsel.backend_v2.repository.UserSettingsRepository;
import com.picsel.backend_v2.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String RATE_KEY_PREFIX      = "rate:login:";
    private static final String RATE_REGISTER_PREFIX = "rate:register:";
    private static final String RATE_REFRESH_PREFIX  = "rate:refresh:";
    private static final String BLACKLIST_PREFIX      = "blacklist:access:";
    private static final int    MAX_LOGIN_ATTEMPTS    = 5;
    private static final int    MAX_REGISTER_ATTEMPTS = 10;
    private static final int    MAX_REFRESH_ATTEMPTS  = 20;
    private static final long   RATE_WINDOW_SEC       = 60;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;

    @Transactional
    public TokenResponse register(RegisterRequest dto) {
        String email = dto.getEmail().trim().toLowerCase();

        // Rate Limiting — 1분 내 10회 초과 시 차단 (계정 대량 생성 방지)
        String rateKey = RATE_REGISTER_PREFIX + email;
        Long attempts = redis.opsForValue().increment(rateKey);
        if (attempts != null && attempts == 1) {
            redis.expire(rateKey, RATE_WINDOW_SEC, TimeUnit.SECONDS);
        }
        if (attempts != null && attempts > MAX_REGISTER_ATTEMPTS) {
            long ttl = redis.getExpire(rateKey, TimeUnit.SECONDS);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "너무 많은 요청입니다. " + ttl + "초 후 다시 시도해주세요.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .email(email)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .socialProvider("NONE")
                .build();

        user = userRepository.save(user);

        UserSettings settings = UserSettings.builder()
                .userSeq(user.getSeq())
                .user(user)
                .build();
        userSettingsRepository.save(settings);

        return issueTokens(user, "회원가입 및 로그인 성공", null);
    }

    @Transactional
    public TokenResponse login(LoginRequest dto) {
        String email = dto.getEmail().trim().toLowerCase();

        // ① Rate Limiting — 1분 내 5회 초과 시 차단
        String rateKey = RATE_KEY_PREFIX + email;
        Long attempts = redis.opsForValue().increment(rateKey);
        if (attempts != null && attempts == 1) {
            redis.expire(rateKey, RATE_WINDOW_SEC, TimeUnit.SECONDS);
        }
        if (attempts != null && attempts > MAX_LOGIN_ATTEMPTS) {
            long ttl = redis.getExpire(rateKey, TimeUnit.SECONDS);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "로그인 시도 횟수를 초과했습니다. " + ttl + "초 후 다시 시도해주세요.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.getPasswordHash() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // ② 로그인 성공 시 Rate Limit 카운터 초기화
        redis.delete(rateKey);

        return issueTokens(user, "로그인 성공", null);
    }

    @Transactional
    public TokenResponse socialLogin(String email, String name, String provider, String providerId) {
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "이메일 정보가 필요합니다.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user != null) {
            if (user.getSocialId() == null || "NONE".equals(user.getSocialProvider())) {
                user.setSocialProvider(provider.toUpperCase());
                user.setSocialId(providerId);
                user = userRepository.save(user);
            }
        } else {
            user = User.builder()
                    .uuid(UUID.randomUUID().toString())
                    .email(normalizedEmail)
                    .name(name != null ? name : "사용자")
                    .socialProvider(provider.toUpperCase())
                    .socialId(providerId)
                    .build();
            user = userRepository.save(user);

            UserSettings settings = UserSettings.builder()
                    .userSeq(user.getSeq())
                    .user(user)
                    .build();
            userSettingsRepository.save(settings);
        }

        return issueTokens(user, "소셜 로그인 성공", provider);
    }

    @Transactional
    public TokenResponse refreshTokens(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token required");
        }

        // Rate Limiting — 토큰 기반 1분 내 20회 초과 시 차단 (토큰 replay 남용 방지)
        String rateKey = RATE_REFRESH_PREFIX + refreshToken.substring(Math.max(0, refreshToken.length() - 16));
        Long attempts = redis.opsForValue().increment(rateKey);
        if (attempts != null && attempts == 1) {
            redis.expire(rateKey, RATE_WINDOW_SEC, TimeUnit.SECONDS);
        }
        if (attempts != null && attempts > MAX_REFRESH_ATTEMPTS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.");
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session not found or refresh token revoked"));

        User user = userRepository.findById(session.getUserSeq())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));

        userSessionRepository.deleteByRefreshToken(refreshToken);

        return issueTokens(user, null, null);
    }

    /**
     * 로그아웃 처리.
     * RefreshToken: DB에서 세션 삭제
     * AccessToken: Redis 블랙리스트에 등록 (잔여 만료 시간까지 유지)
     */
    @Transactional
    public void logout(String refreshToken, String accessToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            userSessionRepository.deleteByRefreshToken(refreshToken);
        }

        // ③ AccessToken 블랙리스트 등록 — 로그아웃 후 1시간 이내 재사용 차단
        if (accessToken != null && !accessToken.isBlank()) {
            long remainingMs = jwtTokenProvider.getRemainingMs(accessToken);
            if (remainingMs > 0) {
                redis.opsForValue().set(
                        BLACKLIST_PREFIX + accessToken,
                        "1",
                        remainingMs,
                        TimeUnit.MILLISECONDS
                );
                log.debug("[Auth] AccessToken blacklisted for {}ms", remainingMs);
            }
        }
    }

    /** AccessToken이 블랙리스트에 있는지 확인 (JwtAuthFilter에서 호출) */
    public boolean isAccessTokenBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + accessToken));
    }

    private TokenResponse issueTokens(User user, String message, String provider) {
        String accessToken  = jwtTokenProvider.generateAccessToken(user.getSeq(), user.getUuid(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getSeq(), user.getUuid());

        long accessMs = jwtTokenProvider.getAccessExpirationMs();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(accessMs / 1000);

        UserSession session = UserSession.builder()
                .userSeq(user.getSeq())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .build();
        userSessionRepository.save(session);

        return TokenResponse.builder()
                .message(message != null ? message : "토큰 갱신 성공")
                .access_token(accessToken)
                .refresh_token(refreshToken)
                .issued_at(Instant.now().toString())
                .user(TokenResponse.UserInfo.builder()
                        .id(user.getSeq())
                        .uuid(user.getUuid())
                        .email(user.getEmail())
                        .name(user.getName())
                        .provider(provider)
                        .build())
                .build();
    }
}
