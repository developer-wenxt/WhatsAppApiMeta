package com.maan.whatsapp.insurance;

import java.util.Map;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.maan.whatsapp.config.exception.WhatsAppValidationException;

public interface SwazilandInsuranceService {

	Object swazilandQuote(Object req)throws JsonProcessingException,JsonMappingException, WhatsAppValidationException;

	Object swazilandQuoteGenerate(Object req) throws JsonProcessingException,JsonMappingException, WhatsAppValidationException;

	public String swazilandFlowRequest(Map<String, Object> req);

	Object googleFlowTest(Object req) throws JsonProcessingException, JsonMappingException, WhatsAppValidationException;

}
