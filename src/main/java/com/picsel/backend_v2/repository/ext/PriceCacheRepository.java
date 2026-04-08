package com.picsel.backend_v2.repository.ext;

import com.picsel.backend_v2.domain.ext.PriceCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PriceCacheRepository extends JpaRepository<PriceCache, Long> {

    @Query("SELECT p FROM PriceCache p WHERE p.cacheKey = :key AND p.updatedAt >= :since")
    Optional<PriceCache> findFresh(@Param("key") String cacheKey, @Param("since") LocalDateTime since);

    Optional<PriceCache> findByCacheKey(String cacheKey);
}
