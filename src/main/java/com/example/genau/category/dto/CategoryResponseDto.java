package com.example.genau.category.dto;

import com.example.genau.category.domain.Category;
import lombok.Getter;

@Getter
public class CategoryResponseDto {
    private Long catId;
    private String catName;

    public static CategoryResponseDto from(Category cat) {
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.catId = cat.getCatId();
        dto.catName = cat.getCatName();
        return dto;
    }
}
