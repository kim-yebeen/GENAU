package com.example.genau.todo.repository;

import com.example.genau.todo.entity.Todolist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TodolistRepository extends JpaRepository<Todolist, Long> {
    List<Todolist> findAllByTeamId(Long teamId);

    // 카테고리별 할 일
    List<Todolist> findAllByTeamIdAndCatId(Long teamId, Long catId);

    // 이번 주(일요일~토요일) 할 일
    List<Todolist> findAllByTeamIdAndDueDateBetween(Long teamId, LocalDate start, LocalDate end);


}

