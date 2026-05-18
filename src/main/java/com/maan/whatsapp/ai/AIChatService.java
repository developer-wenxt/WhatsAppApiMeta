package com.maan.whatsapp.ai;



import java.util.List;

import org.springframework.stereotype.Service;

import com.theokanning.openai.completion.chat.*;

@Service
public class AIChatService {

    private final OpenAiService openAiService;

    public AIChatService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public String chat(String userMessage) {

        ChatMessage message =
                new ChatMessage("user", userMessage);

        ChatCompletionRequest request =
                ChatCompletionRequest.builder()
                        .model("gpt-4o-mini")
                        .messages(List.of(message))
                        .maxTokens(500)
                        .build();

        ChatCompletionResult result =
                openAiService.createChatCompletion(request);

        return result.getChoices()
                .get(0)
                .getMessage()
                .getContent();
    }
}