package com.maan.whatsapp.ai;




import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WhatsAppService {

    @Value("${whatsapp.token}")
    private String token;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    public void sendTextMessage(
            String to,
            String message) {

        WebClient client = WebClient.builder()
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token)
                .build();

        Map<String, Object> body =
                new HashMap<>();

        body.put("messaging_product",
                "whatsapp");

        body.put("to", to);

        body.put("type", "text");

        Map<String, Object> text =
                new HashMap<>();

        text.put("body", message);

        body.put("text", text);

        client.post()
                .uri("https://graph.facebook.com/v19.0/"
                        + phoneNumberId
                        + "/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}