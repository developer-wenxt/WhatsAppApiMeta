package com.maan.whatsapp.ai;
import java.awt.PageAttributes.MediaType;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Service
public class OpenAiService {

    @Value("${openai.api.key}")//    @Value("${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}")

    private String apiKey;

    public String askAi(String question) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();

        body.put("model", "gpt-4o-mini");

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", question);

        messages.add(msg);

        body.put("messages", messages);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        "https://api.openai.com/v1/chat/completions",
                        entity,
                        Map.class);

        List choices = (List) response.getBody().get("choices");

        Map choice = (Map) choices.get(0);

        Map message = (Map) choice.get("message");

        return message.get("content").toString();
    }
}
