package com.example.genau.category.dto;

import com.example.genau.category.domain.Category;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CategoryResponseDto {
    private Long catId;
    private String catName;
    private String catColor;

    public CategoryResponseDto(Long catId, String catName, String catColor) {
        this.catId = catId;
        this.catName = catName;
        this.catColor = catColor;
    }

}
