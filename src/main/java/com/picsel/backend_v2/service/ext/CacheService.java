package com.picsel.backend_v2.service.ext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picsel.backend_v2.domain.ext.PriceCache;
import com.picsel.backend_v2.repository.ext.PriceCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redis;
    private final PriceCacheRepository priceCacheRepository;
    private final ObjectMapper objectMapper;

    @Value("${ext.cache.ttl:21600}")
    private long cacheTtl;

    @Value("${ext.cache.negative-ttl:60}")
    private long negativeTtl;

    // ── 캐시 키 생성 ─────────────────────────────────────────────────────────

    public String generateCacheKey(String productName) {
        try {
            String cleaned = cleanProductName(productName);
            byte[] hash = MessageDigest.getInstance("MD5").digest(cleaned.getBytes(StandardCharsets.UTF_8));
            return "price:" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "price:" + productName.hashCode();
        }
    }

    private String generateNegativeCacheKey(String productName) {
        return generateCacheKey(productName).replace("price:", "price:neg:");
    }

    private String generateFailCountKey(String productName) {
        return generateNegativeCacheKey(productName) + ":fail_count";
    }

    private String cleanProductName(String name) {
        return name.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    // ── Redis 캐시 ─────────────────────────────────────────────────────────

    public Optional<Map<String, Object>> getFromRedis(String productName) {
        try {
            String key = generateCacheKey(productName);
            String value = redis.opsForValue().get(key);
            if (value == null) return Optional.empty();
            Map<String, Object> data = objectMapper.readValue(value, new TypeReference<>() {});
            log.debug("[Cache] Redis HIT: {}", key);
            return Optional.of(data);
        } catch (Exception e) {
            log.warn("[Cache] Redis get error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void setToRedis(String productName, Map<String, Object> data) {
        try {
            String key = generateCacheKey(productName);
            String json = objectMapper.writeValueAsString(data);
            redis.opsForValue().set(key, json, cacheTtl, TimeUnit.SECONDS);
            log.debug("[Cache] Redis SET: {} (TTL {}s)", key, cacheTtl);
        } catch (Exception e) {
            log.warn("[Cache] Redis set error: {}", e.getMessage());
        }
    }

    // ── DB 영구 캐시 ───────────────────────────────────────────────────────

    public Optional<Map<String, Object>> getFromDb(String productName) {
        try {
            String key = generateCacheKey(productName);
            LocalDateTime since = LocalDateTime.now().minusSeconds(cacheTtl);
            return priceCacheRepository.findFresh(key, since)
                    .map(pc -> {
                        try {
                            return objectMapper.readValue(pc.getPayloadJson(), new TypeReference<Map<String, Object>>() {});
                        } catch (Exception e) {
                            return null;
                        }
                    });
        } catch (Exception e) {
            log.warn("[Cache] DB get error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void upsertToDb(String productName, Map<String, Object> data) {
        try {
            String key = generateCacheKey(productName);
            String json = objectMapper.writeValueAsString(data);
            PriceCache pc = priceCacheRepository.findByCacheKey(key)
                    .orElse(PriceCache.builder().cacheKey(key).build());
            pc.setPayloadJson(json);
            priceCacheRepository.save(pc);
        } catch (Exception e) {
            log.warn("[Cache] DB upsert error: {}", e.getMessage());
        }
    }

    // ── Negative Cache ──────────────────────────────────────────────────────

    public boolean isNegativeCached(String productName) {
        String key = generateNegativeCacheKey(productName);
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    public void setNegativeCache(String productName) {
        try {
            String key = generateNegativeCacheKey(productName);
            redis.opsForValue().set(key, "{\"message\":\"검색 결과 없음\"}", negativeTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[Cache] Negative cache set error: {}", e.getMessage());
        }
    }

    public int incrementFailureCount(String productName) {
        try {
            String key = generateFailCountKey(productName);
            Long count = redis.opsForValue().increment(key);
            redis.expire(key, 120, TimeUnit.SECONDS);
            return count != null ? count.intValue() : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    public boolean shouldHardSkip(String productName) {
        try {
            String key = generateFailCountKey(productName);
            String val = redis.opsForValue().get(key);
            return val != null && Integer.parseInt(val) >= 3;
        } catch (Exception e) {
            return false;
        }
    }
}
