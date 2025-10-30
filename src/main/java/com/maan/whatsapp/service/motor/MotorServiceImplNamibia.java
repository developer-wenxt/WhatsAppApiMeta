package com.maan.whatsapp.service.motor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.maan.whatsapp.claimintimation.ClaimIntimationRepository;
import com.maan.whatsapp.claimintimation.InalipaIntimatedTableRepository;
import com.maan.whatsapp.entity.master.PreinspectionDataDetail;
import com.maan.whatsapp.entity.master.QWhatsappTemplateMaster;
import com.maan.whatsapp.entity.master.QWhatsappTemplateMasterPK;
import com.maan.whatsapp.entity.master.WhatsappClaimDocumentSetup;
import com.maan.whatsapp.entity.master.WhatsappTemplateMaster;
import com.maan.whatsapp.entity.master.WhatsappTemplateMasterPK;
import com.maan.whatsapp.entity.whatsapp.PhoenixUserDataDetails;
import com.maan.whatsapp.entity.whatsapp.QWhatsappRequestDetail;
import com.maan.whatsapp.entity.whatsapp.QWhatsappRequestDetailPK;
import com.maan.whatsapp.entity.whatsapp.WhatsappRequestDetail;
import com.maan.whatsapp.entity.whatsapp.WhatsappRequestDetailPK;
import com.maan.whatsapp.repository.whatsapp.PhoenixUserDataDetailsRepo;
import com.maan.whatsapp.repository.whatsapp.PreInspectionDataDetailRepo;
import com.maan.whatsapp.repository.whatsapp.WADataDetailRepo;
import com.maan.whatsapp.repository.whatsapp.WhatsappClaimDocumentRepo;
import com.maan.whatsapp.repository.whatsapp.WhatsappContactDataRepo;
import com.maan.whatsapp.repository.whatsapp.WhatsappRequestDetailRepo;
import com.maan.whatsapp.request.motor.ClaimDocReq;
import com.maan.whatsapp.request.motor.ClaimDocumentReq;
import com.maan.whatsapp.request.motor.DocInsertReq;
import com.maan.whatsapp.request.motor.DocumentJsonFormatReq;
import com.maan.whatsapp.request.whatsapp.WAWatiReq;
import com.maan.whatsapp.response.error.Error;
import com.maan.whatsapp.response.error.Errors;
import com.maan.whatsapp.service.common.CommonService;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class MotorServiceImplNamibia implements MotorServiceNamibia{
	
	@Autowired
	private WADataDetailRepo wddRepo;
	@Autowired
	private WhatsappRequestDetailRepo detailRepo;

	@Autowired
	private WhatsappContactDataRepo contactRepo;
	
	@Autowired
	private CommonService cs;

	@Autowired
	private JPAQueryFactory jpa;

	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private WhatsappClaimDocumentRepo claimRepo;
	
	@Autowired
	private PreInspectionDataDetailRepo preInsDataRepo;
	
	@Autowired
	private ClaimIntimationRepository  intimationRepository;
	
	@Autowired
	private InalipaIntimatedTableRepository inalipaintiRepo;
	
	@Autowired
	private PhoenixUserDataDetailsRepo userDetailsRepo;
	
	private Logger log = LogManager.getLogger(getClass());
	
	String claimWhatsappToken="";

	@Override
	public String callMotorApi(WhatsappRequestDetail detail, String waid) {

		try {

			String response = "";

			WhatsappTemplateMaster waTempM = getWATempM(detail);

			List<WhatsappRequestDetail> reqDetList = getUserReqDet(waTempM, waid);

			if (reqDetList.size() > 0) {

				String auth = waTempM.getApiauth();
				String url = "";
				String method = waTempM.getApimethod();
				
				String isResSaveApi = StringUtils.isBlank(waTempM.getIsResSaveApi()) ? "" : waTempM.getIsResSaveApi();
				
				String isResMsg =StringUtils.isBlank(detail.getIsResMsg()) ? "N" : detail.getIsResMsg();
				
				url ="Y".equalsIgnoreCase(isResMsg)?detail.getIsResMsgApi():waTempM.getApiurl();
				
				String respString = StringUtils.isBlank(waTempM.getResponsestring()) ? "" : waTempM.getResponsestring();
				String errResString = StringUtils.isBlank(waTempM.getErrorrespstring()) ? ""
						: waTempM.getErrorrespstring();
				String reqString = StringUtils.isBlank(waTempM.getRequeststring()) ? "" : waTempM.getRequeststring();
			
				String msgEn = StringUtils.isBlank(waTempM.getMessage_content_en()) ? ""
						: waTempM.getMessage_content_en();
				String regardsEn = StringUtils.isBlank(waTempM.getMessage_regards_en()) ? ""
						: waTempM.getMessage_regards_en();

				String msgAr = StringUtils.isBlank(waTempM.getMessage_content_ar()) ? ""
						: waTempM.getMessage_content_ar().trim();
				String regardsAr = StringUtils.isBlank(waTempM.getMessage_regards_ar()) ? ""
						: waTempM.getMessage_regards_ar().trim();

				String isCtaDynamicYn =StringUtils.isBlank(waTempM.getIsCtaDynamicYn())?"N":waTempM.getIsCtaDynamicYn();
				String ctaButtonUrl =StringUtils.isBlank(waTempM.getCtaButtonUrl())?"":waTempM.getCtaButtonUrl();
				String ctaButtonkeys =StringUtils.isBlank(waTempM.getCtaButtonKeys())?"":waTempM.getCtaButtonKeys();
			
				String isValidationApi = StringUtils.isBlank(detail.getIsvalidationapi()) ? "N" : detail.getIsvalidationapi();
				
				String resStringAr =StringUtils.isBlank(waTempM.getResponseStringAr())?"":waTempM.getResponseStringAr();
				
				//String errorResStrTZS =StringUtils.isBlank(waTempM.getErrorResponseStrTzs())?"":waTempM.getErrorResponseStrTzs();
				
				String interactive_button_yn=StringUtils.isBlank(waTempM.getInteractiveButtonYn())?"N":waTempM.getInteractiveButtonYn();
		
				String language =contactRepo.getLanguage(waid.toString());
				
				String msg ="English".equalsIgnoreCase(language)?msgEn:msgAr;
				
				
				String button1 ="",button2="",button3="",flow_id="",flow_token="",flowRequestDataYn ="",
								flow_api="",flow_api_auth="",flow_api_method ="",flow_button_name="",cta_button_name="",
								location_button_name="",menu_button_name="",message_type="";
				
				if("Y".equalsIgnoreCase(interactive_button_yn)) {
					message_type =waTempM.getMessageType();
					if("FLOW".equalsIgnoreCase(message_type)) {
						flow_token =StringUtils.isBlank(waTempM.getFlowToken())?"":waTempM.getFlowToken();
						flowRequestDataYn =StringUtils.isBlank(waTempM.getRequestdataYn())?"N":waTempM.getRequestdataYn();
						flow_api =StringUtils.isBlank(waTempM.getFlowApi())?"":waTempM.getFlowApi();
						flow_api_auth =StringUtils.isBlank(waTempM.getFlowApiAuth())?"":waTempM.getFlowApiAuth();
						flow_api_method =StringUtils.isBlank(waTempM.getFlowApiMethod())?"":waTempM.getFlowApiMethod();
						flow_id =StringUtils.isBlank(waTempM.getFlowId())?"":waTempM.getFlowId();
					}
					
					if("English".equalsIgnoreCase(language)) {
						button1 =StringUtils.isBlank(waTempM.getButton_1())?"":waTempM.getButton_1();
						button2 =StringUtils.isBlank(waTempM.getButton_2())?"":waTempM.getButton_2();
						button3 =StringUtils.isBlank(waTempM.getButton_3())?"":waTempM.getButton_3();
						flow_button_name =StringUtils.isBlank(waTempM.getFlowButtonName())?"":waTempM.getFlowButtonName();
						cta_button_name =StringUtils.isBlank(waTempM.getCtaButtonName())?"":waTempM.getCtaButtonName();
						location_button_name =StringUtils.isBlank(waTempM.getLocButtonName())?"":waTempM.getLocButtonName();
						menu_button_name=StringUtils.isBlank(waTempM.getMenu_button_name())?"":waTempM.getMenu_button_name();
					}else if("Swahili".equalsIgnoreCase(language)) {
						button1 =StringUtils.isBlank(waTempM.getButton_1_sw())?"":waTempM.getButton_1_sw();
						button2 =StringUtils.isBlank(waTempM.getButton_2_sw())?"":waTempM.getButton_2_sw();
						button3 =StringUtils.isBlank(waTempM.getButton_3_sw())?"":waTempM.getButton_3_sw();
						flow_button_name =StringUtils.isBlank(waTempM.getFlowButtonNameSw())?"":waTempM.getFlowButtonNameSw();
						cta_button_name =StringUtils.isBlank(waTempM.getCtaButtonNameSw())?"":waTempM.getCtaButtonNameSw();
						location_button_name =StringUtils.isBlank(waTempM.getLocButtonNameSw())?"":waTempM.getLocButtonNameSw();
						menu_button_name=StringUtils.isBlank(waTempM.getMenu_button_name_sw())?"":waTempM.getMenu_button_name_sw();

					}						
				}
				


					
				String request = "";
                   log.info("reqString ==>"+reqString +"reqDetList"+reqDetList);
				//if(isValidationApi.equalsIgnoreCase("N")) {
                   if("WhatsAppRequest".equalsIgnoreCase(reqString)) {
                	   Map<String,Object> req = new HashMap<>();
                	   req.put("WhatsAppCode", waid);
                	   req.put("WhatsAppNo",waid);
                	   
                	   request = objectMapper.writeValueAsString(req);
                   }else {
                	   request = setApiRequest(reqDetList, reqString);
   					   request = setApiRequest(waid, request);
   					   System.out.println("Before Adding :"+ request);
   					Map<String, Object> map = objectMapper.readValue(request, Map.class);
   				//	Map<String,Object> req = new HashMap<>();
   					map.put("whatsappBotNum", waid);
   				    request = objectMapper.writeValueAsString(map);
   				 System.out.println("After Adding :"+ request);
                   }
					
				/*} else {
					List<WhatsappRequestDetail> detailList = new ArrayList<WhatsappRequestDetail>();
                             
					detailList.add(detail);

					request = setApiRequest(detailList, reqString);
				}*/
	            log.info("reqString ==>"+reqString +"\nreqDetList"+reqDetList+"\nrequest"+request);

				detail.setRequeststring(request);

				Map<String, Object> map = cs.callApi(url, auth, method, request);

				String code = map.get("Code") == null ? "" : String.valueOf(map.get("Code"));
				

				if ((code.equals("200")||code.equals("201")) && isValidationApi.equalsIgnoreCase("N")) {

					String apiResp = map.get("Response") == null ? "" : String.valueOf(map.get("Response"));
					
					if("English".equalsIgnoreCase(language)) {
						
						apiResp =setApiResponse(respString, apiResp);
						
						apiResp = msgEn + "\n" + apiResp + "\n" + regardsEn; 
						
					}else if("Swahili".equalsIgnoreCase(language)) {
						
						apiResp =setApiResponse(resStringAr, apiResp);
						
						apiResp = msgAr + "\n" + apiResp + "\n" + regardsAr; 
					}
					
					WAWatiReq waRequest = WAWatiReq.builder()
							.filepath("")
							.msg(apiResp)
							.waid(String.valueOf(waid))
							.button_1(button1) 
							.button_2(button2) 
							.button_3(button3) 
							.messageId(waTempM.getRemarks())
							.flow_button_name(flow_button_name)
							.flowApi(flow_api)
							.flowId(flow_id)
							.flowToken(flow_token)
							.flowApiAuth(flow_api_auth)
							.flowApiMethod(flow_api_method)
							.flow_requestdata_yn(flowRequestDataYn)
							.cta_button_name(cta_button_name)
							.location_button_name(location_button_name)
							.messageType(message_type)
							.menu_button_name(menu_button_name)
							.interactiveYn(interactive_button_yn)
							.apiData(map.get("Response") == null ? "N" : String.valueOf(map.get("Response")))
							.isCtaDynamicYn(isCtaDynamicYn)
							.ctaButtonUrl(ctaButtonUrl)
							.ctaButtonKeys(ctaButtonkeys)
							.build();
						
					response = cs.reqPrint(waRequest);
				
				} else if (code.equals("403")) {

					String apiResp = map.get("Response") == null ? "" : String.valueOf(map.get("Response"));

					if(StringUtils.isNotBlank(apiResp)) {
						Errors pojo = objectMapper.readValue(apiResp.toString(), Errors.class);
						
						if("English".equalsIgnoreCase(language))
							response = setErrApiResponse(errResString, pojo);
						else if("Swahili".equalsIgnoreCase(language))
							response = setErrApiResponse(errResString, pojo);	
						
						
						WAWatiReq waRequest = WAWatiReq.builder()
								.filepath("")
								.msg(response)
								.waid(String.valueOf(waid))
								.button_1(button1) 
								.button_2(button2) 
								.button_3(button3) 
								.messageId(waTempM.getRemarks())
								.flow_button_name(flow_button_name)
								.flowApi(flow_api)
								.flowId(flow_id)
								.flowToken(flow_token)
								.flowApiAuth(flow_api_auth)
								.flowApiMethod(flow_api_method)
								.flow_requestdata_yn(flowRequestDataYn)
								.cta_button_name(cta_button_name)
								.location_button_name(location_button_name)
								.messageType(message_type)
								.menu_button_name(menu_button_name)
								.interactiveYn(interactive_button_yn)
								.apiData("N")
								.isCtaDynamicYn(isCtaDynamicYn)
								.ctaButtonUrl(ctaButtonUrl)
								.ctaButtonKeys(ctaButtonkeys)
								.build();
						
						response = cs.reqPrint(waRequest);
						
					}
					
				}else if("Y".equalsIgnoreCase(isResSaveApi) || "Y".equalsIgnoreCase(isResMsg)) {
					
					response = map.get("Response") == null ? "" : String.valueOf(map.get("Response"));

				}
			}

			return response;
		} catch (Exception e) {
			log.error(e);
		}
		return "";
	
	}
	
	private WhatsappTemplateMaster getWATempM(WhatsappRequestDetail detail) {
		try {

			QWhatsappTemplateMaster qtempM = QWhatsappTemplateMaster.whatsappTemplateMaster;
			QWhatsappTemplateMasterPK qtempMPk = qtempM.tempMasterPk;
			
			WhatsappRequestDetailPK reqDetPk = detail.getReqDetPk();

			WhatsappTemplateMaster tempM = jpa.selectFrom(qtempM)
				.where(qtempMPk.agencycode.eq("90016")
						.and(qtempMPk.stagecode.eq(reqDetPk.getCurrentstage()))
						.and(qtempMPk.stagesubcode.eq(reqDetPk.getCurrentsubstage()))
						.and(qtempMPk.productid.eq(reqDetPk.getProductid()))
						.and(qtempM.remarks.eq(detail.getRemarks()))
						.and(qtempM.status.equalsIgnoreCase("Y")))
				.fetchFirst();

			return tempM;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}
	
	private List<WhatsappRequestDetail> getUserReqDet(WhatsappTemplateMaster tempM, String waid) {
		try {

			QWhatsappRequestDetail qreqDet = QWhatsappRequestDetail.whatsappRequestDetail;
			QWhatsappRequestDetailPK qreqDetPk = qreqDet.reqDetPk;
			
			WhatsappTemplateMasterPK tempMPk = tempM.getTempMasterPk();
			
			List<WhatsappRequestDetail> fetch = jpa.selectFrom(qreqDet)
					.where(qreqDet.whatsappid.eq(Long.valueOf(waid))
							.and(qreqDet.status.equalsIgnoreCase("Y"))
							.and(qreqDet.issent.equalsIgnoreCase("Y"))
							.and(qreqDet.isjobyn.equalsIgnoreCase("Y"))
							.and(qreqDet.isreplyyn.equalsIgnoreCase("Y"))
							//.and(qreqDetPk.currentstage.eq(tempMPk.getStagecode()))
							//.and(qreqDetPk.currentsubstage.eq(tempMPk.getStagesubcode()))
							.and(qreqDetPk.productid.eq(tempMPk.getProductid()))
							.and(qreqDet.remarks.eq(tempM.getRemarks()))
							.and(qreqDet.userreply.isNotNull())
							.and(qreqDet.requestkey.isNotNull()))
					.orderBy(qreqDet.stage_order.desc())
					.fetch();


			return fetch;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}
	
	private String setApiRequest(List<WhatsappRequestDetail> detList, String reqString) {
		try {

			String response = reqString;

			Map<String, Object> map = detList.stream().collect(
					Collectors.toMap(WhatsappRequestDetail::getRequestkey, WhatsappRequestDetail::getUserreply));

			for (Map.Entry<String, Object> entry : map.entrySet()) {
				if (response.contains(entry.getKey().toString())) {
					response = response.replace("{" + entry.getKey().toString() + "}",
							entry.getValue() == null ? "" : entry.getValue().toString().replaceAll("\n", ""));
				}
			}

			if (detList.size() > 0) {
				WhatsappRequestDetail reqDet = detList.get(0);

				WhatsappRequestDetailPK reqDetPk = reqDet.getReqDetPk();

				response = response.replace("{MobileNumber}", String.valueOf(reqDetPk.getMobileno()));
				response = response.replace("{mobileNo}", reqDetPk.getWhatsappcode().toString()+reqDetPk.getMobileno().toString());
				response = response.replace("{MobileCode}", String.valueOf(reqDetPk.getWhatsappcode()));
				response = response.replace("{WhatsAppNo}", String.valueOf(reqDetPk.getMobileno()));
				response = response.replace("{WhatsAppCode}", String.valueOf(reqDetPk.getWhatsappcode()));
				response = response.replace("{ProductId}", String.valueOf(reqDetPk.getProductid()));

			}

			return response;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}
	
	private String setApiRequest(String waid, String apiRequest) {
		try {

			String response = apiRequest;
			
			List<Map<String, Object>> list = wddRepo.getChatInputData(waid);

			Predicate<Map<String, Object>> pred = pwdd -> true;
			
			List<Map<String, Object>> mapList = list.stream()
					.filter(pred.and(i -> i.get("ISINPUT").toString().equalsIgnoreCase("Y"))
							.and(i -> (i.get("REQUEST_KEY") != null)))
					.collect(Collectors.toList());
			
			Map<String, Object> map = new HashMap<String, Object>();
			
			for (Map<String, Object> fmap : mapList) {

				String next = fmap.get("REQUEST_KEY").toString();

				if (!map.containsKey(next)) {
					map.put(fmap.get("REQUEST_KEY").toString(), fmap.get("INPUT_VALUE"));
				}
			}

			for (Map.Entry<String, Object> entry : map.entrySet()) {
				if (response.contains(entry.getKey().toString())) {
					response = response.replace("{" + entry.getKey().toString() + "}",
							entry.getValue() == null ? "" : entry.getValue().toString().replaceAll("\n", ""));
				}
			}

			return response;
		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}
	
	private String setApiResponse(String respString, String apiRes) {
		try {

			String response = respString;

			/*Map<String, Object> map = objectMapper.convertValue(pojo, 
					new TypeReference<Map<String, Object>>() {});*/
			
			Map<String, Object> map = objectMapper.readValue(apiRes, Map.class);

			for (Map.Entry<String, Object> entry : map.entrySet()) {
				if (response.contains(entry.getKey().toString())) {
					response = response.replace("{" + entry.getKey().toString() + "}",
							entry.getValue() == null ? "" : entry.getValue().toString());
				}
			}

			return response;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	private String setErrApiResponse(String respString, Errors pojo) {
		try {

			String response = "";
			String tempRes ="";
			String res ="";
			List<Error> errors = pojo.getErrors();

			if (errors.size() > 0) {

				for (Error err : errors) {

					tempRes = respString;
					
					String field = err.getField().trim();
					String msg = err.getMessage().trim();

					if (tempRes.contains(field)) {
						if(field.contains("Arabic")) {
							res =res.replace("{" + field + "}", msg);
						}else if (tempRes.contains(field)) {
							tempRes =tempRes.replace("{" + field + "}", msg);
							res =tempRes;
						}
					}
				}
					/*Map<String, Object> map = objectMapper.convertValue(err, new TypeReference<Map<String, Object>>() {
					});

					for (Map.Entry<String, Object> entry : map.entrySet()) {
						if (tempRes.contains(entry.getKey().toString())) {
							tempRes = tempRes.replace("{" + entry.getKey().toString() + "}",
									entry.getValue() == null ? "" : entry.getValue().toString());
						}
					}*/

					response = res + "\\n" + response;
				}
			

			return response.trim();
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	@Override
	public DocInsertReq getFilePath(DocInsertReq request) {

		try {

			String fileName = request.getFileName();
			String encodedString = request.getBase64File();

			byte[] arr = Base64.getDecoder().decode(encodedString);

			String docPath = cs.getwebserviceurlProperty().getProperty("common.file.path");

			String filePath = docPath + fileName;

			File file = new File(filePath);

			log.info("getFilePath--> filePath: " + filePath);

			FileUtils.writeByteArrayToFile(file, arr);
			
			DocInsertReq response = DocInsertReq.builder()
					.filePath(filePath)
					.build();

			return response;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	@Override
	public String getTreeStructMsg(WhatsappRequestDetail detail) {

		String msg = "";
		try {

			msg = detail.getMessage();

			QWhatsappRequestDetail qDet = QWhatsappRequestDetail.whatsappRequestDetail;

			BooleanExpression and = qDet.whatsappid.eq(detail.getWhatsappid())
					.and(qDet.remarks.eq(detail.getRemarks()))
					.and(qDet.status.equalsIgnoreCase("Y"))
					.and(qDet.isreplyyn.equalsIgnoreCase("Y"))
					.and(qDet.isdocuplyn.equalsIgnoreCase("N"))
					.and(qDet.userreply.isNotNull())
					.and(qDet.requestkey.isNotNull())
					.and(qDet.stage_order.lt(detail.getStage_order()));

			List<WhatsappRequestDetail> list = (List<WhatsappRequestDetail>) detailRepo.findAll(and,
					qDet.stage_order.asc());

			if (list.size() > 0) {
				Map<String, Object> map = list.stream().collect(
						Collectors.toMap(WhatsappRequestDetail::getRequestkey, WhatsappRequestDetail::getUserreply));

				for (Map.Entry<String, Object> entry : map.entrySet()) {
					if (msg.contains(entry.getKey().toString())) {
						msg = msg.replace("{" + entry.getKey().toString() + "}",
								entry.getValue() == null ? "" : entry.getValue().toString());
					}
				}
			}

			return msg;
		} catch (Exception e) {
			log.error(e);
		}
		return msg;
	
	}
	
	/*public String getAuthendicationTokenForClaim() {
	ClaimLoginRequest req = new ClaimLoginRequest();
	String token ="";
	try {
		req.setBranchCode("07");
		req.setInsuranceId("100002");
		req.setPassword("Admin@01");
		req.setRegionCode("08");
		req.setUserId("guest");
		req.setWhatsappYN("Y");
		req.setLoginType("user");
		String claimTokenUrl = cs.getwebserviceurlProperty().getProperty("claim.whatsapp.login.url");
		log.info("===============================================");
		cs.reqPrint(req);
	Map<String, Object> map = cs.callApi(claimTokenUrl, "", "POST", req);
	log.info("ClaimToken Response=====>"+map.get("Response"));
	if(map.size()>0) {
		String apiResp = map.get("Response") == null ? "" : String.valueOf(map.get("Response"));
		ClaimLoginRes loginRes =objectMapper.readValue(apiResp.toString(), ClaimLoginRes.class);
		claimWhatsappToken ="Bearer " +loginRes.getToken();
		token ="Bearer " +loginRes.getToken();
		log.info("Claim Token=====>"+claimWhatsappToken);
	}
	}catch (Exception e) {
	e.printStackTrace();	
	
	}
	return token;
}*/


public boolean saveClaimDocument(String document,WhatsappTemplateMaster reqDet, String type, Long waid, WhatsappRequestDetail reqDet2) {
 Gson print =new Gson();
	try {
		String requestStr="";
		log.info("Claimdocument--> document: " + document);
		
		List<WhatsappRequestDetail> reqDetList = getUserReqDetDocument(reqDet, waid.toString());
		
		requestStr = setApiRequest(reqDetList, reqDet.getRequeststring());
					
		String image_url = cs.getwebserviceurlProperty().getProperty("meta.get.image.api");
		
		String auth = cs.getwebserviceurlProperty().getProperty("meta.message.api.auth");



		OkHttpClient okhttp = new OkHttpClient.Builder()
					.readTimeout(30, TimeUnit.SECONDS)
					.build();
			
		image_url = image_url.replace("{IMAGE_ID}", document);
		image_url = image_url.trim();
	
		Request request = new Request.Builder()
				.url(image_url)
				.addHeader("Authorization", auth)
				.get()
				.build();

		Response response = okhttp.newCall(request)
				.execute();

	
		
		
		String imageRes =response.body().string();
		
		Map<String,Object> meta_image =objectMapper.readValue(imageRes, Map.class);
					
		String image =meta_image.get("url")==null?"":meta_image.get("url").toString();
		String mime_type =meta_image.get("mime_type")==null?"":meta_image.get("mime_type").toString();

		URL url = new URL(image);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", auth);
        
		InputStream is = connection.getInputStream();
		
		byte [] byte_array =IOUtils.toByteArray(is);
		
		String base_64=Base64Utils.encodeToString(byte_array);
		
		//String file_path =cs.getwebserviceurlProperty().getProperty("meta.image.store.path");
		//file_path =file_path+"_"+System.currentTimeMillis();
		//File file = new File(file_path);

		//FileUtils.copyToFile(is, file);
		
		
		Map<String,Object> mapReq =objectMapper.readValue(requestStr, Map.class);
		LinkedHashMap<String,Object> apiReq=new LinkedHashMap<String,Object>();
		LinkedHashMap<String,Object> listMap =new LinkedHashMap<String,Object>();
		List<LinkedHashMap<String, Object>> list =new ArrayList<LinkedHashMap<String, Object>>();
		/*listMap.put("DocTypeId", mapReq.get("DocTypeId")==null?"":mapReq.get("DocTypeId"));
		listMap.put("ProductId", mapReq.get("ProductId")==null?"":mapReq.get("ProductId"));
		listMap.put("Delete", "Y");
		listMap.put("Description", mapReq.get("Description")==null?"":mapReq.get("Description"));
		listMap.put("FileName", mapReq.get("FileName")==null?"":mapReq.get("FileName"));
		listMap.put("UploadType", mapReq.get("UploadType")==null?"":mapReq.get("UploadType"));
		listMap.put("DocId", mapReq.get("DocId")==null?"":mapReq.get("DocId"));
		listMap.put("InsId", mapReq.get("InsId")==null?"":mapReq.get("InsId"));
		listMap.put("CreatedBy", waid);
		listMap.put("LossId", "16");
		listMap.put("PartyNo", "1");*/
	//	listMap.put("companyId", mapReq.get("companyId")==null?"":mapReq.get("companyId"));
	//	listMap.put("fileType", "image/jpeg");
		
	//	list.add(listMap);
		
	//	apiReq.put("DocumentUploadDetails", list);
		apiReq.put("base64", base_64);
		apiReq.put("companyId", mapReq.get("companyId")==null?"":mapReq.get("companyId"));
		apiReq.put("fileType", "image/jpeg");
	//	String claimRefNo =mapReq.get("claim_number")==null?"":mapReq.get("claim_number").toString();
	/*	Boolean encode_status = isBase64Encoded(claimRefNo);
		
		if(encode_status) {
			claimRefNo =new String(Base64.getDecoder().decode(claimRefNo));
			Map<String,Object> map =objectMapper.readValue(claimRefNo, Map.class);
			claimRefNo = map.get("claim_no").toString();
		}
		
		apiReq.put("ClaimNo", claimRefNo);
		apiReq.put("ProductId", mapReq.get("ProductId")==null?"":mapReq.get("ProductId")); */
		String req =print.toJson(apiReq);
		log.info("Upload Image Request "+req);
		Map<String,Object> map = cs.callApi(reqDet.getApiurl(), "", reqDet.getApimethod(), req);
		log.info("Upload Image Response "+print.toJson(map));
		//Map<String,Object> resp = map.get("Response") == null ? null : (Map<String, Object>) map.get("Response");
		PhoenixUserDataDetails saveApiDetails = new PhoenixUserDataDetails();
		saveApiDetails.setWaid(waid);
		saveApiDetails.setWamessageid(document);
		saveApiDetails.setUserReply("Upload Document");
		saveApiDetails.setEntryDate(new Date());
		saveApiDetails.setParentMessageId(null);
		saveApiDetails.setUserMessageId(document);
		saveApiDetails.setStatus("Y");
		saveApiDetails.setFlowRequest(print.toJson(map));
		saveApiDetails.setCompanyId(mapReq.get("companyId")==null?"":(String) mapReq.get("companyId"));
		
		userDetailsRepo.saveAndFlush(saveApiDetails);
		
		
		
	}catch (Exception e) {
		e.printStackTrace();
	}
	return false;
	
}


private DocumentJsonFormatReq setFileUploadRequest(String requestStr, String name, String concat, String imgurlen) {
	DocumentJsonFormatReq Jsonreq = new DocumentJsonFormatReq();
	try {
		String req =requestStr.replace("DocId", "DocTypeId").replace("Losstypeid", "LossId").replace("Partyno", "PartyNo").replace("Claimrefno", "ClaimNo");
		DocumentJsonFormatReq jsonFormat = objectMapper.readValue(req.toString(), DocumentJsonFormatReq.class);
		Jsonreq.setClaimNo(jsonFormat.getClaimNo());
		Jsonreq.setLossId(jsonFormat.getLossId());
		Jsonreq.setPartyNo(jsonFormat.getPartyNo());
		Jsonreq.setDoctTypeId(jsonFormat.getDoctTypeId());
		Jsonreq.setDescription(jsonFormat.getDescription());
		Jsonreq.setInsuranceId("100002");
		
		Jsonreq.setFile(concat+imgurlen);
		Jsonreq.setFileName(name);
	}catch (Exception e) {
		e.printStackTrace();
	}
	return Jsonreq;
	
}


private void setFileUploadRes(Map<String, Object> map, WhatsappTemplateMaster reqDet, Long waid) {
	List<Error>list =new ArrayList<Error>();
	Errors errorMesg=null;
	try {
		String code = map.get("Code") == null ? "" : String.valueOf(map.get("Code"));
		String successRes= map.get("Response") == null ? "" : String.valueOf(map.get("Response"));
		ClaimDocumentReq res = objectMapper.readValue(successRes.toString(), ClaimDocumentReq.class);
		if(StringUtils.isBlank(res.getMessage())) {
			String errorRes =successRes.toString().replace("Message", "ErrorDescription").replace("Code", "ErrorCode");
			errorMesg = objectMapper.readValue(errorRes.toString(), Errors.class);
			list =errorMesg.getErrors();
		}
		if(code.equals("200")) {
			 String msgAr =reqDet.getMessage_content_ar();
			 String msgEn=reqDet.getMessage_content_en();
			 String regEn=	reqDet.getMessage_regards_en();
			 String regar=reqDet.getMessage_regards_ar();
			 String resen=	reqDet.getResponsestring();
			 String resAr=reqDet.getResponseStringAr();
			 String message = StringUtils.isBlank(msgAr) ? msgEn + "\\n" + resen + "\\n" + regEn:(msgAr + "\\n" + resAr + "\\n" + regar)+(msgEn + "\\n" + resen + "\\n" + regEn);
			 String mess="";
			 if(list.size()>0) {
				 String errMsg =list.get(0).getMessage();
				 mess =message.replace("{Message}", errMsg);
			 }else {
				 mess =message.replace("{Message}", res.getMessage());
			 }
			 sendResponseMsg(mess,waid.toString());
		}
	}catch (Exception e) {
		e.printStackTrace();
	}
}


private void sendResponseMsg(String res, String waid) {
	try {
		String commonurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api");
		String msgurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api.sendSessionMessage");
		String auth = cs.getwebserviceurlProperty().getProperty("whatsapp.auth");
		OkHttpClient okhttp = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();
			
		String replace = msgurl.replace("{whatsappNumber}",waid).replace("{messageText}", res);
		
		String url = commonurl + replace;

		RequestBody body = RequestBody.create(new byte[0], null);
		Request request = new Request.Builder()
				.url(url)
				.addHeader("Authorization", auth)
				.post(body)
				.build();

		 okhttp.newCall(request).execute();
			
	}catch (Exception e) {
		e.printStackTrace();
	}
	
}



/*
 * public WhatsappClaimDocumentSetup getClaimDocDetails(String reply, Long waid)
 * { WhatsappClaimDocumentSetup data = new WhatsappClaimDocumentSetup(); try {
 * data=docRepo.findByCheckedAndClaimPkMobNoAndDocIdAndExStatus("Y",
 * waid.toString(), reply,"N"); if(data!=null) { data.setUserReplay("Y");
 * docRepo.save(data); sendDocmentUploadMsgwaid(waid,data.getDocName()); }
 * }catch (Exception e) { e.printStackTrace(); } return data;
 * 
 * }
 */

public void sendDocmentUploadMsgwaid(Long waid, WhatsappTemplateMaster whatsappTemplateMaster) throws IOException {
	String commonurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api");
	String msgurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api.sendSessionMessage");
	String auth = cs.getwebserviceurlProperty().getProperty("whatsapp.auth");
	OkHttpClient okhttp = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();

	//WAMessageMaster wm=messageRepo.findByMessageidAndStatus("FILE_008", "Y");
	//String message=wm.getMessagedescen().replace("{UploadDocMsg}", whatsappTemplateMaster);
	
	String replace = msgurl.replace("{whatsappNumber}",waid.toString()).replace("{messageText}", whatsappTemplateMaster.getMessage_content_en());
	
	String url = commonurl + replace;

	RequestBody body = RequestBody.create(new byte[0], null);
	Request request = new Request.Builder()
			.url(url)
			.addHeader("Authorization", auth)
			.post(body)
			.build();

	 okhttp.newCall(request).execute();
		
}

private List<WhatsappRequestDetail> getUserReqDetDocument(WhatsappTemplateMaster tempM, String waid) {
	try {

		QWhatsappRequestDetail qreqDet = QWhatsappRequestDetail.whatsappRequestDetail;
		QWhatsappRequestDetailPK qreqDetPk = qreqDet.reqDetPk;
		
		WhatsappTemplateMasterPK tempMPk = tempM.getTempMasterPk();
		
		List<WhatsappRequestDetail> fetch = jpa.selectFrom(qreqDet)
				.where(qreqDet.whatsappid.eq(Long.valueOf(waid))
						.and(qreqDet.status.equalsIgnoreCase("Y"))
						.and(qreqDet.issent.equalsIgnoreCase("Y"))
						.and(qreqDet.isjobyn.equalsIgnoreCase("Y"))
						.and(qreqDet.isreplyyn.equalsIgnoreCase("Y"))
						//.and(qreqDetPk.currentstage.eq(tempMPk.getStagecode()))
						//.and(qreqDetPk.currentsubstage.eq(tempMPk.getStagesubcode()))
						.and(qreqDetPk.productid.eq(tempMPk.getProductid()))
						.and(qreqDet.remarks.eq(tempM.getRemarks()))
						.and(qreqDet.userreply.isNotNull())
						.and(qreqDet.requestkey.isNotNull()))
				.orderBy(qreqDet.stage_order.desc())
				.fetch();
		return fetch;
	} catch (Exception e) {
		log.error(e);
	}
	return null;
}


public void saveClaimDocumentApi(WhatsappClaimDocumentSetup doc, String data, String type) {
	
	Gson json = new Gson();
	try {
		String fileurls[] = data.split("fileName=");
		
		String file =fileurls[1] ;
		String commonurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api");
		
		String fileurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api.getMedia");

		String auth = cs.getwebserviceurlProperty().getProperty("whatsapp.auth");

		String url = commonurl + fileurl;

			OkHttpClient okhttp = new OkHttpClient.Builder()
					.readTimeout(30, TimeUnit.SECONDS)
					.build();
			
		url = url.replace("{data}", file);
		url = url.trim();
	
		Request request = new Request.Builder()
				.url(url)
				.addHeader("Authorization", auth)
				.get()
				.build();

		Response response = okhttp.newCall(request)
				.execute();

		InputStream is = response.body().byteStream();
		
		byte[] bytess = IOUtils.toByteArray(is);
		
		String extension = FilenameUtils.getExtension(file);

		String concat ="data:"+type+"/"+extension+";base64,";

		 
		String imgurlen=Base64Utils.encodeToString(bytess);
		
		ClaimDocReq r = ClaimDocReq.builder()
				.companyId("100002").partyId(doc.getPartyId()).lossId(doc.getLossId())
				.docName(doc.getDocName()).filename(doc.getDocName()).file(concat+imgurlen).docId(doc.getClaimPk().getDocId().toString())
				.claimNo(doc.getClaimNo())
				.build();
		log.info("ClaimDoc request=====>>"+json.toJson(r));

		//String authentication=getAuthendicationTokenForClaim();
		
		String uploadUrl=cs.getwebserviceurlProperty().getProperty("claim.whatsapp.document.url");
		
		//Map<String, Object> map = cs.callApi(uploadUrl,authentication , "POST",r);
		
		log.info("claim fileUploading response.........");
		//log.info(cs.reqPrint(map));
		
		//updateTransactionTable(map,doc);
		
	}catch (Exception e) {
		e.printStackTrace();
	}
	
}





private void sendWhatsappMsg(WhatsappClaimDocumentSetup claim,String ApiResponse,String botRes) {
	try {
		
		String commonurl=cs.getwebserviceurlProperty().getProperty("whatsapp.api");
		String msgurl=cs.getwebserviceurlProperty().getProperty("whatsapp.api.sendSessionMessage");
		if(StringUtils.isNotBlank(ApiResponse)) {
			msgurl=msgurl.replace("{whatsappNumber}", claim.getClaimPk().getMobNo().toString()).replace("{messageText}", claim.getDocName()+" "+ApiResponse).trim();
		}else if(StringUtils.isNotBlank(botRes)) {
			msgurl=msgurl.replace("{whatsappNumber}", claim.getClaimPk().getMobNo().toString()).replace("{messageText}", botRes).trim();

		}
		else {
			msgurl=msgurl.replace("{whatsappNumber}", claim.getClaimPk().getMobNo().toString()).replace("{messageText}", claim.getDocDesc()).trim();

		}
		String auth=cs.getwebserviceurlProperty().getProperty("whatsapp.auth");

		String sendMsgUrl =commonurl+msgurl;
		
		OkHttpClient okhttp = new OkHttpClient.Builder()
				.readTimeout(30, TimeUnit.SECONDS)
				.build();
		RequestBody body = RequestBody.create(new byte[0], null);

		Request request = new Request.Builder()
				.url(sendMsgUrl)
				.addHeader("Authorization", auth)
				.post(body)
				.build();

		Response res= okhttp.newCall(request).execute();
		log.info("Send Message Response===>"+res.body().toString());
		claim.setSentYn("Y");
		claimRepo.save(claim);

		
	}catch (Exception e) {
		e.printStackTrace();
	}
	
}

public String setSaveRequest(WhatsappRequestDetail detail, String waid) {
	Map<String, WhatsappRequestDetail> data =new HashMap<String,WhatsappRequestDetail>();
	String chassNo ="";String regNo ="";
	String tran_id ="";
	try {
		QWhatsappRequestDetail qreqDet = QWhatsappRequestDetail.whatsappRequestDetail;
		QWhatsappRequestDetailPK qreqDetPk = qreqDet.reqDetPk;
		List<WhatsappRequestDetail> reqDetList = jpa.selectFrom(qreqDet)
				.where(qreqDet.whatsappid.eq(Long.valueOf(waid))
						.and(qreqDet.status.equalsIgnoreCase("Y"))
						.and(qreqDet.isjobyn.equalsIgnoreCase("Y"))
						.and(qreqDet.isreplyyn.equalsIgnoreCase("Y"))
						.and(qreqDetPk.productid.eq(detail.getReqDetPk().getProductid()))
						.and(qreqDet.remarks.eq(detail.getRemarks()))
						.and(qreqDet.userreply.isNotNull())
						.and(qreqDet.requestkey.isNotNull()))
				.orderBy(qreqDet.stage_order.desc())
				.fetch();
		data = reqDetList.stream().collect(
				Collectors.toMap(WhatsappRequestDetail::getRequestkey, r ->r));
		
		WhatsappRequestDetail chassisNo=data.get("ChassisNo")==null?null:data.get("ChassisNo");
		WhatsappRequestDetail registrationNo=data.get("RegistrationNo")==null?null:data.get("RegistrationNo");
		if(chassisNo!=null) {
			chassNo =chassisNo.getUserreply();
		}else if (registrationNo!=null) {
			regNo =registrationNo.getUserreply();
		}
		PreinspectionDataDetail pidd =PreinspectionDataDetail.builder()
				.chassisNo(chassNo)
				.entry_date(new Date())
				.mobileNo(waid)
				.registrationNo(regNo)
				.status("Y")
				.tranId(preInsDataRepo.getTranId())
				.build();
		PreinspectionDataDetail pdd=preInsDataRepo.save(pidd);
		tran_id =pdd.getTranId().toString();
		for(Map.Entry<String, WhatsappRequestDetail> key :data.entrySet()) {
			if(!key.getKey().equals("ChassisNo") && !key.getKey().equals("RegistrationNo") ) {					
				String imageName = key.getValue().getStageDesc();
				String imagePath =key.getValue().getUserreply().equals("99")?"":key.getValue().getUserreply();
				File file =new File(key.getValue().getUserreply());
				preInsDataRepo.insertImageDetails(pdd.getTranId().toString(), imageName, imagePath,new Date(),"VALID",file.getName(),"N");				
			}
		}
	}catch (Exception e) {
		e.printStackTrace();
		log.error(e);
	}
	
	return tran_id;
}

public static boolean isBase64Encoded(String str) {
    try {
        Base64.getDecoder().decode(str);
        return true;
    } catch (IllegalArgumentException e) {
    	//e.printStackTrace();
        return false;
        
    }
}


}
