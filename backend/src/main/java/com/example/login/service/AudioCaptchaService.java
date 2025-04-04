package com.example.login.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Base64;

@Service
public class AudioCaptchaService {

    @Value("${google.tts.api-key:AIzaSyDx9k7SvmQ9_7YozBAri0ehgeoblZ3A3oI}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public AudioCaptchaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] generateAudio(String captchaText) throws IOException {
        try {
            System.out.println("생성 중인 캡차 텍스트: " + captchaText);

            // 캡차 텍스트 포맷팅 (글자 사이에 공백 추가)
            StringBuilder enhancedText = new StringBuilder();
            for (char c : captchaText.toCharArray()) {
                enhancedText.append(c).append(" ");
            }
            String textToSpeech = enhancedText.toString().trim();

            // Google TTS API 요청 URL
            String url = "https://texttospeech.googleapis.com/v1/text:synthesize?key=" + apiKey;

            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 요청 본문 구성
            String requestBody = String.format(
                    "{\"input\":{\"text\":\"%s\"},\"voice\":{\"languageCode\":\"ko-KR\",\"ssmlGender\":\"FEMALE\"},\"audioConfig\":{\"audioEncoding\":\"MP3\",\"speakingRate\":0.8}}",
                    textToSpeech);

            // HTTP 요청 엔티티 생성
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            // API 호출
            ResponseEntity<GoogleTtsResponse> response = restTemplate.postForEntity(
                    url,
                    request,
                    GoogleTtsResponse.class);

            // 응답 확인
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Base64 인코딩된 오디오 데이터를 디코딩
                byte[] audioData = Base64.getDecoder().decode(response.getBody().getAudioContent());
                System.out.println("생성된 오디오 데이터 길이: " + audioData.length + " bytes");
                return audioData;
            } else {
                System.err.println("Google TTS API 호출 실패: " + response.getStatusCode());
                throw new IOException("오디오 생성 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("오디오 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("오디오 생성 중 오류: " + e.getMessage(), e);
        }
    }

    // Google TTS API 응답을 위한 내부 클래스
    private static class GoogleTtsResponse {
        private String audioContent;

        public String getAudioContent() {
            return audioContent;
        }

    }
}