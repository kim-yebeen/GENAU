// com.example.genau.user.service.UserService.java
package com.example.genau.user.service;

import com.example.genau.user.domain.User;
import com.example.genau.user.dto.UserProfileDto;
import com.example.genau.user.repository.UserRepository;
import com.example.genau.user.dto.EmailRequestDto;
import com.example.genau.user.dto.EmailVerifyDto;
import com.example.genau.user.dto.NameRequestDto;
import com.example.genau.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.example.genau.user.service.AuthService.FLAG_KEY;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;           // :contentReference[oaicite:0]{index=0}:contentReference[oaicite:1]{index=1}
    private final EmailService emailService;               // :contentReference[oaicite:2]{index=2}:contentReference[oaicite:3]{index=3}
    private final RedisTemplate<String,String> redisTemplate;

    private static final String CHANGE_EMAIL_FLAG = "GENAU:EMAIL:FLAG:";

    /** 0) 사용자 정보 조회 */
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. id=" + userId));
        return UserProfileDto.fromEntity(user);
    }

    /** 1) 이름 변경 */
    public void updateName(Long userId, String newName) {
        if (newName.length() > 50) {
            throw new IllegalArgumentException("이름은 50자 이하로 작성해야 합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. id=" + userId));
        user.setUserName(newName);
        userRepository.save(user);
    }

    /** 2) 프로필 이미지 변경 */
    public String updateProfileImage(Long userId, MultipartFile file) throws Exception {
        // 확장자·크기 검사
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

        // 저장
        String dir = System.getProperty("user.dir") + "/uploads/profiles";
        Path uploadPath = Paths.get(dir);
        if (Files.notExists(uploadPath)) Files.createDirectories(uploadPath);

        String filename = "user_" + userId + "." + ext;
        Path target = uploadPath.resolve(filename);
        file.transferTo(target.toFile());

        // DB 업데이트
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. id=" + userId));
        String url = "/uploads/profiles/" + filename;  // 실제 서비스에 맞춰 URL 매핑 필요
        user.setProfileImg(url);
        userRepository.save(user);

        return url;
    }

    /** 1) 전송 */
    public void sendEmailChangeCode(String newEmail) throws Exception {
        emailService.sendVerificationCode(newEmail);
    }

    /** 2) 새 이메일 인증코드 검증 후, 플래그 올리기 */
    public boolean verifyEmailChangeCode(String newEmail, String code) {
        boolean success = emailService.verifyCode(newEmail, code);
        if (success) {
            // “이 이메일은 변경 허가됨” 플래그를 Redis에 올려 둔다
            redisTemplate.opsForValue().set(CHANGE_EMAIL_FLAG + newEmail, "true");
        }
        return success;
    }

    /** 3) 플래그가 올라간 경우에만 실제 변경 */
    public void updateEmail(Long userId, String newEmail) {
        String flag = redisTemplate.opsForValue().get(CHANGE_EMAIL_FLAG + newEmail);
        if (!"true".equals(flag)) {
            throw new IllegalStateException("새 이메일 인증이 필요합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.setMail(newEmail);
        userRepository.save(user);

        // (선택) 사용 후 플래그 정리
        redisTemplate.delete(CHANGE_EMAIL_FLAG + newEmail);
    }
}
