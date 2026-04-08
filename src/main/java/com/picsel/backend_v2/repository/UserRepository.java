package com.picsel.backend_v2.repository;

import com.picsel.backend_v2.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUuid(String uuid);
    boolean existsByEmail(String email);
}
