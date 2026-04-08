package com.picsel.backend_v2.service;

import com.picsel.backend_v2.domain.BenefitOffer;
import com.picsel.backend_v2.repository.BenefitOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BenefitService {

    private final BenefitOfferRepository benefitOfferRepository;

    /**
     * 모든 활성 카드 혜택을 조회하여 가맹점/금액 기준으로 비교합니다.
     * 카드 등록 없이 DB의 전체 BenefitOffer를 provider별로 그룹핑하여 best 혜택을 반환합니다.
     */
    public List<Map<String, Object>> compareAll(String merchant, double amount) {
        List<BenefitOffer> activeOffers = benefitOfferRepository.findByActiveTrue();

        // provider별로 best 혜택 계산
        Map<String, Map<String, Object>> bestByProvider = new LinkedHashMap<>();

        for (BenefitOffer o : activeOffers) {
            if (!isActiveNow(o.getStartDate(), o.getEndDate())) continue;
            if (!matchMerchant(merchant, o.getMerchantFilter())) continue;
            if (o.getMinSpend() != null && o.getMinSpend().doubleValue() > amount) continue;

            double saved = calcDiscount(amount, o.getDiscountType().name(),
                    o.getDiscountValue().doubleValue(),
                    o.getMaxDiscount() != null ? o.getMaxDiscount().doubleValue() : null);

            String provider = o.getProviderName();
            Map<String, Object> existing = bestByProvider.get(provider);

            if (existing == null || saved > (Double) existing.get("saved")) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("provider_name", provider);
                entry.put("offer_id", o.getId());
                entry.put("title", o.getTitle());
                entry.put("discount_type", o.getDiscountType().name());
                entry.put("discount_value", o.getDiscountValue());
                entry.put("saved", saved);
                entry.put("final_price", Math.max(0, amount - saved));
                bestByProvider.put(provider, entry);
            }
        }

        List<Map<String, Object>> results = new ArrayList<>(bestByProvider.values());
        results.sort((a, b) -> Double.compare((Double) b.get("saved"), (Double) a.get("saved")));
        return results;
    }

    public List<Map<String, Object>> top3(String merchant, double amount) {
        List<Map<String, Object>> compared = compareAll(merchant, amount);
        return compared.size() > 3 ? compared.subList(0, 3) : compared;
    }

    public List<Map<String, Object>> extractFromHtml(String htmlOrText) {
        List<Map<String, Object>> offers = new ArrayList<>();
        if (htmlOrText == null || htmlOrText.isBlank()) return offers;

        // HTML 태그 제거
        String text = htmlOrText.replaceAll("<[^>]+>", " ");

        // 패턴: "카드명 N% 할인" 또는 "카드명 N원 할인"
        Pattern pPercent = Pattern.compile("([가-힣a-zA-Z]+(?:카드|페이|CARD|Pay)?)\\s+(\\d+(?:\\.\\d+)?)%\\s*할인",
                Pattern.CASE_INSENSITIVE);
        Pattern pFlat = Pattern.compile("([가-힣a-zA-Z]+(?:카드|페이|CARD|Pay)?)\\s+(\\d+(?:,\\d+)*)원\\s*할인",
                Pattern.CASE_INSENSITIVE);

        Matcher m = pPercent.matcher(text);
        while (m.find()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("provider_name", m.group(1));
            o.put("discount_type", "PERCENT");
            o.put("discount_value", Double.parseDouble(m.group(2)));
            o.put("title", m.group(0).trim());
            offers.add(o);
        }

        m = pFlat.matcher(text);
        while (m.find()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("provider_name", m.group(1));
            o.put("discount_type", "FLAT");
            o.put("discount_value", Double.parseDouble(m.group(2).replaceAll(",", "")));
            o.put("title", m.group(0).trim());
            offers.add(o);
        }

        return offers;
    }

    public List<Map<String, Object>> top3WithExtraOffers(String merchant, double amount,
                                                          List<Map<String, Object>> extra) {
        List<Map<String, Object>> baseline = compareAll(merchant, amount);

        for (Map<String, Object> b : baseline) {
            String providerName = (String) b.get("provider_name");
            double currentSaved = (Double) b.get("saved");
            double extraSaved = 0;
            String extraTitle = null;

            for (Map<String, Object> e : extra) {
                if (!providerName.equalsIgnoreCase((String) e.get("provider_name"))) continue;
                double saved = calcDiscount(amount, (String) e.get("discount_type"),
                        ((Number) e.get("discount_value")).doubleValue(), null);
                if (saved > extraSaved) {
                    extraSaved = saved;
                    extraTitle = (String) e.get("title");
                }
            }

            if (extraSaved > currentSaved) {
                b.put("saved", extraSaved);
                b.put("final_price", Math.max(0, amount - extraSaved));
                if (extraTitle != null) b.put("title", extraTitle);
            }
        }

        baseline.sort((a, b) -> Double.compare((Double) b.get("saved"), (Double) a.get("saved")));
        return baseline.size() > 3 ? baseline.subList(0, 3) : baseline;
    }

    public List<Map<String, Object>> getAllActiveOffers() {
        List<BenefitOffer> offers = benefitOfferRepository.findByActiveTrue();
        List<Map<String, Object>> result = new ArrayList<>();
        for (BenefitOffer o : offers) {
            if (!isActiveNow(o.getStartDate(), o.getEndDate())) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", o.getId());
            entry.put("provider_name", o.getProviderName());
            entry.put("title", o.getTitle());
            entry.put("description", o.getDescription());
            entry.put("merchant_filter", o.getMerchantFilter());
            entry.put("discount_type", o.getDiscountType().name());
            entry.put("discount_value", o.getDiscountValue());
            entry.put("max_discount", o.getMaxDiscount());
            entry.put("min_spend", o.getMinSpend());
            entry.put("end_date", o.getEndDate());
            result.add(entry);
        }
        return result;
    }

    private boolean isActiveNow(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();
        if (start != null && now.isBefore(start)) return false;
        if (end != null && now.isAfter(end)) return false;
        return true;
    }

    private boolean matchMerchant(String merchant, String filter) {
        if (filter == null || filter.isBlank()) return true;
        return merchant.toLowerCase().contains(filter.toLowerCase());
    }

    private double calcDiscount(double amount, String discountType, double discountValue, Double maxDiscount) {
        double saved;
        if ("PERCENT".equals(discountType)) {
            saved = amount * discountValue / 100.0;
        } else {
            saved = discountValue;
        }
        if (maxDiscount != null) saved = Math.min(saved, maxDiscount);
        return Math.floor(saved);
    }
}
