package com.example.genau.user.controller;

import com.example.genau.user.security.JwtUtil;
import com.example.genau.user.service.AuthService;
import com.example.genau.user.dto.*;
import com.example.genau.user.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final com.example.genau.user.repository.UserRepository userRepository;
    private final EmailService emailService;
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = userRepository.existsByMail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // 인증코드 전송
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody EmailRequestDto request) {
        try {
            // 반환값 없이 호출만!
            emailService.sendVerificationCode(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "인증번호가 전송되었습니다."));
        } catch (MessagingException | UnsupportedEncodingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "메일 발송에 실패했습니다."));
        }
    }


    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody EmailVerifyDto req) {
        boolean ok = emailService.verifyCode(req.getEmail(), req.getCode());
        return ResponseEntity.ok(Map.of("verified", ok));
    }


    //회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid SignupRequestDto requestDto) {
        authService.signup(requestDto);
        return ResponseEntity.ok(Map.of("message", "회원가입 완료"));
    }

    //로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        try {
            Map<String, Object> response = authService.login(loginRequestDto);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    //비밀번호 변경
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto dto) {
        try {
            authService.resetPassword(dto);
            return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증 토큰이 필요합니다."));
        }

        String token = authorization.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "유효하지 않은 토큰입니다."));
        }

        // (선택) 서버 측에서 토큰 무효화 작업: 블랙리스트에 등록한다면 여기에 로직 추가
        // tokenBlacklistService.blacklist(token);

        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다. 클라이언트에서 토큰을 삭제해주세요."));
    }
    /**
     * 회원탈퇴: 사용자 계정 삭제
     * 토큰에서 사용자 ID를 추출하여 계정 삭제
     */
    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "인증 토큰이 필요합니다."));
            }

            String token = authorization.substring(7);

            // 토큰 유효성 검증
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "유효하지 않은 토큰입니다."));
            }

            // 토큰에서 사용자 ID 추출
            Long userId = jwtUtil.getUserId(token);

            // 회원 탈퇴 실행
            authService.deleteAccount(userId);

            return ResponseEntity.ok(Map.of("message", "회원탈퇴가 완료되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
