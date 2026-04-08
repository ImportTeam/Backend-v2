package com.picsel.backend_v2.domain.ext;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_failures", indexes = {
        @Index(name = "idx_search_failure_query", columnList = "original_query"),
        @Index(name = "idx_search_failure_created", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SearchFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_query", nullable = false, length = 255)
    private String originalQuery;

    @Column(name = "normalized_query", length = 255)
    private String normalizedQuery;

    /** JSON: attempted search candidates */
    @Column(name = "candidates", columnDefinition = "TEXT")
    private String candidates;

    @Builder.Default
    @Column(name = "attempted_count")
    private Integer attemptedCount = 1;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "category_detected", length = 50)
    private String categoryDetected;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;

    /** pending | manual_fixed | auto_learned | not_product */
    @Builder.Default
    @Column(name = "is_resolved", length = 50)
    private String isResolved = "pending";

    @Column(name = "correct_product_name", length = 255)
    private String correctProductName;

    @Column(name = "correct_pcode", length = 20)
    private String correctPcode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
