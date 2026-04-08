package com.picsel.backend_v2.dto.price;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class SelectedOption {
    private String name;
    private String value;
}
