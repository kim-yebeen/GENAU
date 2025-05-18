package com.example.genau.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NameRequestDto {
    @NotBlank(message = "이름은 비어있을 수 없습니다.")
    private String name;
}

