package com.maan.whatsapp.insurance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.maan.whatsapp.config.exception.WhatsAppValidationException;

public interface MozambiqueInsuranceService {

	Object generateMozambiqueMotorQuote(Object req) throws JsonProcessingException,JsonMappingException, WhatsAppValidationException;

	Object paymentLinkGenerationMozambique(Object req) throws JsonProcessingException,JsonMappingException, WhatsAppValidationException;

}
