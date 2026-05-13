package com.bootcamp.flowcheck.global.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GeminiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=";

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiService(@Value("${gemini.api-key:}") String apiKey) {
        this.apiKey       = apiKey;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Gemini API에 프롬프트를 전송하고 텍스트 응답을 반환합니다.
     *
     * @param prompt 전송할 프롬프트 텍스트
     * @return Gemini 응답 텍스트
     */
    public String generateContent(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다.");
        }

        String requestBody = buildRequestBody(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GEMINI_URL + apiKey,
                    HttpMethod.POST,
                    entity,
                    String.class);

            return extractText(response.getBody());
        } catch (Exception e) {
            log.error("[Gemini] API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("Gemini API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String prompt) {
        try {
            return objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .set("contents", objectMapper.createArrayNode()
                                    .add(objectMapper.createObjectNode()
                                            .set("parts", objectMapper.createArrayNode()
                                                    .add(objectMapper.createObjectNode()
                                                            .put("text", prompt))))));
        } catch (Exception e) {
            throw new RuntimeException("Gemini 요청 직렬화 실패", e);
        }
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");
        } catch (Exception e) {
            log.error("[Gemini] 응답 파싱 실패: {}", responseBody);
            throw new RuntimeException("Gemini 응답 파싱 실패", e);
        }
    }
}
