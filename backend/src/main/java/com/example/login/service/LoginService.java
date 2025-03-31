package com.example.login.service;

import com.example.login.dto.LoginRequestDto;
import com.example.login.dto.LoginResponseDto;
import com.example.login.entity.Log;
import com.example.login.entity.Password;
import com.example.login.entity.RefreshToken;
import com.example.login.entity.User;
import com.example.login.mapper.PasswordMapper;
import com.example.login.mapper.RefreshTokenMapper;
import com.example.login.mapper.UserMapper;
import com.example.login.repository.LogRepository;
import com.example.login.util.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginService {
        private final UserMapper userMapper;
        private final PasswordMapper passwordMapper;
        private final RefreshTokenMapper refreshTokenMapper;
        private final LogRepository logRepository; // JPA 리포지토리
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider tokenProvider;

        public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
        public static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(14);
        public static final Duration ACCESS_TOKEN_DURATION = Duration.ofDays(1);

        @Transactional
        public LoginResponseDto login(LoginRequestDto loginRequest, HttpServletRequest request,
                        HttpServletResponse response) {
                // 1. 사용자명으로 사용자 조회
                User user = userMapper.findByUserName(loginRequest.getUsername());

                if (user == null) {
                        // 사용자가 존재하지 않는 경우 로그 기록 (JPA)
                        saveLog(null, "LOGIN_FAIL", "사용자가 존재하지 않음: " + loginRequest.getUsername(),
                                        loginRequest.getIpAddress(), request.getHeader("User-Agent"));

                        throw new BadCredentialsException("Invalid username or password");
                }

                // 2. 비밀번호 검증 - 별도 테이블에서 비밀번호 정보 조회
                Password password = passwordMapper.findByUserNo(user.getUserNo());

                if (password == null) {
                        // 비밀번호 정보가 없는 경우 (소셜 로그인 사용자일 수 있음)
                        saveLog(user.getUserNo(), "LOGIN_FAIL", "비밀번호 정보 없음: " + loginRequest.getUsername(),
                                        loginRequest.getIpAddress(), request.getHeader("User-Agent"));

                        throw new BadCredentialsException("Invalid login method");
                }

                if (!passwordEncoder.matches(loginRequest.getPassword(), password.getPassword())) {
                        // 비밀번호 불일치 로그 기록 (JPA)
                        saveLog(user.getUserNo(), "LOGIN_FAIL", "비밀번호 불일치: " + loginRequest.getUsername(),
                                        loginRequest.getIpAddress(), request.getHeader("User-Agent"));

                        throw new BadCredentialsException("Invalid username or password");
                }

                // 3. JWT 토큰 생성
                String accessToken = tokenProvider.generateToken(user, ACCESS_TOKEN_DURATION);

                // 4. 리프레시 토큰 생성 및 저장 (MyBatis)
                String refreshToken = tokenProvider.generateToken(user, REFRESH_TOKEN_DURATION);
                saveRefreshToken(user.getUserNo(), refreshToken);
                addRefreshTokenToCookie(request, response, refreshToken);

                // 5. 로그인 성공 로그 기록 (JPA)
                saveLog(user.getUserNo(), "LOGIN_SUCCESS", "로그인 성공: " + loginRequest.getUsername(),
                                loginRequest.getIpAddress(), request.getHeader("User-Agent"));

                // 6. 응답 생성 및 반환
                return LoginResponseDto.builder()
                                .userId(user.getUserNo())
                                .username(user.getUsername())
                                .token(accessToken)
                                .build();
        }

        private void saveLog(Long userNo, String actionType, String description, String ipAddress, String userAgent) {
                Log log = Log.builder()
                                .userNo(userNo)
                                .actionType(actionType)
                                .description(description)
                                .ipAddress(ipAddress)
                                .userAgent(userAgent != null ? userAgent : "Unknown")
                                .status("COMPLETED")
                                .createdAt(LocalDateTime.now())
                                .build();

                logRepository.save(log);
        }

        private void saveRefreshToken(Long userNo, String newRefreshToken) {
                RefreshToken refreshToken = refreshTokenMapper.findByUserNo(userNo);

                if (refreshToken != null) {
                        refreshToken.setRefreshToken(newRefreshToken);
                        refreshTokenMapper.update(refreshToken);
                } else {
                        refreshToken = RefreshToken.builder()
                                        .userNo(userNo)
                                        .refreshToken(newRefreshToken)
                                        .build();
                        refreshTokenMapper.save(refreshToken);
                }
        }

        private void addRefreshTokenToCookie(HttpServletRequest request, HttpServletResponse response,
                        String refreshToken) {
                int cookieMaxAge = (int) REFRESH_TOKEN_DURATION.toSeconds();

                Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                cookie.setMaxAge(cookieMaxAge);

                response.addCookie(cookie);
        }
}