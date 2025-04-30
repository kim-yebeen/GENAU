package com.example.genau.team.service;

import com.example.genau.team.domain.Teammates;
import com.example.genau.team.dto.TeamCreateRequest;
import com.example.genau.team.dto.TeamUpdateRequestDto;
import com.example.genau.team.domain.Team;
import com.example.genau.team.repository.TeamRepository;
import com.example.genau.team.repository.TeammatesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeammatesRepository teammatesRepository;
    // 팀 생성 메서드
    public Team createTeam(TeamCreateRequest request) {
        // 1. 팀 저장
        Team team = new Team();
        team.setUserId(request.getUserId());
        team.setTeamName(request.getTeamName());
        team.setTeamDesc(request.getTeamDesc());
        team.setTeamProfileImg(request.getTeamProfileImg()); // ✅ 프로필 사진 저장
        team.setTeamCreated(LocalDateTime.now());
        team.setTeamUpdated(LocalDateTime.now());

        Team savedTeam = teamRepository.save(team); // DB에 저장

        // 2. 팀 생성자(팀장) teammates에 등록
        Teammates teammates = new Teammates();
        teammates.setUserId(request.getUserId());
        teammates.setTeamId(savedTeam.getTeamId());
        teammates.setTeamParticipated(LocalDateTime.now());
        teammates.setIsManager(true); // ✅ 생성자는 무조건 팀장
        teammatesRepository.save(teammates);

        return savedTeam;
    }

    @Transactional
    public void updateTeam(Long teamId, TeamUpdateRequestDto dto) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다. id=" + teamId));
        team.setTeamName(dto.getTeamName());
        team.setTeamDesc(dto.getTeamDes());
        team.setTeamProfileImg(dto.getTeamImage());
        // 변경 감지(dirty checking)로 자동 저장
    }
}