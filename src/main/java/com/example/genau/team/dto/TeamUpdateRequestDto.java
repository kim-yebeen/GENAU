package com.example.genau.team.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamUpdateRequestDto {
    private String teamName;
    private String teamDes;
    private String teamImage; // (옵션) URL or Base64
}
