package com.example.genau.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TeamWeeklyTodoDto {
    private Long teamId;                  // 팀 ID
    private String teamName;              // 팀 이름
    private List<WeekTodoDto> todos;      // 해당 팀의 이번 주 내 담당 TODO 리스트
}
