package com.maan.whatsapp.insurance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.maan.whatsapp.config.exception.WhatsAppValidationException;

public interface BoatswanaInsuranceService {

	Object quoteGenerationBoatswana(Object req) throws JsonProcessingException,JsonMappingException, WhatsAppValidationException;

	Object paymentLinkGenerationBoatswana(Object req) throws JsonProcessingException,JsonMappingException, WhatsAppValidationException;

}
