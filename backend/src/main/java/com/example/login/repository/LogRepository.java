package com.example.login.repository;

import com.example.login.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {
    List<Log> findByUserNo(Long userNo);

    List<Log> findByActionType(String actionType);

    List<Log> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}