package com.picsel.backend_v2.dto.price;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class PriceSearchRequest {

    @Size(min = 1, max = 500, message = "상품명은 1~500자 이내로 입력해주세요.")
    @JsonProperty("product_name")
    private String productName;

    @Min(0) @Max(1_000_000_000)
    @JsonProperty("current_price")
    private Integer currentPrice;

    @Size(max = 2048)
    @JsonProperty("current_url")
    private String currentUrl;

    @JsonProperty("product_code")
    private String productCode;

    @JsonProperty("selected_options")
    private List<SelectedOption> selectedOptions;

    @Size(max = 3000)
    @JsonProperty("options_text")
    private String optionsText;
}
