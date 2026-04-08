package com.picsel.backend_v2.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "benefit_offers", indexes = {
        @Index(name = "idx_bo_provider_name", columnList = "provider_name"),
        @Index(name = "idx_bo_active_end", columnList = "active, end_date"),
        @Index(name = "idx_bo_active_provider_end", columnList = "active, provider_name, end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenefitOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "merchant_filter", length = 255)
    private String merchantFilter;

    @Column(name = "category_filter", length = 100)
    private String categoryFilter;

    @Column(name = "min_spend", precision = 19, scale = 2)
    private BigDecimal minSpend;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount", precision = 19, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "source_url", length = 255)
    private String sourceUrl;

    @Column(unique = true, length = 64)
    private String hash;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
