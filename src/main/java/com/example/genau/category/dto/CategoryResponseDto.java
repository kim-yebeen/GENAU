package com.example.genau.category.dto;

import com.example.genau.category.domain.Category;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CategoryResponseDto {
    private Long catId;
    private String catName;

    public CategoryResponseDto(Long catId, String catName) {
        this.catId = catId;
        this.catName = catName;
    }

}
