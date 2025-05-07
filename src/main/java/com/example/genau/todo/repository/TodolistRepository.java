package com.example.genau.todo.repository;

import com.example.genau.todo.entity.Todolist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TodolistRepository extends JpaRepository<Todolist, Long> {
    List<Todolist> findAllByTeamId(Long teamId);
}

