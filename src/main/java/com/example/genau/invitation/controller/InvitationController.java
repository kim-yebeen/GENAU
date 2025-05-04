package com.example.genau.invitation.controller;

import com.example.genau.invitation.dto.*;
import com.example.genau.invitation.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    /** 1) 초대 링크 생성 & 이메일 전송 */
    @PostMapping
    public ResponseEntity<InviteCreateResponseDto> create(
            @RequestBody InviteCreateRequestDto req) {
        InviteCreateResponseDto dto = invitationService.createInvitation(req);
        return ResponseEntity.ok(dto);
    }

    /** 2) 링크 유효성 검증 */
    @GetMapping("/validate")
    public ResponseEntity<InviteValidateResponseDto> validate(
            @RequestParam String token) {
        InviteValidateResponseDto dto = invitationService.validateInvitation(token);
        return ResponseEntity.ok(dto);
    }

    /** 3) 초대 수락 (팀원 등록) */
    @PostMapping("/accept")
    public ResponseEntity<?> accept(
            @RequestBody InviteAcceptRequestDto req) {
        invitationService.acceptInvitation(req);
        return ResponseEntity.ok().body(
                java.util.Map.of("message", "초대를 수락했습니다.")
        );
    }
}
