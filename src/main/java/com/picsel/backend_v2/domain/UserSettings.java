package com.picsel.backend_v2.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @Column(name = "user_seq")
    private Long userSeq;

    @Column(name = "dark_mode", nullable = false)
    @Builder.Default
    private boolean darkMode = false;

    @Column(name = "notification_enabled", nullable = false)
    @Builder.Default
    private boolean notificationEnabled = true;

    @Column(name = "compare_mode", nullable = false)
    @Builder.Default
    private String compareMode = "AUTO";

    @Column(name = "currency_preference", nullable = false)
    @Builder.Default
    private String currencyPreference = "KRW";

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_seq")
    private User user;
}
