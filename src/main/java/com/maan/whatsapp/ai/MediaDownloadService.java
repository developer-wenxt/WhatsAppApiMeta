package com.maan.whatsapp.ai;
//===============================
//MediaDownloadService.java
//===============================



import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.springframework.stereotype.Service;

@Service
public class MediaDownloadService {

 public File downloadMedia(String mediaUrl, String outputFileName) throws Exception {

     URL url = new URL(mediaUrl);

     File outputFile = new File(System.getProperty("java.io.tmpdir"), outputFileName);

     try (
             InputStream inputStream = url.openStream();
             FileOutputStream fos = new FileOutputStream(outputFile)
     ) {

         byte[] buffer = new byte[4096];
         int bytesRead;

         while ((bytesRead = inputStream.read(buffer)) != -1) {
             fos.write(buffer, 0, bytesRead);
         }
     }

     return outputFile;
 }
}