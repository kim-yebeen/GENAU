package com.example.genau.notice.service;

import com.example.genau.notice.dto.NotificationDto;
import com.example.genau.team.domain.Team;       // ← 추가
import com.example.genau.user.domain.User;       // ← 추가
import com.example.genau.notice.domain.Notice;
import com.example.genau.notice.repository.NoticeRepository;
import com.example.genau.team.domain.Teammates;
import com.example.genau.team.repository.TeamRepository;
import com.example.genau.team.repository.TeammatesRepository;
import com.example.genau.todo.entity.Todolist;
import com.example.genau.todo.repository.TodolistRepository;
import com.example.genau.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NoticeRepository         noticeRepository;
    private final TeammatesRepository      teammatesRepository;
    private final TeamRepository           teamRepository;
    private final TodolistRepository       todolistRepository;
    private final UserRepository           userRepository;

    /** 0) 팀 참여 즉시 알림 생성 */
    public void createTeamJoinNotification(Long newTeammatesId) {
        // 1) 새로 참여한 사람 정보
        Teammates newTm = teammatesRepository.findById(newTeammatesId)
                .orElseThrow();
        Long teamId = newTm.getTeamId();
        String newUserName = userRepository.findById(newTm.getUserId())
                .map(u -> u.getUserName())
                .orElse("알 수 없는 사용자");

        // 2) 같은 팀 기존 팀원들 (새로 들어온 사람은 제외)
        List<Teammates> existing = teammatesRepository.findAllByTeamId(teamId).stream()
                .filter(tm -> !tm.getTeammatesId().equals(newTeammatesId))
                .toList();

        // 3) 팀 이름
        String teamName = teamRepository.findById(teamId)
                .map(Team::getTeamName)
                .orElse("알 수 없는 팀");

        // 4) 알림 생성
        for (Teammates tm : existing) {
            Notice notice = Notice.builder()
                    .teammatesId(tm.getTeammatesId())
                    .noticeType("TEAM_JOIN")
                    .referenceId(teamId)                // 참조용: 팀 ID
                    .noticeMessage(
                            String.format("%s 팀에 %s 님이 참여했습니다", teamName, newUserName)
                    )
                    .build();
            noticeRepository.save(notice);
        }
    }

    // 매일 오전 9시에 실행.'내일' 마감인 할 일을 찾아, 담당자에게 알림 생성
    @Scheduled(cron = "0 5 2 * * *", zone = "Asia/Seoul")
    public void scheduleDueTomorrowNotifications() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Todolist> list = todolistRepository.findAllByDueDate(tomorrow);

        for (Todolist t : list) {
            // ① 이미 완료된
            // TODO는 건너뛰기
            if (Boolean.TRUE.equals(t.getTodoChecked())) {
                continue;
            }

            // ② 할당자(teammates) 조회
            Teammates tm = teammatesRepository
                    .findByTeamIdAndUserId(t.getTeamId(), t.getAssigneeId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Teammates not found: team=" + t.getTeamId()
                                    + ", user=" + t.getAssigneeId()));

            // ③ 알림 생성
            Notice notice = Notice.builder()
                    .teammatesId(tm.getTeammatesId())
                    .noticeType("TODO_DUE_SOON")
                    .referenceId(t.getTodoId())
                    .noticeMessage("할 일 \"" + t.getTodoTitle() + "\" 마감일이 내일입니다.")
                    .build();

            noticeRepository.save(notice);
        }
    }

    // 2) 할 일이 완료된 경우 → 팀원 전체에게 알림
    public void createTodoCompletedNotification(Long todoId) {
        Todolist todo = todolistRepository.findById(todoId)
                .orElseThrow(() -> new EntityNotFoundException("Todo not found: " + todoId));

        Long teamId = todo.getTeamId();
        List<Teammates> members = teammatesRepository.findAllByTeamId(teamId);

        for (Teammates tm : members) {
            Notice notice = Notice.builder()
                    .teammatesId(tm.getTeammatesId())
                    .noticeType("TODO_COMPLETED")
                    .referenceId(todoId)
                    .noticeMessage("할 일 \"" + todo.getTodoTitle() + "\"가 완료되었습니다.")
                    .build();
            noticeRepository.save(notice);
        }
    }

    //3) 알림 삭제

    public void deleteNotification(Long noticeId, Long userId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("Notice not found: " + noticeId));

        validateNotificationOwnership(notice,userId);

        noticeRepository.deleteById(noticeId);
    }

    //4) 알림 읽음 표시
    public void markAsRead(Long noticeId, Long userId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("Notice not found: " + noticeId));
        notice.setIsRead(true);

        validateNotificationOwnership(notice, userId);
        notice.setIsRead(true);
        noticeRepository.save(notice);
    }


    /** (1) 내 모든 알림 조회 */
    public List<NotificationDto> getNotificationsByUser (Long userId){
        // 1) userId 로 내 teammatesId 목록을 뽑아야 합니다
        List<Long> tmIds = teammatesRepository.findAllByUserId(userId)
                .stream()
                .map(Teammates::getTeammatesId)
                .toList();

        if (tmIds.isEmpty()) {
            return List.of();
        }

        // 2) 그 ID 들로 notice 조회 (OrderBy 생성일자 내림차순)
        List<Notice> notices = noticeRepository
                .findAllByTeammatesIdInOrderByNoticeCreatedDesc(tmIds);

        // 3) DTO 변환
        return notices.stream()
                .map(n -> new NotificationDto(
                        n.getNoticeId(),
                        n.getTeammatesId(),
                        n.getNoticeType(),
                        n.getReferenceId(),
                        n.getNoticeMessage(),
                        n.getNoticeCreated(),
                        n.getIsRead()
                ))
                .toList();
    }

    private void validateNotificationOwnership(Notice notice, Long userId) {
        // 해당 알림의 teammatesId가 현재 사용자의 것인지 확인
        Teammates teammates = teammatesRepository.findById(notice.getTeammatesId())
                .orElseThrow(() -> new EntityNotFoundException("Teammates not found: " + notice.getTeammatesId()));

        if (!teammates.getUserId().equals(userId)) {
            throw new AccessDeniedException("본인의 알림만 관리할 수 있습니다.");
        }
    }

    public long getUnreadCountByUser(Long userId) {
        List<Long> tmIds = teammatesRepository.findAllByUserId(userId)
                .stream()
                .map(Teammates::getTeammatesId)
                .toList();

        if (tmIds.isEmpty()) return 0L;

        return noticeRepository.countByTeammatesIdInAndIsReadFalse(tmIds);
    }

}