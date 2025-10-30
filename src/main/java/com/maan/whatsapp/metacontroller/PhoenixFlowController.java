package com.maan.whatsapp.metacontroller;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.maan.whatsapp.auth.basic.WhatsappEncryptionDecryption;
import com.maan.whatsapp.config.exception.WhatsAppValidationException;
import com.maan.whatsapp.insurance.NamibiaInsuranceService;
import com.maan.whatsapp.insurance.SwazilandInsuranceService;
import com.maan.whatsapp.meta.Messages;
import com.maan.whatsapp.meta.MetaEncryptDecryptRes;
import com.maan.whatsapp.meta.MetaWebhookRequest;
import com.maan.whatsapp.request.wati.getcont.WebhookReq;
import com.maan.whatsapp.service.whatsapp.PhoenixZambiaWhatsAppService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/whatsappflow")
public class PhoenixFlowController {

	Logger log = LogManager.getLogger(PhoenixFlowController.class);
	
	ObjectMapper mapper = new ObjectMapper();
	
	public static Gson printReq =new Gson();
	
	@Autowired
	private PhoenixZambiaFlowService zambiaService;
	
	@Autowired
	private PhoenixZambiaWhatsAppService whatsappZambiaSer;
	
	@Autowired
	private PhoenixNamibiaFlowService namibiaService;
	
	@Autowired
	private SwazilandInsuranceService serviceSwaziland;
	
	@Autowired
	private PhoenixBoatswanaFlowService serviceBoatswana;
	
	@Autowired
	private NamibiaInsuranceService namibiaInsuranceService;
	
	@PostMapping("/create/zambia/quote")
	public ResponseEntity<Object> createZambiaQoute(@RequestBody Map<String, Object> req) throws JsonMappingException,JsonProcessingException{
		
		log.info("/createZambiaQuote encrypted request : "+printReq.toJson(req));
		MetaEncryptDecryptRes dcryptData = WhatsappEncryptionDecryption.metaDecryption(req);
		Map<String,Object> request = mapper.readValue(dcryptData.getEncrypted_flow_data(), Map.class);
		String action = request.get("action") == null ? "" : request.get("action").toString();
		log.info("/createZambiaQuote decrypted request : "+printReq.toJson(dcryptData));
		
		if("ping".equals(action)) {
			String version = request.get("version") == null ? "" : request.get("version").toString();
			Map<String, Object> data = new HashMap<String,Object>();
			data.put("status", "active");
			
			Map<String,Object> healthCheckReq = new HashMap<String,Object>();
			healthCheckReq.put("version", version);
			healthCheckReq.put("data", data);
			
			String encryptReq = printReq.toJson(healthCheckReq);
			dcryptData.setEncrypted_flow_data(encryptReq);
			String response = WhatsappEncryptionDecryption.metaEncryption(dcryptData);
			
			return new ResponseEntity<Object>(response,HttpStatus.OK);
		}
		else {
			String response = zambiaService.createZambiaQuote(request);
			dcryptData.setEncrypted_flow_data(response);
			String encrypt_response = WhatsappEncryptionDecryption.metaEncryption(dcryptData);
			
			return new ResponseEntity<Object>(encrypt_response,HttpStatus.OK);
		}
	}
	
	@GetMapping("/zambia/flow/screen/data")
	public Map<String,Object> zambiaFlowScreenData(){
		return zambiaService.zambiaFlowScreenData();
	}
	
