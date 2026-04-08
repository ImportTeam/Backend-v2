package com.picsel.backend_v2.repository;

import com.picsel.backend_v2.domain.BenefitOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenefitOfferRepository extends JpaRepository<BenefitOffer, Long> {
    List<BenefitOffer> findByActiveTrue();
    List<BenefitOffer> findByActiveTrueAndProviderNameIn(List<String> providerNames);
    List<BenefitOffer> findByActiveTrueAndProviderName(String providerName);
}
