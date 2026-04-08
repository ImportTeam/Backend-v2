package com.picsel.backend_v2.repository;

import com.picsel.backend_v2.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByRefreshToken(String refreshToken);
    List<UserSession> findByUserSeqOrderByCreatedAtDesc(Long userSeq);
    void deleteByRefreshToken(String refreshToken);
    void deleteByUserSeq(Long userSeq);
    Optional<UserSession> findBySeqAndUserSeq(Long seq, Long userSeq);
}
