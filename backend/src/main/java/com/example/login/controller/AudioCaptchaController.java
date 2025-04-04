package com.example.login.controller;

import com.example.login.service.AudioCaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("/api/captcha1-audio")
public class AudioCaptchaController {

    @Autowired
    private AudioCaptchaService audioCaptchaService;

    @GetMapping("/test-audio")
    public ResponseEntity<String> testAudioService() {
        try {
            String testText = "123";
            byte[] audioData = audioCaptchaService.generateAudio(testText);
            return ResponseEntity.ok("오디오 생성 성공! 길이: " + audioData.length + " bytes");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("오류 발생: " + e.getMessage());
        }
    }

    @GetMapping("/audio")
    public ResponseEntity<byte[]> getCaptchaAudio(HttpServletRequest request) {
        try {
            // 세션에서 캡차 텍스트 가져오기
            String captchaText = (String) request.getSession().getAttribute("captchaText");

            if (captchaText == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("캡차가 생성되지 않았습니다. 이미지 캡차를 먼저 요청해주세요.".getBytes());
            }

            // 텍스트를 음성으로 변환
            byte[] audioData = audioCaptchaService.generateAudio(captchaText);
            System.out.println("Generated audio data length: " + audioData.length);
            // HTTP 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioData.length);
            headers.set("Content-Disposition", "inline; filename=captcha.mp3");

            return new ResponseEntity<>(audioData, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("음성 캡차 생성 중 오류가 발생했습니다: " + e.getMessage()).getBytes());
        }
    }
}