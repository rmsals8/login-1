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
        private final LogRepository logRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider tokenProvider;

        public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

        // 기본 토큰 유효 기간 설정
        public static final Duration DEFAULT_REFRESH_TOKEN_DURATION = Duration.ofDays(7);
        public static final Duration EXTENDED_REFRESH_TOKEN_DURATION = Duration.ofDays(30); // 로그인 유지 시 30일
        public static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(30);

        @Transactional
        public LoginResponseDto login(LoginRequestDto loginRequest, HttpServletRequest request,
                        HttpServletResponse response) {
                // 로그인 실패 횟수 관리
                Integer failCount = (Integer) request.getSession().getAttribute("loginFailCount");
                if (failCount == null) {
                        failCount = 0;
                }

                // 실패 횟수가 3회 이상이면 캡차 검증
                if (failCount >= 3) {
                        String captchaInput = loginRequest.getCaptcha();
                        String sessionCaptcha = (String) request.getSession().getAttribute("captchaText");

                        if (captchaInput == null || sessionCaptcha == null || !sessionCaptcha.equals(captchaInput)) {
                                // 캡차 오류 로그 기록
                                saveLog(null, "CAPTCHA_FAIL", "캡차 검증 실패: " + loginRequest.getUsername(),
                                                loginRequest.getIpAddress(), request.getHeader("User-Agent"));

                                throw new BadCredentialsException("자동입력 방지 문자가 일치하지 않습니다.");
                        }

                        // 성공시 캡차 세션 정보 삭제
                        request.getSession().removeAttribute("captchaText");
                }

                // 1. 사용자명으로 사용자 조회
                User user = userMapper.findByUserName(loginRequest.getUsername());

                if (user == null) {
                        // 로그인 실패 횟수 증가
                        request.getSession().setAttribute("loginFailCount", failCount + 1);

                        // 사용자가 존재하지 않는 경우 로그 기록 (JPA)
                        saveLog(null, "LOGIN_FAIL", "사용자가 존재하지 않음: " + loginRequest.getUsername(),
                                        loginRequest.getIpAddress(), request.getHeader("User-Agent"));

                        throw new BadCredentialsException("Invalid username or password");
                }

                // 2. 비밀번호 검증 - 별도 테이블에서 비밀번호 정보 조회
                Password password = passwordMapper.findByUserNo(user.getUserNo());

                if (password == null) {
                        // 로그인 실패 횟수 증가
                        request.getSession().setAttribute("loginFailCount", failCount + 1);

                        // 비밀번호 정보가 없는 경우 (소셜 로그인 사용자일 수 있음)
                        saveLog(user.getUserNo(), "LOGIN_FAIL", "비밀번호 정보 없음: " + loginRequest.getUsername(),
                                        loginRequest.getIpAddress(), request.getHeader("User-Agent"));

                        throw new BadCredentialsException("Invalid login method");
                }

                if (!passwordEncoder.matches(loginRequest.getPassword(), password.getPassword())) {
                        // 로그인 실패 횟수 증가
                        request.getSession().setAttribute("loginFailCount", failCount + 1);

                        // 비밀번호 불일치 로그 기록 (JPA)
                        saveLog(user.getUserNo(), "LOGIN_FAIL", "비밀번호 불일치: " + loginRequest.getUsername(),
                                        loginRequest.getIpAddress(), request.getHeader("User-Agent"));

                        throw new BadCredentialsException("Invalid username or password");
                }

                // 로그인 성공 시 실패 횟수 초기화
                request.getSession().removeAttribute("loginFailCount");

                // 3. JWT 토큰 생성
                String accessToken = tokenProvider.generateToken(user, ACCESS_TOKEN_DURATION);

                // 4. 리프레시 토큰 생성 및 저장 (MyBatis)
                // 로그인 유지 여부에 따라 리프레시 토큰 유효 기간 설정
                Duration refreshTokenDuration = loginRequest.isRememberMe()
                                ? EXTENDED_REFRESH_TOKEN_DURATION
                                : DEFAULT_REFRESH_TOKEN_DURATION;

                String refreshToken = tokenProvider.generateToken(user, refreshTokenDuration);
                saveRefreshToken(user.getUserNo(), refreshToken);
                addRefreshTokenToCookie(request, response, refreshToken, refreshTokenDuration);

                // 5. 로그인 성공 로그 기록 (JPA)
                saveLog(user.getUserNo(), "LOGIN_SUCCESS", "로그인 성공: " + loginRequest.getUsername()
                                + (loginRequest.isRememberMe() ? " (로그인 유지)" : ""),
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
                        String refreshToken, Duration duration) {
                int cookieMaxAge = (int) duration.toSeconds();

                Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                cookie.setMaxAge(cookieMaxAge);

                // HTTPS를 사용하는 경우 Secure 속성 추가
                if (request.isSecure()) {
                        cookie.setSecure(true);
                }

                response.addCookie(cookie);
        }
}