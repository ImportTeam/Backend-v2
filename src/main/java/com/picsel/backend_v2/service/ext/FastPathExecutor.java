package com.picsel.backend_v2.service.ext;

import com.picsel.backend_v2.dto.price.TopPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP 기반 다나와 크롤링 FastPath.
 * Jsoup으로 검색 결과 페이지를 파싱하여 최저가 정보를 추출합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FastPathExecutor {

    @Value("${ext.crawler.user-agent}")
    private String userAgent;

    @Value("${ext.crawler.fastpath-timeout-ms:8000}")
    private int timeoutMs;

    @Value("${ext.crawler.min-html-length:5000}")
    private int minHtmlLength;

    private static final String SEARCH_URL = "https://search.danawa.com/dsearch.php?query=%s&originalQuery=%s&tab=goods";
    private static final Pattern PCODE_PATTERN = Pattern.compile("[?&]pcode=(\\d+)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("[,\\s]");

    /**
     * @return Optional.empty() 이면 FastPath 실패 → SlowPath 시도
     */
    public Optional<Map<String, Object>> execute(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format(SEARCH_URL, encoded, encoded);

            log.debug("[FastPath] GET {}", url);
            Document doc = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(timeoutMs)
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .get();

            if (doc.html().length() < minHtmlLength) {
                log.warn("[FastPath] HTML too short ({}), possible block", doc.html().length());
                return Optional.empty();
            }

            return parseSearchResults(doc, query);

        } catch (Exception e) {
            log.warn("[FastPath] Error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Map<String, Object>> parseSearchResults(Document doc, String query) {
        // 다나와 검색 결과 목록
        Elements items = doc.select("ul.product_list > li.prod_item, li.prod_item.prod_layer");

        if (items.isEmpty()) {
            // 대체 셀렉터
            items = doc.select(".product_list_wrap .prod_item");
        }
        if (items.isEmpty()) {
            log.debug("[FastPath] No product items found");
            return Optional.empty();
        }

        List<TopPrice> topPrices = new ArrayList<>();
        Integer lowestPrice = null;
        String productUrl = null;
        String productName = null;
        String productId = null;
        String mall = null;
        boolean freeShipping = false;

        int rank = 1;
        for (Element item : items) {
            if (rank > 3) break;

            // 상품명
            Element nameEl = item.selectFirst(".prod_name a, .prod_name_e a, a.click_log_product_standard_title_link");
            // 가격
            Element priceEl = item.selectFirst(".price_sect a strong, .lowest_price .txt_num, .pricelist-kmd strong");
            // 링크
            Element linkEl = item.selectFirst("a[href*=pcode], a[href*=prod.danawa]");

            if (priceEl == null) continue;

            Integer price = parsePrice(priceEl.text());
            if (price == null || price <= 0) continue;

            String itemUrl = linkEl != null ? linkEl.absUrl("href") : "";
            String itemMall = extractMall(item);
            boolean itemFreeShipping = item.text().contains("무료") || item.text().contains("무료배송");
            String pcode = extractPcode(itemUrl);

            if (rank == 1) {
                lowestPrice = price;
                productUrl = itemUrl;
                productId = pcode;
                mall = itemMall;
                freeShipping = itemFreeShipping;
                if (nameEl != null) productName = nameEl.text().trim();
            }

            topPrices.add(TopPrice.builder()
                    .rank(rank)
                    .mall(itemMall)
                    .price(price)
                    .freeShipping(itemFreeShipping)
                    .delivery(itemFreeShipping ? "무료배송" : "")
                    .link(itemUrl)
                    .build());
            rank++;
        }

        if (lowestPrice == null) return Optional.empty();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product_name", productName != null ? productName : query);
        result.put("product_id", productId);
        result.put("lowest_price", lowestPrice);
        result.put("price", lowestPrice);
        result.put("product_url", productUrl);
        result.put("source", "fastpath");
        result.put("mall", mall);
        result.put("free_shipping", freeShipping);
        result.put("top_prices", topPrices);
        result.put("price_trend", null);
        result.put("updated_at", java.time.Instant.now().toString());

        log.debug("[FastPath] OK: {} → {}원", productName, lowestPrice);
        return Optional.of(result);
    }

    private Integer parsePrice(String text) {
        try {
            String digits = PRICE_PATTERN.matcher(text).replaceAll("").replaceAll("[^0-9]", "");
            return digits.isEmpty() ? null : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractMall(Element item) {
        Element mallEl = item.selectFirst(".mall_name, .logo_img img, .shop_logo img");
        if (mallEl != null) {
            String alt = mallEl.attr("alt");
            if (!alt.isBlank()) return alt;
            return mallEl.text().trim();
        }
        return "다나와";
    }

    private String extractPcode(String url) {
        if (url == null) return null;
        Matcher m = PCODE_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }
}
