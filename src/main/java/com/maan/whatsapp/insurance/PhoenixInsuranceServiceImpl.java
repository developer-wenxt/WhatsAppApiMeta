package com.maan.whatsapp.insurance;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.maan.whatsapp.config.exception.WhatsAppValidationException;
import com.maan.whatsapp.response.error.Error;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

@Service
@PropertySource("classpath:WebServiceUrl.properties")
public class PhoenixInsuranceServiceImpl implements PhoenixInsuranceService{
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private PhoenixAsyncProcessThread threadprocess;
	
	Logger log = LogManager.getLogger(getClass());
	
	@Autowired
	@Lazy
	private PhoenixAsyncProcessThread thread;
	
	@Value("${phoenix.swaziland.motor.thirdparty.payementApi}")
	private String thirdPartySwazilanPaymentApi;
	
	@Value("${phoenix.mozambique.motor.thirdparty.payementApi}")
	private String thirdPartyMozambiquePaymentApi;
	
	@Value("${phoenix.namibia.motor.thirdparty.payementApi}")
	private String thirdPartyNamibiaPaymentApi;
	
	@Value("${phoenix.boatawana.motor.thirdparty.payementApi}")
	private String thirdPartyBoatswanaPaymentApi;
	
	@Value("${phoenix.zambia.motor.thirdparty.payementApi}")
	private String thirdPartyZambiaPaymentApi;
	
	@Value("${phoenix.swaziland.motor.thirdparty.paymentCheckApi}")
	private String thirdPartySwazilandPaymentCheckApi;
	
	@Value("${phoenix.mozambique.motor.thirdparty.paymentCheckApi}")
	private String thirdPartyMozambiquePaymentCheckApi;
	
	@Value("${phoenix.namibia.motor.thirdparty.paymentCheckApi}")
	private String thirdPartyNamibiaPaymentCheckApi;
	
	@Value("${phoenix.boatawana.motor.thirdparty.paymentCheckApi}")
	private String thirdPartyBoatswanaPaymentCheckApi;
	
	@Value("${phoenix.zambia.motor.thirdparty.paymentCheckApi}")
	private String thirdPartyZambiaPaymentCheckApi;
	
	@Value("${whatsapp.api}")						
	private String whatsappApi;
	
	@Value("${whatsapp.api.sendSessionMessage}")						
	private String whatsappApiSendSessionMessage;
	
	@Value("${whatsapp.auth}")						
	private String whatsappAuth;
	
	@Value("${whatsapp.api.button}")						
	private String whatsappApiButton;
	
	@Value("${phoenix.tira.post.api}")						
	private String tiraPostApi;
	
	@Value("${phoenix.motor.policy.document}")						
	private String motorPolicyDocumentApi;
	
	@Value("${phoenix.motor.swaziland.redirect.url}")						
	private String swazilandRedirectUrl;
	
	@Value("${phoenix.motor.mozambique.redirect.url}")						
	private String mozambiqueRedirectUrl;
	
	@Value("${phoenix.motor.namibia.redirect.url}")						
	private String namibiaRedirectUrl;
	
	@Value("${phoenix.motor.boatawana.redirect.url}")						
	private String boatswanaRedirectUrl;
	
	@Value("${phoenix.motor.zambia.redirect.url}")						
	private String zambiaRedirectUrl;
	
	@Autowired
	private Gson objectPrint;

