package com.example.genau.invitation.service;

import com.example.genau.invitation.domain.Invitation;
import com.example.genau.invitation.dto.*;
import com.example.genau.invitation.repository.InvitationRepository;
import com.example.genau.notice.service.NotificationService;
import com.example.genau.team.domain.Team;
import com.example.genau.team.domain.Teammates;
import com.example.genau.team.repository.TeamRepository;
import com.example.genau.team.repository.TeammatesRepository;
import com.example.genau.user.repository.UserRepository;
import com.example.genau.user.domain.User;
import com.example.genau.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final TeamRepository teamRepository;
    private final TeammatesRepository teammatesRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    /** 1) 초대 링크 생성 및 메일 발송 */
    @Transactional
    public InviteCreateResponseDto createInvitation(InviteCreateRequestDto req) {
        // 팀 존재 여부 확인
        Team team = teamRepository.findById(req.getTeamId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 팀입니다."));

        // 이미 초대된 이메일인지 확인 (미수락 상태)
        boolean existingInvitation = invitationRepository
                .existsByEmailAndTeamIdAndAcceptedFalse(req.getEmail(), req.getTeamId());
        if (existingInvitation) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "이미 초대가 진행 중인 사용자입니다.");
        }

        // 이미 팀원인지 확인
        User existingUser = userRepository.findByMail(req.getEmail()).orElse(null);
        if (existingUser != null) {
            boolean isAlreadyMember = teammatesRepository
                    .existsByTeamIdAndUserId(req.getTeamId(), existingUser.getUserId());
            if (isAlreadyMember) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "이미 팀원으로 등록된 사용자입니다.");
            }
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expires = LocalDateTime.now().plusDays(7);

        Invitation inv = Invitation.builder()
                .teamId(req.getTeamId())
                .email(req.getEmail())
                .token(token)
                .createdAt(LocalDateTime.now())
                .expiresAt(expires)
                .accepted(false)
                .build();

        invitationRepository.save(inv);

        // 메일 발송
        String link = "http://localhost:5173/invitations/validate?token=" + token;
        String html = """
            <p>%s 팀에 초대되었습니다!</p>
            <p><a href="%s">여기를 클릭</a>하여 팀 초대를 수락하세요.</p>
            <p>이 링크는 7일 후 만료됩니다.</p>
            """.formatted(team.getTeamName(), link);

        try {
            emailService.sendHtmlMail(req.getEmail(), "[GENAU] " + team.getTeamName() + " 팀 초대", html);
        } catch (Exception e) {
            // 이메일 발송 실패 시 초대 레코드 삭제
            invitationRepository.delete(inv);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다.");
        }

        return new InviteCreateResponseDto(token, expires);
    }

    /** 2) 토큰 유효성 검증 */
    @Transactional(readOnly = true)
    public InviteValidateResponseDto validateInvitation(String token) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 초대 링크입니다."));

        boolean valid = !inv.isAccepted() && LocalDateTime.now().isBefore(inv.getExpiresAt());

        // 팀 정보도 함께 반환
        Team team = null;
        if (valid) {
            team = teamRepository.findById(inv.getTeamId()).orElse(null);
        }

        InviteValidateResponseDto dto = new InviteValidateResponseDto();
        dto.setValid(valid);
        dto.setTeamId(inv.getTeamId());
        dto.setEmail(inv.getEmail());

        if (team != null) {
            dto.setTeamName(team.getTeamName());
            dto.setTeamDesc(team.getTeamDesc());
        }

        return dto;
    }

    /** 3) 초대 수락 → teammates에 등록 */
    @Transactional
    public InviteAcceptResponseDto acceptInvitation(InviteAcceptRequestDto req) {
        // 1) 토큰 조회 및 기본 유효성 검사
        Invitation inv = invitationRepository.findByToken(req.getToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 초대 링크입니다."));

        if (inv.isAccepted()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "이미 수락된 초대입니다.");
        }

        if (LocalDateTime.now().isAfter(inv.getExpiresAt())) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, "만료된 초대입니다.");
        }

        // 2) 사용자 검증 - 요청한 userId와 초대된 이메일이 일치하는지 확인
        User requestUser = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "존재하지 않는 사용자입니다."));

        // 초대된 이메일과 요청 사용자의 이메일이 일치하는지 확인
        if (!requestUser.getMail().equals(inv.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "초대받은 사용자만 수락할 수 있습니다.");
        }

        // 3) 중복 가입 방지 - 다시 한 번 확인
        boolean alreadyMember = teammatesRepository
                .existsByTeamIdAndUserId(inv.getTeamId(), requestUser.getUserId());
        if (alreadyMember) {
            // 이미 팀원이면 초대를 수락 처리하고 팀 정보 반환
            inv.setAccepted(true);
            invitationRepository.save(inv);

            Team team = teamRepository.findById(inv.getTeamId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "팀이 존재하지 않습니다."));

            return new InviteAcceptResponseDto(
                    team.getTeamId(),
                    team.getTeamName(),
                    team.getTeamDesc()
            );
        }

        // 4) 팀 존재 여부 확인
        Team team = teamRepository.findById(inv.getTeamId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "팀이 존재하지 않습니다."));

        // 5) teammates 레코드 생성
        Teammates newTeammate = new Teammates(
                null,
                requestUser.getUserId(),
                inv.getTeamId(),
                LocalDateTime.now(),
                false // isManager = false
        );

        Teammates savedTeammate = teammatesRepository.save(newTeammate);

        // 6) 초대 상태 업데이트
        inv.setAccepted(true);
        invitationRepository.save(inv);

        // 7) 알림 생성
        try {
            notificationService.createTeamJoinNotification(savedTeammate.getTeammatesId());
        } catch (Exception e) {
            // 알림 생성 실패는 로그만 남기고 진행
            System.err.println("알림 생성 실패: " + e.getMessage());
        }

        return new InviteAcceptResponseDto(
                team.getTeamId(),
                team.getTeamName(),
                team.getTeamDesc()
        );
    }
}

