package com.maan.whatsapp.document.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

public interface RegistrationDocumentRecognitionService {

	JsonNode getDocumentResult(MultipartFile file, Object req) throws IOException;

	JsonNode getResult(Object req);
}
