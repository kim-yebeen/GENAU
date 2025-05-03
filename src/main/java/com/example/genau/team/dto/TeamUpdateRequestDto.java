package com.example.genau.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamUpdateRequestDto {
    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("teamName")
    private String teamName;

    // 요청 JSON 에는 "teamDesc" 키를, 내부 필드에는 teamDesc 로 바인딩
    @JsonProperty("teamDesc")
    private String teamDesc;

    // 요청 JSON 에는 "teamProfileImg" 키를, 내부 필드에는 teamProfileImg 로 바인딩
    @JsonProperty("teamProfileImg")
    private String teamProfileImg;
}
