package com.example.genau.notice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter @Setter
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;

    // 알림을 받을 teammates_id
    private Long teammatesId;

    // TEAM_JOIN, DUE_TOMORROW, TODO_COMPLETE 등
    private String noticeType;

    // 관련 엔티티의 ID (teamId 또는 todoId 등)
    private Long referenceId;

    @Column(columnDefinition = "TEXT")
    private String noticeMessage;

    @Builder.Default
    private LocalDateTime noticeCreated = LocalDateTime.now();

    @Builder.Default
    private Boolean isRead = false;
}
