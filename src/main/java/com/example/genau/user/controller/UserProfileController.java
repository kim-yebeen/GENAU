package com.example.genau.user.controller;

import com.example.genau.user.dto.EmailRequestDto;
import com.example.genau.user.dto.EmailVerifyDto;
import com.example.genau.user.dto.NameRequestDto;
import com.example.genau.user.dto.UserProfileDto;
import com.example.genau.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    /** 0) 사용자 정보 조회 */
    @GetMapping
    public ResponseEntity<UserProfileDto> getMyProfile(
            @RequestParam Long userId
    ) {
        UserProfileDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /** 1) 이름 변경 */
    @PutMapping("/name")
    public ResponseEntity<?> updateName(
            @RequestParam Long userId,
            @Valid @RequestBody NameRequestDto body
    ) {
        userService.updateName(userId, body.getName());
        return ok().body("{\"message\":\"이름이 변경되었습니다.\"}");
    }

    /** 2) 프로필 이미지 변경 */
    @PostMapping( value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateImage(
            @RequestParam Long userId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        String imageUrl = userService.updateProfileImage(userId, file);
        return ok().body("{\"imageUrl\":\"" + imageUrl + "\"}");
    }

    /** 1) 새 이메일로 인증코드 전송 */
    @PostMapping("/change/send-code")
    public ResponseEntity<?> sendChangeCode(
            @Valid @RequestBody EmailRequestDto body
    ) throws Exception {
        userService.sendEmailChangeCode(body.getEmail());
        return ok(Map.of("message","인증코드 전송됨"));
    }

    /** 2) 새 이메일 인증코드 검증 */
    @PostMapping("/change/verify-code")
    public ResponseEntity<?> verifyChangeCode(
            @Valid @RequestBody EmailVerifyDto body
    ) {
        boolean ok = userService.verifyEmailChangeCode(body.getEmail(), body.getCode());
        return ok(Map.of("verified",ok));
    }

    /** 3) 인증된 새 이메일로 변경 */
    @PutMapping
    public ResponseEntity<?> applyEmailChange(
            @RequestParam Long userId,
            @Valid @RequestBody EmailRequestDto body
    ) {
        userService.updateEmail(userId, body.getEmail());
        return ok(Map.of("message","이메일이 변경되었습니다."));
    }
}
