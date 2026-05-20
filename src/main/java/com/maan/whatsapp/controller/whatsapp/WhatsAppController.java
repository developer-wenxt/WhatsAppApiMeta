package com.maan.whatsapp.controller.whatsapp;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.maan.whatsapp.auth.basic.WhatsappEncryptionDecryption;
import com.maan.whatsapp.insurance.AsyncProcessThread;
import com.maan.whatsapp.insurance.InsuranceServiceImpl;
import com.maan.whatsapp.meta.FlowCreateQuoteReq;
import com.maan.whatsapp.meta.Messages;
import com.maan.whatsapp.meta.MetaEncryptDecryptRes;
import com.maan.whatsapp.meta.MetaWebhookRequest;
import com.maan.whatsapp.request.motor.WAQuoteReq;
import com.maan.whatsapp.request.wati.getcont.WebhookReq;
import com.maan.whatsapp.request.whatsapp.WhatsAppReq;
import com.maan.whatsapp.response.motor.WAQuoteRes;
import com.maan.whatsapp.service.whatsapp.WhatsAppService;

import jakarta.servlet.http.HttpServletResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RestController
@RequestMapping("/whatsapp")
public class WhatsAppController {

	Logger log =LogManager.getLogger(WhatsAppController.class);
	
	private static OkHttpClient okhttp = new OkHttpClient.Builder()
			.readTimeout(30, TimeUnit.SECONDS)
			.build();
	
	ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	private InsuranceServiceImpl insurance;
	
	@Autowired
	private AsyncProcessThread asyncThread;
	
	
	@Autowired
	private WhatsAppService whatsappSer;
	
	public static Gson printReq =new Gson();
	
	@PostMapping("/saveDetail")
	public String saveRequestDetail(@RequestBody List<WhatsAppReq> request) {
		String response = whatsappSer.saveRequestDetail(request);
		return response;
	}

	@PostMapping("/webhook")
	public String webhookRes(@RequestBody WebhookReq request) {
		log.info("Webhook request ==>"+printReq.toJson(request));
		String response = whatsappSer.webhookRes(request);
		return response;
	}
	