	@PostMapping("/create/namibia/quote")
	public ResponseEntity<Object> createNamibiaQoute(@RequestBody Map<String, Object> req) throws JsonMappingException,JsonProcessingException{
		log.info("/createNamibiaQuote encrypted request : "+printReq.toJson(req));
		MetaEncryptDecryptRes dcryptData = WhatsappEncryptionDecryption.metaDecryption(req);
		Map<String,Object> request = mapper.readValue(dcryptData.getEncrypted_flow_data(), Map.class);
		String action = request.get("action") == null ? "" : request.get("action").toString();
		log.info("/createNamibiaQuote decrypted request : "+printReq.toJson(dcryptData));
		
		if("ping".equals(action)) {
			String version = request.get("version") == null ? "" : request.get("version").toString();
			Map<String, Object> data = new HashMap<String,Object>();
			data.put("status", "active");
			
			Map<String,Object> healthCheckReq = new HashMap<String,Object>();
			healthCheckReq.put("version", version);
			healthCheckReq.put("data", data);
			
			String encryptReq = printReq.toJson(healthCheckReq);
			dcryptData.setEncrypted_flow_data(encryptReq);
			String response = WhatsappEncryptionDecryption.metaEncryption(dcryptData);
			
			return new ResponseEntity<Object>(response,HttpStatus.OK);
		}else {
			String response = namibiaService.createNamibiaQuote(request);
			dcryptData.setEncrypted_flow_data(response);
			String encrypt_response = WhatsappEncryptionDecryption.metaEncryption(dcryptData);
			
			return new ResponseEntity<Object>(encrypt_response,HttpStatus.OK);
		}
	}
	
	@GetMapping("/namibia/flow/screen/data")
	public Map<String,Object> namibiaFlowScreenData(){
		return namibiaService.namibiaFlowScreenData();
	}
	
	@PostMapping("/swaziland/flow/data")
	public Map<String,Object> createSwazilandFlowRequest(@RequestBody Map<String, Object> req) throws JsonMappingException,JsonProcessingException{
		log.info("/create Swaziland request : "+printReq.toJson(req));
		
	    String responce = serviceSwaziland.swazilandFlowRequest(req);
	    if(StringUtils.isNotBlank(responce)) {
	    	Map<String,Object> mapResponse = mapper.readValue(responce, Map.class);
			return mapResponse;
	    }
	    return null;
	}
	
