package com.example.login.service;

import com.example.login.config.KakaoOAuthConfig;
import com.example.login.dto.KakaoTokenResponse;
import com.example.login.dto.KakaoUserInfoResponse;
import com.example.login.dto.LoginResponseDto;
import com.example.login.entity.Log;
import com.example.login.entity.RefreshToken;
import com.example.login.entity.SocialLogin;
import com.example.login.entity.User;
import com.example.login.mapper.RefreshTokenMapper;
import com.example.login.mapper.SocialLoginMapper;
import com.example.login.mapper.UserMapper;
import com.example.login.repository.LogRepository;
import com.example.login.util.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private final KakaoOAuthConfig kakaoOAuthConfig;
    private final UserMapper userMapper;
    private final SocialLoginMapper socialLoginMapper;
    private final LogRepository logRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenMapper refreshTokenMapper;
    private final RestTemplate restTemplate;

    private static final int KAKAO_SOCIAL_CODE = 4; // 카카오 소셜 로그인 코드
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);
    private static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(30);

    /**
     * 카카오 인증 URL 반환
     */
    public String getKakaoAuthUrl() {
        return kakaoOAuthConfig.getAuthorizationUrl();
    }

    /**
     * 카카오 로그인 처리
     */
    @Transactional
    public LoginResponseDto kakaoLogin(String code, HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1. 카카오로부터 액세스 토큰 획득
            KakaoTokenResponse tokenResponse = getKakaoAccessToken(code);
            if (tokenResponse == null || tokenResponse.getAccess_token() == null) {
                throw new RuntimeException("Failed to get access token from Kakao");
            }

            // 2. 액세스 토큰으로 카카오 API 호출하여 사용자 정보 획득
            KakaoUserInfoResponse userInfo = getKakaoUserInfo(tokenResponse.getAccess_token());
            if (userInfo == null || userInfo.getId() == null) {
                throw new RuntimeException("Failed to get user info from Kakao");
            }

            // 3. 사용자 정보를 기반으로 회원가입 또는 로그인 처리
            User user = processKakaoUser(userInfo, tokenResponse.getAccess_token());

            // 4. JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateToken(user, ACCESS_TOKEN_DURATION);

            // 5. 리프레시 토큰 생성 및 저장
            String refreshToken = jwtTokenProvider.generateToken(user, REFRESH_TOKEN_DURATION);
            saveRefreshToken(user.getUserNo(), refreshToken, response);

            // 6. 로그인 성공 로그 기록
            saveLog(user.getUserNo(), "KAKAO_LOGIN_SUCCESS",
                    "카카오 로그인 성공: " + userInfo.getId(),
                    getClientIp(request), request.getHeader("User-Agent"));

            return LoginResponseDto.builder()
                    .userId(user.getUserNo())
                    .username(user.getUsername())
                    .token(accessToken)
                    .build();
        } catch (Exception e) {
            // 로그인 실패 로그 기록
            saveLog(null, "KAKAO_LOGIN_FAIL",
                    "카카오 로그인 실패: " + e.getMessage(),
                    getClientIp(request), request.getHeader("User-Agent"));
            throw new RuntimeException("Kakao login failed: " + e.getMessage(), e);
        }
    }

    /**
     * 카카오 액세스 토큰 획득
     */
    private KakaoTokenResponse getKakaoAccessToken(String code) {
        // 토큰 요청을 위한 파라미터 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoOAuthConfig.getClientId());
        params.add("redirect_uri", kakaoOAuthConfig.getRedirectUrl());
        params.add("code", code);

        // 카카오에 토큰 요청
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                kakaoOAuthConfig.getTokenUrl(),
                request,
                KakaoTokenResponse.class);

        return response.getBody();
    }

    /**
     * 카카오 사용자 정보 요청
     */
    private KakaoUserInfoResponse getKakaoUserInfo(String accessToken) {
        // 사용자 정보 요청을 위한 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 카카오에 사용자 정보 요청
        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<KakaoUserInfoResponse> response = restTemplate.exchange(
                kakaoOAuthConfig.getUserInfoUrl(),
                HttpMethod.POST,
                request,
                KakaoUserInfoResponse.class);

        return response.getBody();
    }

    /**
     * 카카오 사용자 처리 (회원가입 또는 로그인)
     */
    private User processKakaoUser(KakaoUserInfoResponse userInfo, String accessToken) {
        // 카카오 ID로 소셜 로그인 정보 조회
        String kakaoId = String.valueOf(userInfo.getId());
        SocialLogin socialLogin = socialLoginMapper.findByExternalId(kakaoId, KAKAO_SOCIAL_CODE);

        // 이미 가입된 사용자인 경우
        if (socialLogin != null) {
            // 액세스 토큰 업데이트
            socialLogin.setAccessToken(accessToken);
            socialLogin.setUpdateDate(LocalDateTime.now());
            socialLoginMapper.update(socialLogin);

            // 사용자 정보 조회
            User user = userMapper.findByUserNo(socialLogin.getUserNo());
            if (user == null) {
                throw new RuntimeException("User not found for social login: " + kakaoId);
            }

            return user;
        }

        // 새로운 사용자 등록
        // 이메일이 있다면 해당 이메일로 가입된 회원이 있는지 확인
        String email = userInfo.getEmail();
        User existingUser = null;

        if (email != null && !email.isEmpty()) {
            try {
                existingUser = userMapper.findByEmail(email);
            } catch (Exception e) {
                // 이메일로 검색 실패해도 계속 진행
            }
        }

        // 같은 이메일로 가입된 사용자가 있는 경우, 소셜 로그인 정보만 추가
        if (existingUser != null) {
            SocialLogin newSocialLogin = SocialLogin.builder()
                    .userNo(existingUser.getUserNo())
                    .socialCode(KAKAO_SOCIAL_CODE)
                    .externalId(kakaoId)
                    .accessToken(accessToken)
                    .updateDate(LocalDateTime.now())
                    .build();

            socialLoginMapper.save(newSocialLogin);

            return existingUser;
        }

        // 신규 사용자 생성
        String nickname = userInfo.getNickname();
        if (nickname == null || nickname.isEmpty()) {
            nickname = "kakao_user_" + UUID.randomUUID().toString().substring(0, 8);
        }

        User newUser = User.builder()
                .userName(nickname)
                .loginType(1) // 소셜 로그인
                .email(email)
                .build();

        userMapper.save(newUser);
        Long userNo = userMapper.getLastInsertId();
        newUser.setUserNo(userNo);

        // 소셜 로그인 정보 저장
        SocialLogin newSocialLogin = SocialLogin.builder()
                .userNo(userNo)
                .socialCode(KAKAO_SOCIAL_CODE)
                .externalId(kakaoId)
                .accessToken(accessToken)
                .updateDate(LocalDateTime.now())
                .build();

        socialLoginMapper.save(newSocialLogin);

        return newUser;
    }

    /**
     * 리프레시 토큰 저장
     */
    private void saveRefreshToken(Long userNo, String refreshToken, HttpServletResponse response) {
        // DB에 리프레시 토큰 저장
        RefreshToken existingToken = refreshTokenMapper.findByUserNo(userNo);

        if (existingToken != null) {
            existingToken.setRefreshToken(refreshToken);
            refreshTokenMapper.update(existingToken);
        } else {
            RefreshToken newToken = RefreshToken.builder()
                    .userNo(userNo)
                    .refreshToken(refreshToken)
                    .build();
            refreshTokenMapper.save(newToken);
        }

        // 쿠키에 리프레시 토큰 저장
        int cookieMaxAge = (int) REFRESH_TOKEN_DURATION.toSeconds();

        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(cookieMaxAge);

        response.addCookie(cookie);
    }

    /**
     * 로그 저장
     */
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

    /**
     * 클라이언트 IP 가져오기
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}