	@PostMapping("/webhook/meta")
	public ResponseEntity<String> webhookRes(@RequestBody MetaWebhookRequest req,HttpServletResponse res) throws IOException {
		WebhookReq webhookReq = new WebhookReq();
		try {	
		 if(req!=null) {
			 
			List<Messages> msg =req.getEntry()==null?null
					:req.getEntry().get(0).getChanges()==null?null
					:req.getEntry().get(0).getChanges().get(0).getValue().getMessages()==null?null
					:req.getEntry().get(0).getChanges().get(0).getValue().getMessages();
			
			if(msg!=null) {
				log.info("meta/Webhook request ==>"+printReq.toJson(req));
				
				String from =msg.get(0).getFrom();
					
				String type =msg.get(0).getType();
				
				webhookReq.setPhoneNumberId(req.getEntry().get(0).getChanges().get(0).getValue().getMetadata().getPhone_number_id());
				webhookReq.setDisplayMobileNo(req.getEntry().get(0).getChanges().get(0).getValue().getMetadata().getDisplay_phone_number());
				webhookReq.setType(type);
				webhookReq.setWhatsappMessageId(msg.get(0).getId());
				webhookReq.setWaId(from);
				
				if("text".equalsIgnoreCase(type)) {
					webhookReq.setText(msg.get(0).getText().getBody());	
					
				}else if("location".equalsIgnoreCase(type)) {
					
					String locationUrl ="https://www.google.com/maps/search/"+msg.get(0).getLocation().getLatitude()+","+msg.get(0).getLocation().getLongitude()+"";
					webhookReq.setData(locationUrl);
					
				}else if("image".equalsIgnoreCase(type)) {
					
					webhookReq.setImageId(msg.get(0).getImage().getId());
					webhookReq.setData(msg.get(0).getImage().getId());
					webhookReq.setMimeType(msg.get(0).getImage().getMime_type());
					
				}else if("interactive".equalsIgnoreCase(type)) {
					
					String inteType =msg.get(0).getInteractive().getType();
					webhookReq.setInteractiveType(inteType);
					
					if("button_reply".equalsIgnoreCase(inteType)) {
						
						webhookReq.setText(msg.get(0).getInteractive().getButton_reply().getTitle());						
						
					}else if("nfm_reply".equalsIgnoreCase(inteType)) {
						
						String str_json = msg.get(0).getInteractive().getNfm_reply().getResponse_json();
						Map<String,Object> map = mapper.readValue(str_json, Map.class);
						map.remove("wa_flow_response_params");
						String json_as_string =mapper.writeValueAsString(map);
						String encode_str =Base64.getEncoder().encodeToString(json_as_string.getBytes());
						webhookReq.setText(encode_str);
					
					}else if("list_reply".equalsIgnoreCase(inteType)) {
						webhookReq.setText(msg.get(0).getInteractive().getList_reply().getId());
						webhookReq.setButtonTitle(msg.get(0).getInteractive().getList_reply().getTitle());
					
					}
					
					webhookReq.setType("text");
				}
				
				webhookReq.setTimestamp(msg.get(0).getTimestamp());
				webhookReq.setSenderName(req.getEntry().get(0).getChanges().get(0).getValue().getContacts().get(0).getProfile().getName());
				
				
				log.info("Modfied Webhook request || "+printReq.toJson(webhookReq));
				
				whatsappSer.webhookRes(webhookReq);
					
				res.setStatus(200);

				return new ResponseEntity<String>("",HttpStatus.OK);
					
				
			}
		 }
		 
		}catch (Exception e) {
			e.printStackTrace();
			res.setStatus(200);
		}
		res.setStatus(200);
		return new ResponseEntity<String>("",HttpStatus.OK);
		
	}
	
	
	
	
	private void sendFlowMessage(String from) throws IOException {
		String txt ="```Welcome to Alliance Bima ChapChap, Please choose your product to get insurance```\n\n*Choose 1* : Motor Claim\n*Choose 2* : Motor Insurance\n*Choose 3* : PreInspection Image Upload\n*Choose 4* : Inalipa Customer Claim\n*Choose 00* : To Change Your Language\n*Choose 10* : B2C Form Page";
		
		Map<String,Object> header =new HashMap<String, Object>();
		Map<String,Object> body =new HashMap<String, Object>();
		Map<String,Object> footer =new HashMap<String, Object>();
		Map<String,Object> action =new HashMap<String, Object>();
		
		header.put("type", "text");
		header.put("text", "Alliance Insurance Corporation Limited");
		
		body.put("text", txt);
		footer.put("text", "");
		
		String pay_1 ="[ {\"id\": \"1\", \"title\": \"--SELECT--\"}]";
		String pay_2 ="[ {\"id\": \"1\", \"title\": \"--SELECT--\"}]";
		String pay_3 ="[ {\"id\": \"1\", \"title\": \"--SELECT--\"}]";
		String pay_4 ="[ {\"id\": \"1\", \"title\": \"--SELECT--\"}]";
		
		Map<String,Object> data =new HashMap<String, Object>();
		data.put("sectionName", mapper.readValue(pay_1, List.class));
		data.put("bodyType", mapper.readValue(pay_2, List.class));
		data.put("idType",  mapper.readValue(pay_3, List.class));
		data.put("vehicleUsage",  mapper.readValue(pay_4, List.class));
		
		Map<String,Object> flow_action_payload =new HashMap<String, Object>();
		flow_action_payload.put("screen", "QUOTATION");
		flow_action_payload.put("data", data);
		
		Map<String,Object> parameters =new HashMap<String, Object>();
		parameters.put("flow_message_version", "3");
		parameters.put("flow_token", "a4d4852a-1be4-480e-95eb-41d456d867f8");
		parameters.put("flow_id", "3770791093210120");
		parameters.put("flow_cta", "Buy Insurance");
		parameters.put("flow_action", "navigate");
		parameters.put("flow_action_payload", flow_action_payload);
		
		action.put("name", "flow");
		action.put("parameters", parameters);
		
		
		Map<String,Object> interactive =new HashMap<String, Object>();
		interactive.put("type", "flow");
		interactive.put("header", header);
		interactive.put("body", body);
		interactive.put("footer", footer);
		interactive.put("action", action);
		
		
		Map<String,Object> baseRequest =new HashMap<String, Object>();
		baseRequest.put("messaging_product", "whatsapp");
		baseRequest.put("recipient_type", "individual");
		baseRequest.put("to", from);
		baseRequest.put("type", "interactive");
		baseRequest.put("interactive", interactive);
		
		
		String message =new Gson().toJson(baseRequest);
		System.out.println("FLOW --SEND MESSAGE REQUEST || "+message);
		MediaType contentType =MediaType.parse("application/json");
		okhttp3.RequestBody reqBody =okhttp3.RequestBody.create(message,contentType);
		String url ="https://graph.facebook.com/v18.0/303735452816578/messages";
		
		Request sendReq = new Request.Builder()
				.url(url)
				//.addHeader("Authorization", "Bearer EAAFjP8nCyKwBO16RUZC8AQtyvTLKKVHKZCKnwvxSJedSYCGBR6tiJmlw94lkU1cgDYyTTwNOWieG0HhD1rkU413waWk8e58eaLnjTS2ZBJdAn6bxrimuHpGZCIZBePZBZCjORRDs5hxBxwchtyvlppmmZAepVoB02pCOgIH3FbORed4GOsfvfHjeMZBbU3ZAXYslhd2DCAMDUYiXpv4fZAt4DgZD")
				.addHeader("Authorization", "Bearer EAAFjP8nCyKwBOwm56D8cdAL2Ar9e7ZCQaU3yn6BP23GACuGoFZBo8rrnSZBkTBuPAFGUXQh0TBvg8QGHsM8qaGkUnSN5SfRV8HUsHyKJANSmGz4l77mXOpZAdfKZCCWlwoYZCIoFtZBekPGelKODSYOPdvLPHD4XbGn6ooFK172ByksoTBmnXRnAG1Tmj7qZCHev")
				.post(reqBody)
				.build();

		Response response = okhttp.newCall(sendReq).execute();

		String responseString = response.body().string();

		System.out.println("FLOW --SEND RESPONSE ||"+responseString);
		
	}