	@SuppressWarnings("unchecked")
	@Override
	public String buypolicy(String request) throws WhatsAppValidationException {
		
		List<Error> errorList = new ArrayList<>(2);
		String exception ="",response="";
		
		String decodeStr =new String(Base64.getDecoder().decode(request.getBytes()));
		Map<String, Object> req=null;
		String redirectUrl="";
		
		try {
			req = mapper.readValue(decodeStr, Map.class);
		} catch (Exception e1) {
			e1.printStackTrace();
			exception=e1.getMessage();
		} 
		
		if(StringUtils.isNotBlank(exception)) {
			errorList.add(new Error(exception, "ErrorMsg", "101"));
		}
		
		if(errorList.size()>0) {
			throw new WhatsAppValidationException(errorList);
		}
		
		log.info("BuyPolicy request in WhatsappApp :"+decodeStr);
		
		String merchantRefNo=req.get("MerchantRefNo")==null?"":req.get("MerchantRefNo").toString();
		String CompanyId=req.get("CompanyId")==null?"":req.get("CompanyId").toString();
		String whatsappCode=req.get("WhatsappCode")==null?"":req.get("WhatsappCode").toString();
		String whatsappNo=req.get("WhtsappNo")==null?"":req.get("WhtsappNo").toString();
		String quoteNo=req.get("QuoteNo")==null?"":req.get("QuoteNo").toString();
		
		String phoenixPaymentReqApi ="";
		
		if(CompanyId.equals("100049")) {//swaziland
			phoenixPaymentReqApi =thirdPartySwazilanPaymentApi+merchantRefNo;
		}else if(CompanyId.equals("100046")) {//zambia
			phoenixPaymentReqApi =thirdPartyZambiaPaymentApi+merchantRefNo;
		}else if(CompanyId.equals("100047")) {//boatswana
			phoenixPaymentReqApi =thirdPartyBoatswanaPaymentApi+merchantRefNo;
		}else if(CompanyId.equals("100048")) {//mozambique
			phoenixPaymentReqApi =thirdPartyMozambiquePaymentApi+merchantRefNo;
		}else if(CompanyId.equals("100050")) {//namibia
			phoenixPaymentReqApi =thirdPartyNamibiaPaymentApi+merchantRefNo;
		}
		
		log.info("PhoenixPaymentReqApi || " +phoenixPaymentReqApi);
		
		Map<String,String> payMap =new HashMap<>();
		payMap.put("InsuranceId", CompanyId);
		
		String payReq =objectPrint.toJson(payMap);
		if(CompanyId.equals("100049")) {//swaziland
			response = thread.callSwazilandComApi(phoenixPaymentReqApi, payReq);
		}else if(CompanyId.equals("100046")) {//zambia
			response = thread.callZambiaComApi(phoenixPaymentReqApi, payReq);
		}else if(CompanyId.equals("100047")) {//boatswana
			response = thread.callNamibiaComApi(phoenixPaymentReqApi, payReq);
		}else if(CompanyId.equals("100048")) {//mozambique
			response = thread.callNamibiaComApi(phoenixPaymentReqApi, payReq);
		}else if(CompanyId.equals("100050")) {//namibia
			response = thread.callNamibiaComApi(phoenixPaymentReqApi, payReq);
		}
		//response = thread.callNamibiaComApi(phoenixPaymentReqApi, payReq);
		
		log.info("PhoenixPaymentReqApiRes" +response);
		
		Map<String, Object> selecome=null;
		try {
			selecome = mapper.readValue(response, Map.class);
		} catch (Exception e1) {
			e1.printStackTrace();
			exception=e1.getMessage();
		} 
		
        String url ="";
		
		if(CompanyId.equals("100049")) {//swaziland
			 url = thirdPartySwazilandPaymentCheckApi+quoteNo;
		}else if(CompanyId.equals("100046")) {//zambia
			 url = thirdPartyZambiaPaymentCheckApi+quoteNo;
		}else if(CompanyId.equals("100047")) {//boatswana
			 url = thirdPartyBoatswanaPaymentCheckApi+quoteNo;
		}else if(CompanyId.equals("100048")) {//mozambique
			 url = thirdPartyMozambiquePaymentCheckApi+quoteNo;
		}else if(CompanyId.equals("100050")) {//namibia
			 url = thirdPartyNamibiaPaymentCheckApi+quoteNo;
		}
		
		String message = "",payment_result="";
		if(selecome != null) {
			 payment_result =selecome.get("result")==null?"":selecome.get("result").toString();
		}
		
		if(payment_result.equalsIgnoreCase("SUCCESS")) {
			List<Map<String,Object>> data =selecome.get("data")==null?null:(List<Map<String, Object>>) selecome.get("data");
			 message = data.get(0).get("payment_gateway_url").toString();
			

		/*	StringBuilder sb = new StringBuilder("*Payment has been initiated.Please check your mobile notification*\n\n");
			//sb.append("Payment Reference No : "+paymnet_ref_no);
			//sb.append("\n\n");
			//sb.append("Payment Transaction No : "+paymnet_tran_no);
			//sb.append("\n\n");
			sb.append("Payment Initiated Status : "+payment_result);
			sb.append("\n\n");
			sb.append("Message : *"+message.trim()+"*");
			sb.append("\n\n");
			sb.append("Quotation No  : "+quoteNo);
			sb.append("\n\n");
			
			String str = new String (sb);
			
			String msgs = "";
			try {
				 msgs = URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
		
					
			log.info("PaymentCheck status API || " +url);
					
			OkHttpClient okhttp = new OkHttpClient.Builder()
					.readTimeout(30, TimeUnit.SECONDS)
					.build();
					
			String wattiUrl =whatsappApi+whatsappApiSendSessionMessage;
					
			wattiUrl = wattiUrl.replace("{whatsappNumber}", whatsappCode+whatsappNo);
			wattiUrl = wattiUrl.replace("{pageSize}", "");
			wattiUrl = wattiUrl.replace("{pageNumber}", "");
			wattiUrl = wattiUrl.replace("{messageText}", msgs);
			wattiUrl = wattiUrl.trim();
					
					
			RequestBody body = RequestBody.create(new byte[0], null);
		
			Request wattiRequest = new Request.Builder()
					.url(wattiUrl)
					.addHeader("Authorization",this.whatsappAuth )
					.post(body)
					.build();
	
			try {
				okhttp.newCall(wattiRequest).execute().close();;
				
				}catch (Exception e) {
					e.printStackTrace();
			} */
		
		}
		
		
	 //   url = thirdPartyPaymentCheckApi+quoteNo;
		
	/*	log.info("PaymentCheck status API || " +url);
		
		OkHttpClient okhttp = new OkHttpClient.Builder()
				.readTimeout(30, TimeUnit.SECONDS)
				.build();
		
		String wattiUrl =whatsappApi+whatsappApiSendSessionMessage;
		
		wattiUrl = wattiUrl.replace("{whatsappNumber}", "919159339730");
		wattiUrl = wattiUrl.replace("{pageSize}", "");
		wattiUrl = wattiUrl.replace("{pageNumber}", "");
		wattiUrl = wattiUrl.replace("{messageText}", "*Payment has been initiated.Please check your mobile notification*");
		wattiUrl = wattiUrl.trim();
		
		RequestBody body = RequestBody.create(new byte[0], null);
		
		Request wattiRequest = new Request.Builder()
				.url(wattiUrl)
				.addHeader("Authorization",this.whatsappAuth )
				.post(body)
				.build();

		try {
			okhttp.newCall(wattiRequest).execute().close();;
			
			}catch (Exception e) {
				e.printStackTrace();
		} */
		
		
	/*	Map<String,String> documentInfoMap = new HashMap<String,String>();
		documentInfoMap.put("whatsappCode", whatsappCode);
		documentInfoMap.put("whatsappNo", whatsappNo);
		documentInfoMap.put("motorPolicyDocumentApi", motorPolicyDocumentApi);
		documentInfoMap.put("whatsappApiButton", whatsappApiButton);
		documentInfoMap.put("whatsappAuth", this.whatsappAuth);
		documentInfoMap.put("quoteNo", quoteNo);
		documentInfoMap.put("tiraPostApi", tiraPostApi);
		
		PhoenixThreadUserCreation user_Creation = new PhoenixThreadUserCreation(request, url, threadprocess,
				"PAYMENT_TRIGGER", merchantRefNo, documentInfoMap) ;
		user_Creation.setName("PAYMENT_TRIGGER");
		user_Creation.start();
		
		if(CompanyId.equals("100049")) {//swaziland
			redirectUrl = swazilandRedirectUrl;
		}else if(CompanyId.equals("100046")) {//zambia
			redirectUrl = zambiaRedirectUrl;
		}else if(CompanyId.equals("100047")) {//boatswana
			redirectUrl = boatswanaRedirectUrl;
		}else if(CompanyId.equals("100048")) {//mozambique
			redirectUrl = mozambiqueRedirectUrl;
		}else if(CompanyId.equals("100050")) {//namibia
			redirectUrl = namibiaRedirectUrl;
		} */
		redirectUrl = message;
		return redirectUrl;
	}

}
