package com.example.genau.team.service;

import com.example.genau.team.domain.Teammates;
import com.example.genau.team.dto.TeamCreateRequest;
import com.example.genau.team.dto.TeamMemberDto;
import com.example.genau.team.dto.TeamSummaryDto;
import com.example.genau.team.dto.TeamUpdateRequestDto;
import com.example.genau.team.domain.Team;
import com.example.genau.team.repository.TeamRepository;
import com.example.genau.team.repository.TeammatesRepository;
import com.example.genau.user.domain.User;
import com.example.genau.user.repository.UserRepository;
import com.example.genau.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeammatesRepository teammatesRepository;
    private final UserRepository userRepository;          // ← UserRepository 주입
    private final EmailService emailService;              // ← 이메일 발송 서비스


    // 팀 생성 메서드
    public Team createTeam(TeamCreateRequest request) {
        // 1. 팀 저장
        Team team = new Team();
        team.setUserId(request.getUserId());
        team.setTeamName(request.getTeamName());
        team.setTeamDesc(request.getTeamDesc());
        team.setTeamProfileImg(request.getTeamProfileImg()); //
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

    // 팀 정보 조회
    public Team getTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다. id=" + teamId));
    }

    // 팀 정보 수정
    @Transactional
    public void updateTeam(Long teamId, Long requestingUserId, TeamUpdateRequestDto dto) {
        // 1) 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("해당 팀이 없습니다. id=" + teamId));

        // 2) 요청자가 팀장인지 체크
        boolean isManager =
                team.getUserId().equals(requestingUserId)
                        || teammatesRepository.existsByTeamIdAndUserIdAndIsManagerTrue(teamId, requestingUserId);

        if (!isManager) {
            throw new AccessDeniedException("팀장만 팀 정보를 수정할 수 있습니다.");
        }

        // 3) null 아닌 필드만 덮어쓰기
        if (dto.getTeamName() != null)       team.setTeamName(dto.getTeamName());
        if (dto.getTeamDesc() != null)       team.setTeamDesc(dto.getTeamDesc());
        if (dto.getTeamProfileImg() != null) team.setTeamProfileImg(dto.getTeamProfileImg());

        // 4) 업데이트 타임스탬프
        team.setTeamUpdated(LocalDateTime.now());
        // (영속성 컨텍스트가 관리하므로 save() 생략 가능)
    }

    @Transactional
    public void leaveTeam(Long teamId, Long userId) {
        // 1) 팀이 존재하는지 확인
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다. id=" + teamId));

        // 2) 만약 요청자가 팀장(manager)이면 '팀 삭제 로직' 호출
        boolean isManager = teammatesRepository
                .existsByTeamIdAndUserIdAndIsManagerTrue(teamId, userId);
        if (isManager) {
            // 팀장 퇴장 → 팀 전체 삭제
            // (기존 deleteTeam 메서드를 재사용)
            deleteTeam(teamId, userId);
            return;
        }

        // 3) 일반 팀원 퇴장 → 해당 멤버만 삭제
        // deleteByTeamIdAndUserId는 멤버가 없으면 아무 동작도 하지 않습니다.
        teammatesRepository.deleteByTeamIdAndUserId(teamId, userId);
    }

    // 팀 삭제 (팀 생성자만)
    @Transactional
    public void deleteTeam(Long teamId, Long userId) {
        Team team = getTeam(teamId);
        if (!team.getUserId().equals(userId)) {
            throw new RuntimeException("팀 삭제 권한이 없습니다.");
        }
        // 연관된 팀원 레코드 먼저 삭제
        teammatesRepository.deleteAllByTeamId(teamId);
        // 실제 팀 삭제
        teamRepository.delete(team);
    }

    //팀원조회
    @Transactional(readOnly = true)
    public List<TeamMemberDto> listTeamMembers(Long teamId) {
        return teammatesRepository.findAllByTeamId(teamId).stream()
                .map(tm -> {
                    User user = userRepository.findById(tm.getUserId())
                            .orElseThrow();
                    return new TeamMemberDto(
                            user.getUserId(),
                            user.getUserName(),
                            tm.getIsManager(),
                            tm.getTeamParticipated(),
                            user.getProfileImg()      // 여기서 프로필 이미지 꺼내서 넣어줍니다.
                    );
                })
                .toList();
    }

    //사용자가 속한 모든 팀 스페이스 요약 조회
    @Transactional(readOnly = true)
    public List<TeamSummaryDto> getMyTeams(Long userId) {
        List<Teammates> all = teammatesRepository.findAllByUserId(userId);
        return all.stream().map(tm -> {
            Team t = teamRepository.findById(tm.getTeamId())
                    .orElseThrow(() -> new RuntimeException("팀이 없습니다. id=" + tm.getTeamId()));
            return new TeamSummaryDto(
                    t.getTeamId(),
                    t.getTeamName(),
                    t.getTeamDesc(),
                    t.getTeamProfileImg(),
                    tm.getIsManager(),
                    tm.getTeamParticipated(),
                    t.getTeamUpdated()
            );
        }).collect(Collectors.toList());
    }

    public String uploadTeamProfileImage(Long teamId, Long userId, MultipartFile file) throws Exception {
        // 1. 팀장 권한 체크 (기존 로직 재활용)
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("해당 팀이 없습니다. id=" + teamId));

        boolean isManager = team.getUserId().equals(userId)
                || teammatesRepository.existsByTeamIdAndUserIdAndIsManagerTrue(teamId, userId);
        if (!isManager) {
            throw new AccessDeniedException("팀장만 팀 프로필 이미지를 변경할 수 있습니다.");
        }

        // 2. 파일 검증 (UserService와 동일)
        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            throw new IllegalArgumentException("유효한 파일이 아닙니다.");
        }

        String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        if (!ext.matches("png|jpe?g")) {
            throw new IllegalArgumentException("이미지는 PNG/JPG만 업로드 가능합니다.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("이미지 크기는 5MB 이하로 제한됩니다.");
        }

        // 3. 팀별 동적 경로로 파일 저장
        String dir = System.getProperty("user.dir") + "/uploads/teams/" + teamId;
        Path uploadPath = Paths.get(dir);
        if (Files.notExists(uploadPath)) Files.createDirectories(uploadPath);

        String filename = "team_profile." + ext;
        Path target = uploadPath.resolve(filename);
        file.transferTo(target.toFile());

        // 4. DB 업데이트
        String url = "/uploads/teams/" + teamId + "/" + filename;
        team.setTeamProfileImg(url);
        team.setTeamUpdated(LocalDateTime.now());
        teamRepository.save(team);

        return url;
    }

}
