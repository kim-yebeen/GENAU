package com.example.genau.user.service;

import com.example.genau.user.dto.LoginRequestDto;
import com.example.genau.user.dto.ResetPasswordRequestDto;
import com.example.genau.user.dto.SignupRequestDto;
import com.example.genau.user.domain.User;
import com.example.genau.user.repository.UserRepository;
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

    /** 3) 비밀번호 재설정 */
    public void resetPassword(ResetPasswordRequestDto dto) {
        // 플래그 확인
        String verified = redisTemplate.opsForValue().get(FLAG_KEY + dto.getEmail());
        if (!"true".equals(verified)) {
            throw new RuntimeException("이메일 인증되지 않음");
        }

        User user = userRepository.findByMail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("해당 이메일의 유저가 존재하지 않습니다."));

        // 3) 새 비밀번호 ≠ 기존 비밀번호 검증 (추가)
        if (passwordEncoder.matches(dto.getNewPassword(), user.getUserPw())) {
            throw new IllegalArgumentException("새 비밀번호는 이전 비밀번호와 달라야 합니다.");
        }

        // 4) 비밀번호 암호화 후 저장 (기존에 주석 처리된 부분을 활성화)
        user.setUserPw(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        // (선택) 키 정리
        redisTemplate.delete(CODE_KEY + dto.getEmail());
        redisTemplate.delete(FLAG_KEY + dto.getEmail());
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
                .userPw(dto.getPassword()) // 나중에 암호화 적용
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
        if (!user.getUserPw().equals(dto.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        return Map.of(
                "message", "로그인 성공",
                "userId",   user.getUserId(),
                "name",     user.getUserName()
        );
    }
}