	//@GetMapping("/webhook/meta")
	public ResponseEntity<String> getWebhookRequest(@RequestHeader("hub_mode") String hub_mode,@RequestHeader("hub.challenge") String hub_challenge,
			@RequestHeader("hub.verify_token") String hub_verify_token) {		
		System.out.println( "hub_mode  "+hub_mode +" || hub_challenge " +hub_challenge +" || hub_verify_token"+hub_verify_token);
		if("AllianceInsurance@001".equals(hub_verify_token)) {
			
			return new ResponseEntity<String>(hub_challenge,HttpStatus.OK);
			
		}else {
			return new ResponseEntity<String>(HttpStatus.FORBIDDEN);
		}
	}
	
	@GetMapping("/webhook/meta")
    public ResponseEntity<String> webhookGet(@RequestParam("hub.mode") String hubMode,
                                             @RequestParam("hub.verify_token") String verifyToken,
                                             @RequestParam("hub.challenge") String hubChallenge) {
		System.out.println( "hub_mode  "+hubMode +" || hub_challenge " +hubChallenge +" || hub_verify_token"+verifyToken);

        if ("subscribe".equals(hubMode) && "AllianceInsurance@001".equals(verifyToken)) {
            return ResponseEntity.ok(hubChallenge);
        } else {
            return ResponseEntity.status(403).body("Success");
        }
    }

	@GetMapping("/send/sessExpMsg")
	public String sendSessExpMsg() {
		String response = whatsappSer.sendSessExpMsg();
		return response;
	}

	@PostMapping("/msg/content")
	public List<WAQuoteRes> getWAMsgDet(@RequestBody WAQuoteReq request) {
		List<WAQuoteRes> response = whatsappSer.getWAMsgDet(request);
		return response;
	}
	
	@PostMapping("/webhook/meta/quote")
	public ResponseEntity<Object> webhookRes1(@RequestBody Map<Object,Object> request) throws IOException {
		log.info("meta/Webhook request ==>"+printReq.toJson(request));
		
		String response =WhatsappEncryptionDecryption.whatsappEncryptionDecryption(request);
		
		return new ResponseEntity<Object>(response,HttpStatus.OK);
	}
	
	
	
