package com.example.genau.invitation.service;

import com.example.genau.invitation.domain.Invitation;
import com.example.genau.invitation.dto.*;
import com.example.genau.invitation.repository.InvitationRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final TeamRepository teamRepository;
    private final TeammatesRepository teammatesRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /** 1) 초대 링크 생성 및 메일 발송 */
    @Transactional
    public InviteCreateResponseDto createInvitation(InviteCreateRequestDto req) {
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

        // 메일
        String link = "http://localhost:8080/invitations/validate?token=" + token;
        String html = """
            <p>GENAU에 초대되었습니다!</p>
            <p><a href="%s">여기를 클릭</a>하여 팀 초대를 수락하세요.</p>
            """.formatted(link);
        emailService.sendHtmlMail(req.getEmail(), "[GENAU] 팀 초대", html);

        return new InviteCreateResponseDto(token, expires);
    }

    /** 2) 토큰 유효성 검증 */
    @Transactional(readOnly = true)
    public InviteValidateResponseDto validateInvitation(String token) {
        Invitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 토큰입니다."));

        boolean valid = !inv.isAccepted() && LocalDateTime.now().isBefore(inv.getExpiresAt());

        InviteValidateResponseDto dto = new InviteValidateResponseDto();
        dto.setValid(valid);
        dto.setTeamId(inv.getTeamId());
        dto.setEmail(inv.getEmail());
        return dto;
    }

    /** 3) 초대 수락 → teammates에 등록 */
    @Transactional
    public void acceptInvitation(InviteAcceptRequestDto req) {
        Invitation inv = invitationRepository.findByToken(req.getToken())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 토큰입니다."));

        if (inv.isAccepted()) {
            throw new RuntimeException("이미 수락된 초대입니다.");
        }
        if (LocalDateTime.now().isAfter(inv.getExpiresAt())) {
            throw new RuntimeException("만료된 초대입니다.");
        }

        // 사용자 확인
        User user = userRepository.findByMail(inv.getEmail())
                .orElseThrow(() -> new RuntimeException("가입되지 않은 사용자입니다."));

        // 중복 가입 방지
        boolean already = teammatesRepository
                .existsByTeamIdAndUserId(inv.getTeamId(), user.getUserId());
        if (already) {
            throw new RuntimeException("이미 팀원으로 등록되어 있습니다.");
        }

        // teammates 레코드 생성
        teammatesRepository.save(
                new Teammates(null, user.getUserId(), inv.getTeamId(),
                        LocalDateTime.now(), false)
        );

        // 초대 상태 업데이트
        inv.setAccepted(true);
        invitationRepository.save(inv);
    }
}
