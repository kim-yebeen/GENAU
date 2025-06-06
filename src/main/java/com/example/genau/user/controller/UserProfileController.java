package com.example.genau.user.controller;

import com.example.genau.user.dto.*;
import com.example.genau.user.security.AuthUtil;
import com.example.genau.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/users/me")  // 이미 '/me' 경로로 되어있음 ✅
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    /** 0) 사용자 정보 조회 - 인증 필요 */
    @GetMapping
    public ResponseEntity<UserProfileDto> getMyProfile() {
        Long userId = AuthUtil.getCurrentUserId();
        UserProfileDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /** 1) 이름 변경 - 인증 필요 */
    @PutMapping("/name")
    public ResponseEntity<?> updateName(@Valid @RequestBody NameRequestDto body) {
        Long userId = AuthUtil.getCurrentUserId();
        userService.updateName(userId, body.getName());
        return ResponseEntity.ok().body(Map.of("message", "이름이 변경되었습니다."));
    }

    /** 2) 프로필 이미지 변경 - 인증 필요 */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateImage(@RequestParam("file") MultipartFile file) throws Exception {
        Long userId = AuthUtil.getCurrentUserId();
        String imageUrl = userService.updateProfileImage(userId, file);
        return ResponseEntity.ok().body(Map.of("imageUrl", imageUrl));
    }

    /** 2-1) 프로필 이미지 삭제 - 인증 필요 (추가) */
    @DeleteMapping("/image")
    public ResponseEntity<?> deleteImage() throws Exception {
        Long userId = AuthUtil.getCurrentUserId();
        userService.deleteProfileImage(userId);
        return ResponseEntity.ok().body(Map.of("message", "프로필 이미지가 삭제되었습니다."));
    }

    /** 3) 새 이메일로 인증코드 전송 - 인증 필요 ⭐ */
    @PostMapping("/email/send-code")
    public ResponseEntity<?> sendChangeCode(@Valid @RequestBody EmailRequestDto body) throws Exception {
        // 인증된 사용자만 이메일 변경 인증코드 요청 가능
        Long userId = AuthUtil.getCurrentUserId();

        // 현재 사용자의 이메일과 다른 이메일로만 변경 가능하도록 검증할 수도 있음
        userService.sendEmailChangeCode(body.getEmail());
        return ResponseEntity.ok(Map.of("message", "인증코드 전송됨"));
    }

    /** 4) 새 이메일 인증코드 검증 - 인증 필요 ⭐ */
    @PostMapping("/email/verify-code")
    public ResponseEntity<?> verifyChangeCode(@Valid @RequestBody EmailVerifyDto body) {
        // 인증된 사용자만 검증 가능
        Long userId = AuthUtil.getCurrentUserId();

        boolean verified = userService.verifyEmailChangeCode(body.getEmail(), body.getCode());
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    /** 5) 인증된 새 이메일로 변경 - 인증 필요 ⭐ */
    @PutMapping("/email")
    public ResponseEntity<?> applyEmailChange(@Valid @RequestBody EmailRequestDto body) {
        Long userId = AuthUtil.getCurrentUserId();
        userService.updateEmail(userId, body.getEmail());
        return ResponseEntity.ok(Map.of("message", "이메일이 변경되었습니다."));
    }

    /** 6) 비밀번호 변경 - 인증 필요 ⭐ */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequestDto body) {
        try {
            Long userId = AuthUtil.getCurrentUserId();
            userService.changePassword(userId, body);
            return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
