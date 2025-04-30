package com.example.genau.team.controller;

import com.example.genau.team.domain.Team;
import com.example.genau.team.dto.TeamCreateRequest;
import com.example.genau.team.dto.TeamUpdateRequestDto;
import com.example.genau.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;


    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody TeamCreateRequest request) {
        Team created = teamService.createTeam(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    /**
     * PUT /teams/{teamId}
     * Update an existing team’s metadata.
     */
    @PutMapping("/{teamId}")
    public ResponseEntity<Map<String, String>> updateTeam(
            @PathVariable Long teamId,
            @RequestBody TeamUpdateRequestDto dto) {
        teamService.updateTeam(teamId, dto);
        return ResponseEntity
                .ok(Map.of("message", "팀 정보가 업데이트되었습니다."));
    }
}
