package com.pratik.financegpt.service;

import com.pratik.financegpt.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ChatService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public ChatService(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    public String processMessage(String message){
        try {
            Map<String , Object> part = new HashMap<>();
            part.put("text" , message);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String urlWithKey = apiUrl + "?key=" + apiKey;
            ResponseEntity<Map> response = restTemplate.exchange(
                    urlWithKey,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map responseBody = response.getBody();
            List candidates = (List) responseBody.get("candidates");
            Map candidate = (Map) candidates.get(0);
            Map responseContent = (Map) candidate.get("content");
            List parts = (List) responseContent.get("parts");
            Map firstPart = (Map) parts.get(0);

            return (String) firstPart.get("text");

        } catch (Exception e) {
            return "Error calling gemini API: " + e.getMessage();
        }
    }
}
