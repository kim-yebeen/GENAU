package com.example.genau.team.controller;

import com.example.genau.team.domain.Team;
import com.example.genau.team.dto.TeamCreateRequest;
import com.example.genau.team.dto.TeamMemberDto;
import com.example.genau.team.dto.TeamSummaryDto;
import com.example.genau.team.dto.TeamUpdateRequestDto;
import com.example.genau.team.service.TeamService;
import com.example.genau.user.security.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;

    //팀 생성
    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody TeamCreateRequest request) {
        Team created = teamService.createTeam(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    //팀 정보 조회
    @GetMapping("/{teamId}")
    public ResponseEntity<Team> getTeam(@PathVariable Long teamId) {
        Team team = teamService.getTeam(teamId);
        return ResponseEntity.ok(team);
    }

    //팀 정보 수정
    // 3) 팀 정보 수정 (userId도 body 에서 꺼냄)
    @PatchMapping("/{teamId}")
    public ResponseEntity<?> updateTeam(
            @PathVariable Long teamId,
            @RequestBody TeamUpdateRequestDto dto
    ) {
        teamService.updateTeam(teamId, dto.getUserId(), dto);
        return ResponseEntity.ok(Map.of("message", "팀 정보가 업데이트되었습니다."));
    }

    // 4) 팀 나가기 (팀 생성자만 호출 가능)
    @PostMapping("/{teamId}/leave")
    public ResponseEntity<?> leaveTeam(
            @PathVariable Long teamId,
            @RequestBody Map<String, Long> body  // { "userId": ... }
    ) {
        Long userId = body.get("userId");
        teamService.leaveTeam(teamId, userId);
        return ResponseEntity.ok(Map.of("message", "팀에서 나갔습니다."));
    }

    // 5) 팀 삭제 (팀 생성자만 가능)
    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> deleteTeam(
            @PathVariable Long teamId,
            @RequestBody Map<String, Long> body  // { "userId": ... }
    ) {
        Long userId = body.get("userId");
        teamService.deleteTeam(teamId, userId);
        return ResponseEntity.ok(Map.of("message", "팀이 삭제되었습니다."));
    }

    // 팀원 전체 조회
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberDto>> getTeamMembers(@PathVariable Long teamId) {
        List<TeamMemberDto> members = teamService.listTeamMembers(teamId);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/users/{userId}/teams")
    public ResponseEntity<List<TeamSummaryDto>> listMyTeamsByUser(
            @PathVariable Long userId
    ) {
        List<TeamSummaryDto> teams = teamService.getMyTeams(userId);
        return ResponseEntity.ok(teams);
    }

    //image upload
    @PostMapping(value = "/{teamId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updateTeamProfileImage(
            @PathVariable Long teamId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        Long userId = AuthUtil.getCurrentUserId();
        String imageUrl = teamService.uploadTeamProfileImage(teamId, userId, file);
        return ResponseEntity.ok().body(Map.of("teamProfileImg", imageUrl));
    }


}