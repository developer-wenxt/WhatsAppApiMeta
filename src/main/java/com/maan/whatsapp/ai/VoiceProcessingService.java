package com.maan.whatsapp.ai;

import java.io.File;

import org.springframework.stereotype.Service;

import com.querydsl.core.types.Path;

import WhisperService.WhisperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceProcessingService {

    private final MediaDownloadService mediaDownloadService;
    private final AudioConversionService audioConversionService;
    private final WhisperService whisperService;

    public String processVoice(String mediaUrl, String fileName) {

        Path oggPath = mediaDownloadService.downloadAudio(mediaUrl, fileName);

        File wavFile = audioConversionService.convertOggToWav(
                oggPath.toFile()
        );

        return whisperService.transcribe(wavFile);
    }
}