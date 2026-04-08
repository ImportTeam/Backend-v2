package com.picsel.backend_v2.dto.price;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriceSearchResponse {

    private String status;   // success | error
    private Data data;
    private String message;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("selected_options")
    private List<SelectedOption> selectedOptions;

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Data {
        @JsonProperty("product_name")
        private String productName;

        @JsonProperty("product_id")
        private String productId;

        @JsonProperty("is_cheaper")
        private Boolean isCheaper;

        @JsonProperty("price_diff")
        private Integer priceDiff;

        @JsonProperty("lowest_price")
        private Integer lowestPrice;

        private String link;
        private String mall;

        @JsonProperty("free_shipping")
        private Boolean freeShipping;

        @JsonProperty("top_prices")
        private List<TopPrice> topPrices;

        @JsonProperty("price_trend")
        private Object priceTrend;

        @JsonProperty("selected_options")
        private List<SelectedOption> selectedOptions;

        private String source;

        @JsonProperty("elapsed_ms")
        private Double elapsedMs;
    }
}
