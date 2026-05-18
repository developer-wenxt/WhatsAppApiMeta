package com.maan.whatsapp.ai;


import java.io.File;
import java.nio.file.Files;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/voice")
public class VoiceChatController {

    private final SpeechToTextService sttService;
    private final AIChatService aiChatService;
    private final TextToSpeechService ttsService;

    public VoiceChatController(
            SpeechToTextService sttService,
            AIChatService aiChatService,
            TextToSpeechService ttsService) {

        this.sttService = sttService;
        this.aiChatService = aiChatService;
        this.ttsService = ttsService;
    }

    @PostMapping(
            value = "/chat",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<byte[]> voiceChat(
            @RequestParam("file") MultipartFile file)
            throws Exception {

        File temp = File.createTempFile("audio", ".mp3");

        file.transferTo(temp);

        String text =
                sttService.transcribe(temp);

        String aiResponse =
                aiChatService.chat(text);

        byte[] audio =
                ttsService.generateSpeech(aiResponse);

        Files.deleteIfExists(temp.toPath());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(audio);
    }
}