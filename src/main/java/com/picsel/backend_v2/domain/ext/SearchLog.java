package com.picsel.backend_v2.domain.ext;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs", indexes = {
        @Index(name = "idx_status_created", columnList = "status,created_at"),
        @Index(name = "idx_query_created",  columnList = "query_name,created_at"),
        @Index(name = "idx_product_id",     columnList = "product_id"),
        @Index(name = "idx_user_uuid",      columnList = "user_uuid")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** nullable — null means anonymous/non-logged-in search */
    @Column(name = "user_uuid", length = 36)
    private String userUuid;

    @Column(name = "query_name", nullable = false)
    private String queryName;

    @Column(name = "origin_price")
    private Integer originPrice;

    @Column(name = "found_price")
    private Integer foundPrice;

    @Column(name = "product_id")
    private String productId;

    /** HIT | MISS | FAIL | SUCCESS */
    @Column(name = "status", nullable = false)
    private String status;

    /** cache | fastpath | slowpath */
    @Column(name = "source")
    private String source;

    @Column(name = "elapsed_ms")
    private Double elapsedMs;

    /** JSON: TOP 3 prices */
    @Column(name = "top_prices", columnDefinition = "TEXT")
    private String topPrices;

    /** JSON: price history */
    @Column(name = "price_trend", columnDefinition = "TEXT")
    private String priceTrend;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
