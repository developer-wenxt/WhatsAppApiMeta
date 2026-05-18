package com.maan.whatsapp.ai;

import java.io.File;

//===============================
//AudioConversionService.java
//===============================



import org.springframework.stereotype.Service;

@Service
public class AudioConversionService {

 public File convertOggToWav(File inputFile) throws Exception {

     String outputPath = inputFile.getAbsolutePath()
             .replace(".ogg", ".wav");

     ProcessBuilder processBuilder = new ProcessBuilder(
             "ffmpeg",
             "-i",
             inputFile.getAbsolutePath(),
             outputPath
     );

     processBuilder.redirectErrorStream(true);

     Process process = processBuilder.start();

     int exitCode = process.waitFor();

     if (exitCode != 0) {
         throw new RuntimeException("FFmpeg conversion failed");
     }

     return new File(outputPath);
 }
}
