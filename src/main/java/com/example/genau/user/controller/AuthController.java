package com.example.genau.user.controller;

import com.example.genau.user.service.AuthService;
import com.example.genau.user.dto.*;
import com.example.genau.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final com.example.genau.user.repository.UserRepository userRepository;
    private final EmailService emailService;
    private final AuthService authService;

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = userRepository.existsByMail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // 인증코드 전송
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = emailService.sendVerificationCode(email);
        return ResponseEntity.ok(Map.of("message", "인증번호가 전송되었습니다."));
    }

    // 인증코드 검증
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        boolean verified = emailService.verifyCode(email, code);
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    //회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequestDto requestDto) {
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


}
