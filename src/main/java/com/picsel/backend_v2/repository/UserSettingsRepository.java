package com.picsel.backend_v2.repository;

import com.picsel.backend_v2.domain.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByUserSeq(Long userSeq);
}