	@PostMapping("/zambia/flow/data")
	public ResponseEntity<String> zambiaWebhookFlow(@RequestBody MetaWebhookRequest req, HttpServletResponse res)
			throws IOException {
		WebhookReq webhookReq = new WebhookReq();
		try {
			if (req != null) {

				List<Messages> msg = req.getEntry() == null ? null
						: req.getEntry().get(0).getChanges() == null ? null
								: req.getEntry().get(0).getChanges().get(0).getValue().getMessages() == null ? null
										: req.getEntry().get(0).getChanges().get(0).getValue().getMessages();

				if (msg != null) {
					log.info("Zambia/Webhook request ==>" + printReq.toJson(req));

					String from = msg.get(0).getFrom();

					String type = msg.get(0).getType();

					webhookReq.setPhoneNumberId(
							req.getEntry().get(0).getChanges().get(0).getValue().getMetadata().getPhone_number_id());
					webhookReq.setDisplayMobileNo(req.getEntry().get(0).getChanges().get(0).getValue().getMetadata()
							.getDisplay_phone_number());
					webhookReq.setType(type);
					webhookReq.setWhatsappMessageId(msg.get(0).getId());
					webhookReq.setWaId(from);

					if ("text".equalsIgnoreCase(type)) {
						webhookReq.setText(msg.get(0).getText().getBody());

					} else if ("location".equalsIgnoreCase(type)) {

						String locationUrl = "https://www.google.com/maps/search/"
								+ msg.get(0).getLocation().getLatitude() + "," + msg.get(0).getLocation().getLongitude()
								+ "";
						webhookReq.setData(locationUrl);

					} else if ("image".equalsIgnoreCase(type)) {

						webhookReq.setImageId(msg.get(0).getImage().getId());
						webhookReq.setData(msg.get(0).getImage().getId());
						webhookReq.setMimeType(msg.get(0).getImage().getMime_type());

					} else if ("interactive".equalsIgnoreCase(type)) {

						String inteType = msg.get(0).getInteractive().getType();
						webhookReq.setInteractiveType(inteType);

						if ("button_reply".equalsIgnoreCase(inteType)) {

							webhookReq.setText(msg.get(0).getInteractive().getButton_reply().getTitle());

						} else if ("nfm_reply".equalsIgnoreCase(inteType)) {

							String str_json = msg.get(0).getInteractive().getNfm_reply().getResponse_json();
							Map<String, Object> map = mapper.readValue(str_json, Map.class);
							map.remove("wa_flow_response_params");
							String json_as_string = mapper.writeValueAsString(map);
							String encode_str = Base64.getEncoder().encodeToString(json_as_string.getBytes());
							webhookReq.setText(encode_str);

						} else if ("list_reply".equalsIgnoreCase(inteType)) {
							webhookReq.setText(msg.get(0).getInteractive().getList_reply().getId());
							webhookReq.setButtonTitle(msg.get(0).getInteractive().getList_reply().getTitle());

						}

					//	webhookReq.setType("text");
					}

					webhookReq.setTimestamp(msg.get(0).getTimestamp());
					webhookReq.setSenderName(req.getEntry().get(0).getChanges().get(0).getValue().getContacts().get(0)
							.getProfile().getName());

					log.info("Modfied Zambia Webhook request || " + printReq.toJson(webhookReq));

					whatsappZambiaSer.zambiaWebhookFlowRes(webhookReq);

					res.setStatus(200);

					return new ResponseEntity<String>("", HttpStatus.OK);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			res.setStatus(200);
		}
		res.setStatus(200);
		return new ResponseEntity<String>("", HttpStatus.OK);

	}
	
	@PostMapping("/create/boatswana/quote")
	public ResponseEntity<Object> createBoatswanaQoute(@RequestBody Map<String, Object> req) throws JsonMappingException,JsonProcessingException{
		log.info("/createBoatswanaQuote encrypted request : "+printReq.toJson(req));
		MetaEncryptDecryptRes dcryptData = WhatsappEncryptionDecryption.metaDecryption(req);
		Map<String,Object> request = mapper.readValue(dcryptData.getEncrypted_flow_data(), Map.class);
		String action = request.get("action") == null ? "" : request.get("action").toString();
		log.info("/createBoatswanaQuote decrypted request : "+printReq.toJson(dcryptData));
		
		if("ping".equals(action)) {
			String version = request.get("version") == null ? "" : request.get("version").toString();
			Map<String, Object> data = new HashMap<String,Object>();
			data.put("status", "active");
			
			Map<String,Object> healthCheckReq = new HashMap<String,Object>();
			healthCheckReq.put("version", version);
			healthCheckReq.put("data", data);
			
			String encryptReq = printReq.toJson(healthCheckReq);
			dcryptData.setEncrypted_flow_data(encryptReq);
			String response = WhatsappEncryptionDecryption.metaEncryption(dcryptData);
			
			return new ResponseEntity<Object>(response,HttpStatus.OK);
		}else {
			String response = serviceBoatswana.createBoatswanaQuote(request);
			dcryptData.setEncrypted_flow_data(response);
			String encrypt_response = WhatsappEncryptionDecryption.metaEncryption(dcryptData);
			
			return new ResponseEntity<Object>(encrypt_response,HttpStatus.OK);
		}
	}
	
	@GetMapping("/boatswana/flow/screen/data")
	public Map<String,Object> boatswanaFlowScreenData(){
		return serviceBoatswana.boatswanaFlowScreenData();
	}
	
	
	@PostMapping("/nambia/quote")
	public Object testObj(@RequestBody Object req ) {
		log.info("/form flow request format : "+printReq.toJson(req));
		  Object resp= null;
		try {
			resp = namibiaInsuranceService.insuranceFlowNamibia(req);
		} catch (JsonProcessingException | WhatsAppValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resp;
	}
	
	/*@PostMapping("/test")
	public Object testObj(@RequestBody Object req ) {
		log.info("/flow request format : "+printReq.toJson(req));
		  Object resp= null;
		try {
			resp = serviceSwaziland.googleFlowTest(req);
		} catch (JsonProcessingException | WhatsAppValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resp;
	}*/
}
