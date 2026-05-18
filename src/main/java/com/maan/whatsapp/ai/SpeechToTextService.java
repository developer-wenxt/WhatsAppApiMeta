package com.maan.whatsapp.ai;



import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SpeechToTextService {

    @Value("${openai.api.key}")
    private String apiKey;

    public String transcribe(File audioFile) {

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + apiKey)
                .build();

        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add("file",
                new FileSystemResource(audioFile));

        body.add("model", "whisper-1");

        return webClient.post()
                .uri("/v1/audio/transcriptions")
                .contentType(
                        MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(WhisperResponse.class)
                .block()
                .getText();
    }