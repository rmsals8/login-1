package com.example.login.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TestController.java 추가
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public TestController(PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/create-user")
    public String createTestUser() {
        String username = "testuser";
        String password = "test123";
        String encodedPassword = passwordEncoder.encode(password);

        jdbcTemplate.update(
                "INSERT INTO users (username, password) VALUES (?, ?)",
                username, encodedPassword);

        return "Test user created: " + username + " / " + password +
                "\nEncoded password: " + encodedPassword;
    }
}