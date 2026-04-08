package com.picsel.backend_v2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "헬스체크", description = "서버 및 의존성 상태 확인 API")
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbcTemplate;

    @Operation(
            summary = "서버 상태 확인",
            description = "Redis 및 MySQL 연결 상태를 포함한 전체 서버 상태를 반환합니다.\n\n**Response:**\n- `status`: ok / degraded / error\n- `redis`: ok / error\n- `database`: ok / error\n- `timestamp`: 현재 시각 (ISO 8601)\n- `version`: API 버전"
    )
    @ApiResponse(responseCode = "200", description = "상태 확인 성공 (status 필드로 정상 여부 구분)")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        String redisStatus = "ok";
        String dbStatus    = "ok";

        try {
            redis.opsForValue().get("health:ping");
        } catch (Exception e) {
            redisStatus = "error";
        }

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            dbStatus = "error";
        }

        String overall = ("ok".equals(redisStatus) && "ok".equals(dbStatus)) ? "ok"
                : ("error".equals(redisStatus) && "error".equals(dbStatus)) ? "error"
                : "degraded";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status",    overall);
        result.put("redis",     redisStatus);
        result.put("database",  dbStatus);
        result.put("timestamp", Instant.now().toString());
        result.put("version",   "1.0.0");

        return ResponseEntity.ok(result);
    }
}
