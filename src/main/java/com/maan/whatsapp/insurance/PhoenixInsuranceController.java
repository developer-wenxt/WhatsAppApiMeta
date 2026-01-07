package com.maan.whatsapp.insurance;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.maan.whatsapp.config.exception.WhatsAppValidationException;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/insurance")
public class PhoenixInsuranceController {
	
	@Autowired
	private NamibiaInsuranceService serviceNamibia;
	
	@Autowired
	private ZambiaInsuranceService serviceZambia;
	
	@Autowired
	private PhoenixInsuranceService insService;
	
	@Autowired
	private SwazilandInsuranceService serviceSwaziland;
	
	@Autowired
	private BoatswanaInsuranceService serviceBotswana;
	
	@PostMapping("/generate/namibia/quote")
	public Object generateNamibiaQuote(@RequestBody InsuranceReq req) throws WhatsAppValidationException,JsonProcessingException, JsonMappingException{
		return serviceNamibia.generateNamibiaQuote(req);
	}

	@GetMapping("/phoenix/buypolicy/{request}")
	public RedirectView buypolicy(@PathVariable("request") String request,HttpServletResponse reponse) throws Exception{
		String redirectLink = insService.buypolicy(request);
		return new RedirectView(redirectLink);
	}
	
	//@PostMapping("/generate/zambia/quote")
	//public Object generateZambiaQuote(@RequestBody Object req) throws WhatsAppValidationException,JsonProcessingException, JsonMappingException{
		//return serviceZambia.generateZambiaQuote(req);
	//}
	
	//@PostMapping("/generate/zambia/quote")
	public Object ZambiaQuote(@RequestBody Object req) throws WhatsAppValidationException,JsonProcessingException, JsonMappingException{
		return serviceZambia.zambiaQuote(req);
	}
	
	//@PostMapping("/generate/swaziland/quote")
	public Object generateSwazilandQuote(@RequestBody Object req) throws WhatsAppValidationException,JsonProcessingException, JsonMappingException{
		return serviceSwaziland.swazilandQuote(req);
	}
	
	@PostMapping("/generate/swaziland/quote")
	public Object swazilandQuoteGenerate(@RequestBody Map<String, String> req) throws WhatsAppValidationException,JsonProcessingException, JsonMappingException{
		return serviceSwaziland.swazilandQuoteGenerate(req);
	}
	
	@PostMapping("/doc/namibia/response")
	public Object namibiaDocResponse(@RequestBody InsuranceReq req) {
		return  serviceNamibia.docResponseSetter(req);
	}
	
	@PostMapping("/namibia/quote/generate")
	public Object genNamibiaQuote(@RequestBody Object req) throws WhatsAppValidationException,JsonProcessingException, JsonMappingException{
		return  serviceNamibia.quoteGenerationNamibia(req);
	}
	
	@PostMapping("/boatswana/quote/generate")
	public Object genBoatswanaQuote(@RequestBody Object req) throws WhatsAppValidationException,JsonProcessingException, JsonMappingException{
		return  serviceBotswana.quoteGenerationBoatswana(req);
	}
	
	@PostMapping("/boatswana/payment/generate")
	public Object generateBoatswanaPaymentLink(@RequestBody Object req) throws WhatsAppValidationException,JsonProcessingException, JsonMappingException{
		return  serviceBotswana.paymentLinkGenerationBoatswana(req);
	}
	
	@PostMapping("/generate/zambia/quote")
	public Object generateZambiaQuote(@RequestBody Object req) throws WhatsAppValidationException,JsonProcessingException, JsonMappingException{
		return serviceZambia.generateZambiaMotorQuote(req);
	}
}
