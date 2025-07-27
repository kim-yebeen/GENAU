package com.example.genau.todo.repository;

import com.example.genau.todo.entity.Todolist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TodolistRepository extends JpaRepository<Todolist, Long> {

    @Query("SELECT t FROM Todolist t JOIN t.assignees a " +
            "WHERE t.teamId = :teamId AND a.userId = :assigneeId " +
            "AND t.dueDate BETWEEN :start AND :end")
    List<Todolist> findByTeamIdAndAssigneeIdAndDueDateBetween(
            @Param("teamId") Long teamId,
            @Param("assigneeId") Long assigneeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<Todolist> findAllByTeamId(Long teamId);

    // 카테고리별 할 일
    List<Todolist> findAllByTeamIdAndCatId(Long teamId, Long catId);

    // 이번 주(일요일~토요일) 할 일
    List<Todolist> findAllByTeamIdAndDueDateBetween(Long teamId, LocalDate start, LocalDate end);

    // 변환 완료된 투두 목록 조회
    List<Todolist> findAllByConvertStatus(String convertStatus);

    // 특정 팀의 변환 완료된 투두 조회
    List<Todolist> findAllByTeamIdAndConvertStatus(Long teamId, String convertStatus);


    /**
     * 특정 팀(teamId)에서 특정 팀원(teammatesId)에게 할당된
     * dueDate 범위(start ~ end) 내의 할 일 조회
     */
    /*List<Todolist> findAllByTeamIdAndAssigneeIdAndDueDateBetween(
            Long teamId,
            Long assigneeId,
            LocalDate start,
            LocalDate end
    );*/

    @Query("SELECT t FROM Todolist t JOIN t.assignees a " +
            "WHERE t.teamId = :teamId AND a.userId = :assigneeId " +
            "AND t.dueDate BETWEEN :start AND :end")
    List<Todolist> findAllByTeamIdAndAssigneeIdAndDueDateBetween(
            @Param("teamId") Long teamId,
            @Param("assigneeId") Long assigneeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );


    /** 내일 마감인 할 일 조회 */
    List<Todolist> findAllByDueDate(LocalDate dueDate);

    //List<Todolist> findAllByAssigneeId(Long assigneeId);

    @Query("SELECT t FROM Todolist t JOIN t.assignees a " +
            "WHERE a.userId = :assigneeId")
    List<Todolist> findAllByAssigneeId(@Param("assigneeId") Long assigneeId);


}

