package com.picsel.backend_v2.config;

import com.picsel.backend_v2.security.JwtAuthFilter;
import com.picsel.backend_v2.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // 브라우저가 Content-Type을 무시하고 파일을 실행하는 MIME 스니핑 방지
                        .contentTypeOptions(c -> {})
                        // iframe 삽입 차단 → Clickjacking 방지
                        .frameOptions(f -> f.deny())
                        // HTTPS 강제 (1년, 서브도메인 포함)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        // XSS 필터 활성화 (레거시 브라우저 대응)
                        .xssProtection(xss -> xss
                                .headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                )
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI (/swagger 로 접속)
                        .requestMatchers("/swagger", "/swagger/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs").permitAll()
                        // 헬스체크 & Extension 공개 API (인증 불필요)
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/v1/price/**").permitAll()
                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/google", "/auth/google/callback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/kakao", "/auth/kakao/callback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/naver", "/auth/naver/callback").permitAll()
                        // 혜택 비교 공개 API (Chrome 확장, 비로그인 허용)
                        .requestMatchers(HttpMethod.GET, "/benefits/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/benefits/**").permitAll()
                        // 대시보드 공개 섹션
                        .requestMatchers(HttpMethod.GET, "/dashboard/popular-products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/dashboard/benefit-offers").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://picsel.kr",
                "https://www.picsel.kr",
                "https://*.picsel.kr",
                "https://picsel.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
