package com.example.login.service;

import com.example.login.entity.RefreshToken;
import com.example.login.entity.User;
import com.example.login.mapper.RefreshTokenMapper;
import com.example.login.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class TokenService {
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserService userService;

    public String createNewAccessToken(String refreshToken) {
        if (!tokenProvider.validToken(refreshToken)) {
            throw new IllegalArgumentException("Unexpected token");
        }

        RefreshToken token = refreshTokenMapper.findByRefreshToken(refreshToken);
        if (token == null) {
            throw new IllegalArgumentException("Unexpected token");
        }

        User user = userService.findById(token.getUserNo());

        return tokenProvider.generateToken(user, Duration.ofHours(2));
    }
}