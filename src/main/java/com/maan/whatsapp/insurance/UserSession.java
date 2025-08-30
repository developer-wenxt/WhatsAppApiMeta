package com.maan.whatsapp.insurance;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class UserSession {

	private int currentQuestionIndex = 0;
    private final Map<String, String> responses = new HashMap<>();

    public void addAnswer(String key, String answer) {
        responses.put(key, answer);
    }

    public void nextQuestion() {
        currentQuestionIndex++;
    }

    public boolean isComplete(int totalQuestions) {
        return responses.size() == totalQuestions;
    }
}
