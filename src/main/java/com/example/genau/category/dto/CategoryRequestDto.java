package com.example.genau.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CategoryRequestDto {
    private Long teamId;

    @NotBlank(message = "카테고리명(catName)은 필수입니다.")
    private String catName;


}