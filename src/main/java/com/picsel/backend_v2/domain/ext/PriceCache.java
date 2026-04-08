package com.picsel.backend_v2.domain.ext;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "price_cache", indexes = {
        @Index(name = "idx_price_cache_key_updated", columnList = "cache_key,updated_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PriceCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", nullable = false, unique = true)
    private String cacheKey;

    /** Full crawler result as JSON */
    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
