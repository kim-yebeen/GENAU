package com.example.genau.user.repository;

import com.example.genau.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByMail(String mail);
    Optional<User> findByMail(String mail);

}