package com.example.genau.user.service;

import com.example.genau.user.dto.LoginRequestDto;
import com.example.genau.user.dto.SignupRequestDto;
import com.example.genau.user.domain.User;
import com.example.genau.user.repository.UserRepository;
import com.example.genau.user.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    // Redis key prefixes
    private static final String CODE_KEY     = "verify:";
    public static final String FLAG_KEY     = "verified:";

    /** 1) 인증 코드 발송 */
    public void sendEmailVerificationCode(String email) {
        String code = String.valueOf(new Random().nextInt(900_000) + 100_000);
        // 5분간 저장
        redisTemplate.opsForValue().set(CODE_KEY + email, code, Duration.ofMinutes(5));
        // TODO: 실제 메일 전송 로직 호출
        System.out.println("이메일 인증코드: " + code);
    }

    /** 2) 인증 코드 검증 */
    public boolean verifyEmailCode(String email, String code) {
        String saved = redisTemplate.opsForValue().get(CODE_KEY + email);
        if (saved != null && saved.equals(code)) {
            // 맞으면 플래그를 3분간 찍어두고 true 리턴
            redisTemplate.opsForValue().set(FLAG_KEY + email, "true", Duration.ofMinutes(3));
            return true;
        }
        return false;
    }



    /** 4) 회원가입 */
    public void signup(SignupRequestDto dto) {
        // 중복 체크
        if (userRepository.existsByMail(dto.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }
        // 인증 플래그 확인
        String verified = redisTemplate.opsForValue().get(FLAG_KEY + dto.getEmail());
        if (!"true".equals(verified)) {
            throw new RuntimeException("이메일 인증되지 않음");
        }
        // 저장
        User user = User.builder()
                .mail(dto.getEmail())
                .userName(dto.getName())
                .userPw(passwordEncoder.encode(dto.getPassword())) // 나중에 암호화 적용
                .build();
        userRepository.save(user);
        System.out.println("회원가입 완료: " + dto.getEmail());

        // (선택) 키 정리
        redisTemplate.delete(CODE_KEY + dto.getEmail());
        redisTemplate.delete(FLAG_KEY + dto.getEmail());
    }

    /** 5) 로그인 */
    public Map<String, Object> login(LoginRequestDto dto) {
        User user = userRepository.findByMail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일입니다."));

        // 암호화된 비밀번호 비교로 수정
        if (!passwordEncoder.matches(dto.getPassword(), user.getUserPw())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // JWT 토큰 생성
        String token = jwtUtil.createToken(user.getUserId(), user.getUserName());

        return Map.of(
                "message", "로그인 성공",
                "userId", user.getUserId(),
                "name", user.getUserName(),
                "token", token
        );
    }

    // 로그아웃 메서드 수정
    public Map<String, String> logout(String token) {
        // 토큰을 블랙리스트에 추가
        tokenBlacklistService.blacklistToken(token);
        return Map.of("message", "로그아웃 되었습니다.");
    }

    // 회원탈퇴 메서드에도 토큰 무효화 추가
    @Transactional
    public void deleteAccount(Long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 현재 토큰을 블랙리스트에 추가
        tokenBlacklistService.blacklistToken(token);

        userRepository.delete(user);
    }
}
