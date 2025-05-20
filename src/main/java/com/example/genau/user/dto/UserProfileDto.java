package com.example.genau.user.dto;

import com.example.genau.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class UserProfileDto {
    private Long userId;
    private String userName;
    private String email;
    private String profileImg;

    // User 엔티티로부터 DTO 생성하는 정적 메소드
    public static UserProfileDto fromEntity(User user) {
        return UserProfileDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getMail())
                .profileImg(user.getProfileImg())
                .build();
    }
}