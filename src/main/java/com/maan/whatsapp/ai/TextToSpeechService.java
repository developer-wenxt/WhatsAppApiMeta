package com.maan.whatsapp.ai;



import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class TextToSpeechService {

    @Value("${openai.api.key}")
    private String apiKey;

    public byte[] generateSpeech(String text) {

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + apiKey)
                .build();

        Map<String, Object> body =
                new HashMap<>();

        body.put("model", "tts-1");
        body.put("input", text);
        body.put("voice", "alloy");

        return webClient.post()
                .uri("/v1/audio/speech")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}