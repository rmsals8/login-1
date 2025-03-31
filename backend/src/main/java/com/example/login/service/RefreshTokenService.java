package com.example.login.service;

import com.example.login.entity.RefreshToken;
import com.example.login.mapper.RefreshTokenMapper;
import com.example.login.util.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtTokenProvider tokenProvider;

    public RefreshToken findByRefreshToken(String refreshToken) {
        RefreshToken token = refreshTokenMapper.findByRefreshToken(refreshToken);
        if (token == null) {
            throw new IllegalArgumentException("Unexpected token");
        }
        return token;
    }

    @Transactional
    public void delete() {
        String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        Long userId = tokenProvider.getUserId(token);

        refreshTokenMapper.deleteByUserNo(userId);
    }
}