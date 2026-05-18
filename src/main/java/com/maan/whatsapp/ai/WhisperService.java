package com.maan.whatsapp.ai;

import java.io.File;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

//===============================
//WhisperService.java
//===============================


import okhttp3.MediaType;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class WhisperService {

 @Value("${openai.api.key}")
 private String openAiApiKey;

 private final OkHttpClient client = new OkHttpClient();

 public String transcribe(File audioFile) throws Exception {

     RequestBody fileBody =
             RequestBody.create(audioFile, MediaType.parse("audio/wav"));

     MultipartBody requestBody =
             new MultipartBody.Builder()
                     .setType(MultipartBody.FORM)
                     .addFormDataPart("file",
                             audioFile.getName(),
                             fileBody)
                     .addFormDataPart("model", "whisper-1")
                     .build();

     Request request = new Request.Builder()
             .url("https://api.openai.com/v1/audio/transcriptions")
             .header("Authorization", "Bearer " + openAiApiKey)
             .post(requestBody)
             .build();

     Response response = client.newCall(request).execute();

     if (!response.isSuccessful()) {
         throw new RuntimeException("Whisper API failed");
     }

     String responseBody = response.body().string();

     JSONObject jsonObject = new JSONObject(responseBody);

     return jsonObject.getString("text");
 }
}
