// TeamCatResponseDto.java
package com.example.genau.category.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponseDto {
    private Long catId;
    private String catName;
}