	@PostMapping("/webhook/meta/dynamic")
	public ResponseEntity<Object> dynamic(@RequestBody Map<String,Object> request) throws IOException {
		log.info("/webhook/meta/dynamic ==>"+printReq.toJson(request));
		MetaEncryptDecryptRes decrypt =WhatsappEncryptionDecryption.metaDecryption(request);
		Map<String,Object> healthCheck =mapper.readValue(decrypt.getEncrypted_flow_data(), Map.class);
		
		String action =healthCheck.get("action")==null?"":healthCheck.get("action").toString();
		
		// health check
		if("ping".equals(action)) {
			
			String version =healthCheck.get("version")==null?"":healthCheck.get("version").toString();
			log.info("/webhook/meta/dynamic health check version  ==>"+version);
			Map<String,Object> data =new HashMap<String, Object>();
			data.put("status", "active");
			
			Map<String,Object> healthCheckReq =new HashMap<String, Object>();
			healthCheckReq.put("version", version);
			healthCheckReq.put("data", data);
			
			String encryptReq =printReq.toJson(healthCheckReq);
			
			
			decrypt.setEncrypted_flow_data(encryptReq);
			String response =WhatsappEncryptionDecryption.metaEncryption(decrypt);
			
			return new ResponseEntity<Object>(response,HttpStatus.OK);
			
		}else if(!"INIT".equals(action)){
		
			log.info("meta/dynamic request ==>"+printReq.toJson(request));

			
			Map<String,Object> flowReq =(Map<String,Object>) healthCheck.get("data");
			
			String type =flowReq.get("type")==null?"":flowReq.get("type").toString();
			
			if("MASTERS".equalsIgnoreCase(type)) {
			
				Map<String,Object> data = insurance.getWhatsappFlowMaster();
		
				Map<String,Object> encryReq =new HashMap<String, Object>();
				encryReq.put("version", "3.0");
				encryReq.put("screen", "QUOTATION");
				encryReq.put("data", data);
				
				String encryptReq =printReq.toJson(encryReq);
				
				
				
				decrypt.setEncrypted_flow_data(encryptReq);
				String response =WhatsappEncryptionDecryption.metaEncryption(decrypt);
				
				return new ResponseEntity<Object>(response,HttpStatus.OK);
				
			}else if("VEHICLE_USAGE".equalsIgnoreCase(type)){
				
				
				String sectionId =flowReq.get("sectionName")==null?"":flowReq.get("sectionName").toString();
				String token =asyncThread.getEwayToken();
				List<Map<String,String>> vehiUsage =asyncThread.getVehicleUsage(token, sectionId);
				Map<String,Object> data =new HashMap<String, Object>();
				data.put("vehicleUsage", vehiUsage);
				
				Map<String,Object> encryReq =new HashMap<String, Object>();
				encryReq.put("version", "3.0");
				encryReq.put("screen", "QUOTATION");
				encryReq.put("data", data);
				
				String encryptReq =printReq.toJson(encryReq);
				
				decrypt.setEncrypted_flow_data(encryptReq);
				
				String response =WhatsappEncryptionDecryption.metaEncryption(decrypt);
				
				return  new ResponseEntity<Object>(response,HttpStatus.OK);
				
				
			}else if("QUOTE_RESPONSE".equalsIgnoreCase(type))	{
				
				Map<String,Object> flow =(Map<String,Object>) healthCheck.get("data");
				FlowCreateQuoteReq quoteReq =new FlowCreateQuoteReq();
				quoteReq.setAirtelPayNo(flow.get("mobileNo").toString());
				quoteReq.setBodyType(flow.get("bodyType").toString());
				quoteReq.setClaimType(flow.get("claimyn").toString());
				quoteReq.setCustomerName(flow.get("customerName").toString());
				quoteReq.setIdNumber(flow.get("idNumber").toString());
				quoteReq.setIdType(flow.get("idType").toString());
				quoteReq.setMobileNo(flow.get("mobileNo").toString());
				quoteReq.setMotorUsageId(flow.get("vehicleUsage").toString());
				quoteReq.setRegisrationNo(flow.get("registrationNo").toString());
				//quoteReq.setRegisrationNo("T470EGF");
				
				quoteReq.setSectionId(flow.get("sectionName").toString());
				quoteReq.setSumInsured(flow.get("sumInsured")==null?"0":flow.get("sumInsured").toString());
				quoteReq.setTypeofInsurance(flow.get("policyType").toString());
				quoteReq.setWhatsAppCode("255");
				quoteReq.setWhatsAppNo(flow.get("mobileNo").toString());
				
				log.info("meta/dynamic create quote request ==>"+printReq.toJson(request));
				
				Map<String,Object> quoteRes =insurance.flowCreateQuote(quoteReq);
				
				Map<String,Object> qr =(Map<String,Object>)quoteRes.get("Response");
				
				String referenceno =qr.get("referenceno")==null?"N/A":qr.get("referenceno").toString();
				String inceptiondate =qr.get("inceptiondate")==null?"N/A":qr.get("inceptiondate").toString();
				String expirydate =qr.get("expirydate")==null?"N/A":qr.get("expirydate").toString();
				String registration =qr.get("registration")==null?"N/A":qr.get("registration").toString();
				String chassis =qr.get("chassis")==null?"N/A":qr.get("chassis").toString();
				String suminsured =qr.get("suminsured")==null?"N/A":qr.get("suminsured").toString();
				String usage =qr.get("usage")==null?"N/A":qr.get("usage").toString();
				String vehtype =qr.get("vehtype")==null?"N/A":qr.get("vehtype").toString();
				String color =qr.get("color")==null?"N/A":qr.get("color").toString();
				String premium =qr.get("premium")==null?"N/A":qr.get("premium").toString();
				String vatamt =qr.get("vatamt")==null?"N/A":qr.get("vatamt").toString();
				String totalpremium =qr.get("totalpremium")==null?"N/A":qr.get("totalpremium").toString();
				String vat =qr.get("vat")==null?"N/A":qr.get("vat").toString();
				
				
				
				Map<String,Object> data =new HashMap<String, Object>();
				data.put("registrationNo", "Registration No : "+registration+"");
				data.put("chassisNo", "Chassis No : "+chassis+"");
				data.put("vehicleUsage", "Vehicle Usage : "+usage+"");
				data.put("vehicleType", "Vehicle Type : "+vehtype+"");
				data.put("vehicleColor", "Vehicle Color : "+color+"");
				data.put("sumInsured", "SumInsured  : "+suminsured+"");
				data.put("premium", "Premium  : "+premium+"");
				data.put("vat", "VAT("+vat+") : "+vatamt+"");
				data.put("totalPremium", "Total Premium : "+totalpremium+" TZS");
				data.put("inceptionDate", "Inception Date : "+inceptiondate+"");
				data.put("expiryDate", "Expiry Date : "+expirydate+"");
				data.put("referenceNo", "Reference no : "+referenceno+"");
				data.put("vehilceHederName", "Your Vehicle Details");
				data.put("premiumHeaderName", "Your Premium Details");
				data.put("policyHeaderName", "Policy Details");
				
				
				Map<String,Object> encryReq =new HashMap<String, Object>();
				encryReq.put("version", "3.0");
				encryReq.put("screen", "QUOTE_RESPONSE");
				encryReq.put("data", data);
				
				
				String encryptReq =printReq.toJson(encryReq);
				
				decrypt.setEncrypted_flow_data(encryptReq);
				
				String response =WhatsappEncryptionDecryption.metaEncryption(decrypt);
				
				return  new ResponseEntity<Object>(response,HttpStatus.OK);
			

			}
		}
		return null;
		
	}

	
	@PostMapping("/webhook/meta/quotation")
	public ResponseEntity<Object> quotation(@RequestBody Map<String,Object> request) throws IOException {
		log.info("/webhook/meta/dynamic ==>"+printReq.toJson(request));
		MetaEncryptDecryptRes decrypt =WhatsappEncryptionDecryption.metaDecryption(request);
		Map<String,Object> healthCheck =mapper.readValue(decrypt.getEncrypted_flow_data(), Map.class);
		
		String action =healthCheck.get("action")==null?"":healthCheck.get("action").toString();
		
		// health check
		if("ping".equals(action)) {
			
			String version =healthCheck.get("version")==null?"":healthCheck.get("version").toString();
			log.info("/webhook/meta/dynamic health check version  ==>"+version);
			Map<String,Object> data =new HashMap<String, Object>();
			data.put("status", "active");
			
			Map<String,Object> healthCheckReq =new HashMap<String, Object>();
			healthCheckReq.put("version", version);
			healthCheckReq.put("data", data);
			
			return new ResponseEntity<Object>(healthCheckReq,HttpStatus.OK);
			
		}else {
		
			log.info("meta/dynamic request ==>"+printReq.toJson(request));

			
			Map<String,Object> flowReq =(Map<String,Object>) healthCheck.get("data");
			
			String type =flowReq.get("type")==null?"":flowReq.get("type").toString();
			
			if("MASTERS".equalsIgnoreCase(type)) {
			
				Map<String,Object> data = insurance.getWhatsappFlowMaster();
		
				Map<String,Object> encryReq =new HashMap<String, Object>();
				encryReq.put("version", "3.0");
				encryReq.put("screen", "QUOTATION");
				encryReq.put("data", data);
				
				String encryptReq =printReq.toJson(encryReq);
				
				
				decrypt.setEncrypted_flow_data(encryptReq);
				String response =WhatsappEncryptionDecryption.metaEncryption(decrypt);
				
				return new ResponseEntity<Object>(response,HttpStatus.OK);
				
			}else if("VEHICLE_USAGE".equalsIgnoreCase(type)){
				
				
				String sectionId =flowReq.get("sectionName")==null?"":flowReq.get("sectionName").toString();
				String token =asyncThread.getEwayToken();
				List<Map<String,String>> vehiUsage =asyncThread.getVehicleUsage(token, sectionId);
				Map<String,Object> data =new HashMap<String, Object>();
				data.put("vehicleUsage", vehiUsage);
				
				Map<String,Object> encryReq =new HashMap<String, Object>();
				encryReq.put("version", "3.0");
				encryReq.put("screen", "QUOTATION");
				encryReq.put("data", data);
				
				String encryptReq =printReq.toJson(encryReq);
				
				decrypt.setEncrypted_flow_data(encryptReq);
				
				String response =WhatsappEncryptionDecryption.metaEncryption(decrypt);
				
				return  new ResponseEntity<Object>(response,HttpStatus.OK);
				
				
			}else if("QUOTE_RESPONSE".equalsIgnoreCase(type))	{
				
				Map<String,Object> flow =(Map<String,Object>) healthCheck.get("data");
				FlowCreateQuoteReq quoteReq =new FlowCreateQuoteReq();
				quoteReq.setAirtelPayNo(flow.get("mobileNo").toString());
				quoteReq.setBodyType(flow.get("bodyType").toString());
				quoteReq.setClaimType(flow.get("claimyn").toString());
				quoteReq.setCustomerName(flow.get("customerName").toString());
				quoteReq.setIdNumber(flow.get("idNumber").toString());
				quoteReq.setIdType(flow.get("idType").toString());
				quoteReq.setMobileNo(flow.get("mobileNo").toString());
				quoteReq.setMotorUsageId(flow.get("vehicleUsage").toString());
				quoteReq.setRegisrationNo(flow.get("registrationNo").toString());
				//quoteReq.setRegisrationNo("T470EGF");
				
				quoteReq.setSectionId(flow.get("sectionName").toString());
				quoteReq.setSumInsured(flow.get("sumInsured")==null?"0":flow.get("sumInsured").toString());
				quoteReq.setTypeofInsurance(flow.get("policyType").toString());
				quoteReq.setWhatsAppCode("255");
				quoteReq.setWhatsAppNo(flow.get("mobileNo").toString());
				
				log.info("meta/dynamic create quote request ==>"+printReq.toJson(request));
				
				Map<String,Object> quoteRes =insurance.flowCreateQuote(quoteReq);
				
				Map<String,Object> qr =(Map<String,Object>)quoteRes.get("Response");
				
				String referenceno =qr.get("referenceno")==null?"N/A":qr.get("referenceno").toString();
				String inceptiondate =qr.get("inceptiondate")==null?"N/A":qr.get("inceptiondate").toString();
				String expirydate =qr.get("expirydate")==null?"N/A":qr.get("expirydate").toString();
				String registration =qr.get("registration")==null?"N/A":qr.get("registration").toString();
				String chassis =qr.get("chassis")==null?"N/A":qr.get("chassis").toString();
				String suminsured =qr.get("suminsured")==null?"N/A":qr.get("suminsured").toString();
				String usage =qr.get("usage")==null?"N/A":qr.get("usage").toString();
				String vehtype =qr.get("vehtype")==null?"N/A":qr.get("vehtype").toString();
				String color =qr.get("color")==null?"N/A":qr.get("color").toString();
				String premium =qr.get("premium")==null?"N/A":qr.get("premium").toString();
				String vatamt =qr.get("vatamt")==null?"N/A":qr.get("vatamt").toString();
				String totalpremium =qr.get("totalpremium")==null?"N/A":qr.get("totalpremium").toString();
				String vat =qr.get("vat")==null?"N/A":qr.get("vat").toString();
				
				
				
				Map<String,Object> data =new HashMap<String, Object>();
				data.put("registrationNo", "Registration No : "+registration+"");
				data.put("chassisNo", "Chassis No : "+chassis+"");
				data.put("vehicleUsage", "Vehicle Usage : "+usage+"");
				data.put("vehicleType", "Vehicle Type : "+vehtype+"");
				data.put("vehicleColor", "Vehicle Color : "+color+"");
				data.put("sumInsured", "SumInsured  : "+suminsured+"");
				data.put("premium", "Premium  : "+premium+"");
				data.put("vat", "VAT("+vat+") : "+vatamt+"");
				data.put("totalPremium", "Total Premium : "+totalpremium+" TZS");
				data.put("inceptionDate", "Inception Date : "+inceptiondate+"");
				data.put("expiryDate", "Expiry Date : "+expirydate+"");
				data.put("referenceNo", "Reference no : "+referenceno+"");
				data.put("vehilceHederName", "Your Vehicle Details");
				data.put("premiumHeaderName", "Your Premium Details");
				data.put("policyHeaderName", "Policy Details");
				
				
				Map<String,Object> encryReq =new HashMap<String, Object>();
				encryReq.put("version", "3.0");
				encryReq.put("screen", "QUOTE_RESPONSE");
				encryReq.put("data", data);
				
				
				String encryptReq =printReq.toJson(encryReq);
				
				decrypt.setEncrypted_flow_data(encryptReq);
				
				String response =WhatsappEncryptionDecryption.metaEncryption(decrypt);
				
				return  new ResponseEntity<Object>(response,HttpStatus.OK);
			

			}
		}
		String version =healthCheck.get("version")==null?"":healthCheck.get("version").toString();
		log.info("/webhook/meta/dynamic health check version  ==>"+version);
		Map<String,Object> data =new HashMap<String, Object>();
		data.put("status", "active");
		
		Map<String,Object> healthCheckReq =new HashMap<String, Object>();
		healthCheckReq.put("version", version);
		healthCheckReq.put("data", data);
		
		return new ResponseEntity<Object>(healthCheckReq,HttpStatus.OK);
		
	}
	
	
	//@PostConstruct
	public void convertto_base64() throws JsonMappingException, JsonProcessingException {
		
		
		String str ="{\"vehicle_color\":\"1\",\"address\":\"Chennai\",\"fuel_used\":\"1\",\"mobile_no\":\"123456789\",\"title\":\"1\",\"country_code\":\"255\",\"seating_capacity\":\"4\",\"flow_token\":\"0251a97e-bd67-494f-9f93-83fef81b912c\",\"chassis_no\":\"13267\",\"body_type\":\"1\",\"manufacture_year\":\"2020\",\"broker_loginid\":\"\",\"model\":\"102103\",\"customer_name\":\"Joy\",\"region\":\"23000\",\"make\":\"21\",\"engine_capacity\":\"1000\",\"isbroker\":\"2\",\"email\":\"joy@123.com\",\"vehicle_usage\":\"17\",\"wa_flow_response_params\":{\"title\":\"STP QUOTE\",\"flow_id\":\"985913242664526\",\"flow_name\":\"STP_POLICY_22_05_2024_02\",\"response_message\":\"{\\\"screens\\\":[{\\\"id\\\":\\\"CUSTOMER_DETAILS\\\",\\\"title\\\":\\\"Customer Information\\\",\\\"components\\\":[{\\\"name\\\":\\\"title\\\",\\\"label\\\":\\\"Title\\\"},{\\\"name\\\":\\\"customer_name\\\",\\\"label\\\":\\\"Customer Name\\\"},{\\\"name\\\":\\\"country_code\\\",\\\"label\\\":\\\"Country Code\\\"},{\\\"name\\\":\\\"mobile_no\\\",\\\"label\\\":\\\"Mobile No\\\"},{\\\"name\\\":\\\"email\\\",\\\"label\\\":\\\"Email ID\\\"},{\\\"name\\\":\\\"address\\\",\\\"label\\\":\\\"Address\\\"},{\\\"name\\\":\\\"region\\\",\\\"label\\\":\\\"Region\\\"}]},{\\\"id\\\":\\\"VEHICLE_DETAILS\\\",\\\"title\\\":\\\"Vehicle Information\\\",\\\"components\\\":[{\\\"name\\\":\\\"isbroker\\\",\\\"label\\\":\\\"Who is creating quotation?\\\"},{\\\"name\\\":\\\"broker_loginid\\\",\\\"label\\\":\\\"Broker LoginId\\\"},{\\\"name\\\":\\\"chassis_no\\\",\\\"label\\\":\\\"Chassis Number\\\"},{\\\"name\\\":\\\"body_type\\\",\\\"label\\\":\\\"Body Type\\\"},{\\\"name\\\":\\\"make\\\",\\\"label\\\":\\\"Make\\\"},{\\\"name\\\":\\\"model\\\",\\\"label\\\":\\\"Model\\\"},{\\\"name\\\":\\\"engine_capacity\\\",\\\"label\\\":\\\"Engine Capacity\\\"},{\\\"name\\\":\\\"manufacture_year\\\",\\\"label\\\":\\\"Manufacture Year\\\"},{\\\"name\\\":\\\"fuel_used\\\",\\\"label\\\":\\\"Fuel Used\\\"},{\\\"name\\\":\\\"vehicle_color\\\",\\\"label\\\":\\\"Vehicle Color\\\"},{\\\"name\\\":\\\"vehicle_usage\\\",\\\"label\\\":\\\"Vehicle Usage\\\"},{\\\"name\\\":\\\"seating_capacity\\\",\\\"label\\\":\\\"Seating Capacity\\\"}]}],\\\"response\\\":[{\\\"id\\\":\\\"CUSTOMER_DETAILS\\\",\\\"title\\\":\\\"Customer Information\\\",\\\"components\\\":[{\\\"name\\\":\\\"title\\\",\\\"type\\\":\\\"Dropdown\\\",\\\"label\\\":\\\"Title\\\",\\\"value\\\":\\\"Mr\\\"},{\\\"name\\\":\\\"customer_name\\\",\\\"type\\\":\\\"TextInput\\\",\\\"label\\\":\\\"Customer Name\\\",\\\"value\\\":\\\"Joy\\\"},{\\\"name\\\":\\\"country_code\\\",\\\"type\\\":\\\"Dropdown\\\",\\\"label\\\":\\\"Country Code\\\",\\\"value\\\":\\\"255\\\"},{\\\"name\\\":\\\"mobile_no\\\",\\\"type\\\":\\\"TextInput\\\",\\\"label\\\":\\\"Mobile No\\\",\\\"value\\\":\\\"123456789\\\"},{\\\"name\\\":\\\"email\\\",\\\"type\\\":\\\"TextInput\\\",\\\"label\\\":\\\"Email ID\\\",\\\"value\\\":\\\"joy@123.com\\\"},{\\\"name\\\":\\\"address\\\",\\\"type\\\":\\\"TextArea\\\",\\\"label\\\":\\\"Address\\\",\\\"value\\\":\\\"Chennai\\\"},{\\\"name\\\":\\\"region\\\",\\\"type\\\":\\\"Dropdown\\\",\\\"label\\\":\\\"Region\\\",\\\"value\\\":\\\"Arusha\\\"}]},{\\\"id\\\":\\\"VEHICLE_DETAILS\\\",\\\"title\\\":\\\"Vehicle Information\\\",\\\"components\\\":[{\\\"name\\\":\\\"isbroker\\\",\\\"type\\\":\\\"RadioButtonsGroup\\\",\\\"label\\\":\\\"Who is creating quotation?\\\",\\\"value\\\":\\\"Self\\\"},{\\\"name\\\":\\\"chassis_no\\\",\\\"type\\\":\\\"TextInput\\\",\\\"label\\\":\\\"Chassis Number\\\",\\\"value\\\":\\\"13267\\\"},{\\\"name\\\":\\\"body_type\\\",\\\"type\\\":\\\"Dropdown\\\",\\\"label\\\":\\\"Body Type\\\",\\\"value\\\":\\\"SALOON\\\"},{\\\"name\\\":\\\"make\\\",\\\"type\\\":\\\"Dropdown\\\",\\\"label\\\":\\\"Make\\\",\\\"value\\\":\\\"AUDI\\\"},{\\\"name\\\":\\\"model\\\",\\\"type\\\":\\\"Dropdown\\\",\\\"label\\\":\\\"Model\\\",\\\"value\\\":\\\"100\\\"},{\\\"name\\\":\\\"engine_capacity\\\",\\\"type\\\":\\\"TextInput\\\",\\\"label\\\":\\\"Engine Capacity\\\",\\\"value\\\":\\\"1000\\\"},{\\\"name\\\":\\\"manufacture_year\\\",\\\"type\\\":\\\"Dropdown\\\",\\\"label\\\":\\\"Manufacture Year\\\",\\\"value\\\":\\\"2020\\\"},{\\\"name\\\":\\\"fuel_used\\\",\\\"type\\\":\\\"Dropdown\\\",\\\"label\\\":\\\"Fuel Used\\\",\\\"value\\\":\\\"Diesel\\\"},{\\\"name\\\":\\\"vehicle_color\\\",\\\"type\\\":\\\"Dropdown\\\",\\\"label\\\":\\\"Vehicle Color\\\",\\\"value\\\":\\\"Tektite Grey\\\"},{\\\"name\\\":\\\"vehicle_usage\\\",\\\"type\\\":\\\"Dropdown\\\",\\\"label\\\":\\\"Vehicle Usage\\\",\\\"value\\\":\\\"Private\\\"},{\\\"name\\\":\\\"seating_capacity\\\",\\\"type\\\":\\\"TextInput\\\",\\\"label\\\":\\\"Seating Capacity\\\",\\\"value\\\":\\\"4\\\"}]}]}\"}}";

	
		Map<String,Object> map = mapper.readValue(str, Map.class);
		
		map.remove("wa_flow_response_params");
		
		System.out.println(map);
	}

}
