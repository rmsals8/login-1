package com.example.login.repository;

import com.example.login.entity.RefreshToken;
import com.example.login.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
    private final RefreshTokenMapper refreshTokenMapper;

    public Optional<RefreshToken> findByUserId(Long userId) {
        RefreshToken refreshToken = refreshTokenMapper.findByUserNo(userId);
        return Optional.ofNullable(refreshToken);
    }

    public Optional<RefreshToken> findByRefreshToken(String refreshToken) {
        RefreshToken token = refreshTokenMapper.findByRefreshToken(refreshToken);
        return Optional.ofNullable(token);
    }

    public void save(RefreshToken refreshToken) {
        RefreshToken existingToken = refreshTokenMapper.findByUserNo(refreshToken.getUserNo());
        if (existingToken != null) {
            refreshTokenMapper.update(refreshToken);
        } else {
            refreshTokenMapper.save(refreshToken);
        }
    }

    public void deleteByUserId(Long userId) {
        refreshTokenMapper.deleteByUserNo(userId);
    }
}