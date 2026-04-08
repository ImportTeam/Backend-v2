package com.picsel.backend_v2.dto.price;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class TopPrice {
    private int rank;
    private String mall;
    private int price;
    @JsonProperty("free_shipping")
    private Boolean freeShipping;
    private String delivery;
    private String link;
}
