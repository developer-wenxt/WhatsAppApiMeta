package com.maan.whatsapp.service.wati;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.maan.whatsapp.entity.master.QWhatsappTemplateMaster;
import com.maan.whatsapp.entity.master.QWhatsappTemplateMasterPK;
import com.maan.whatsapp.entity.master.WhatsappMessageMenuMaster;
import com.maan.whatsapp.entity.master.WhatsappTemplateMaster;
import com.maan.whatsapp.entity.whatsapp.WhatsappRequestDetail;
import com.maan.whatsapp.repository.master.WhatsappMessageMenuMasterRepository;
import com.maan.whatsapp.repository.whatsapp.WhatsappContactDataRepo;
import com.maan.whatsapp.repository.whatsapp.WhatsappRequestDetailRepo;
import com.maan.whatsapp.request.whatsapp.ButtonHeaderReq;
import com.maan.whatsapp.request.whatsapp.WAWatiReq;
import com.maan.whatsapp.response.wati.sendsesfile.MessageFileRes;
import com.maan.whatsapp.response.wati.sendsesfile.SendSessionFile;
import com.maan.whatsapp.response.wati.sendsesmsg.SendMessageResponse;
import com.maan.whatsapp.service.common.CommonService;
import com.maan.whatsapp.service.motor.MotorService;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vdurmont.emoji.EmojiParser;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class WatiApiCall {

	@Autowired
	private CommonService cs;
	@Autowired
	private MotorService motSer;

	@Autowired
	private WhatsappRequestDetailRepo detailRepo;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private JPAQueryFactory jpa;
	
	private Logger log = LogManager.getLogger(getClass());
	
	@Autowired
	private WhatsappContactDataRepo contactDataRepo ;
	
	@Autowired
	private WhatsappMessageMenuMasterRepository wmmmRpo;
	
	private static MediaType contentType =MediaType.parse("application/json");;

	@SuppressWarnings("unchecked")
	@Async
	public CompletableFuture<String> sendMsg(OkHttpClient okhttp, RequestBody body, String commonurl, String msgurl,
			String fileurl, String auth, WhatsappRequestDetail detail, String waid, Date reqtime) {
		try {

			String fileyn = StringUtils.isBlank(detail.getFile_yn()) ? "" : detail.getFile_yn();
			String apiCall = StringUtils.isBlank(detail.getIsapicall()) ? "N" : detail.getIsapicall();
			String isValidationApi = StringUtils.isBlank(detail.getIsvalidationapi()) ? "N" : detail.getIsvalidationapi();
            String isbuttonMsg =StringUtils.isBlank(detail.getIsButtonMsg()) ? "N" : detail.getIsButtonMsg();
            String isResMsg =StringUtils.isBlank(detail.getIsResMsg()) ? "N" : detail.getIsResMsg();
            String isResMsgApi=StringUtils.isBlank(detail.getIsResMsgApi()) ? "" : detail.getIsResMsgApi();
            String isTemplateMsg =StringUtils.isBlank(detail.getIsTemplateMsg())?"N":detail.getIsTemplateMsg();
            if("Y".equalsIgnoreCase(isResMsg)) {
            	String msg =detail.getMessage();
            	if(StringUtils.isNotEmpty(isResMsgApi)) {
            		String apiResp = motSer.callMotorApi(detail, waid);
            		Map<String,Object> data =objectMapper.readValue(apiResp, Map.class);
            		for(Map.Entry<String, Object> entry :data.entrySet()) {
            			if(msg.contains(entry.getKey())) {
            				msg=msg.replace("{"+entry.getKey()+"}", entry.getValue()==null?"":entry.getValue().toString());
            			}
            		}
            		detail.setIsResMsg("N");
            		detail.setMessage(msg);
            	}else if(StringUtils.isEmpty(isResMsgApi)) {
            		Long currentStageCode =detail.getReqDetPk().getCurrentstage();
            		Long subStageCode =detail.getReqDetPk().getCurrentsubstage()-1;
            		String mesText=detailRepo.getMessageText(waid,currentStageCode.toString(),subStageCode.toString());
            		Map<String,Object> data =objectMapper.readValue(mesText, Map.class);
            		for(Map.Entry<String, Object> entry :data.entrySet()) {
            			if(msg.contains(entry.getKey())) {
            				msg=msg.replace("{"+entry.getKey()+"}", entry.getValue()==null?"":entry.getValue().toString());
            			}
            		}
            		detail.setIsResMsg("N");
            		detail.setMessage(msg);
            	}
            	
            	detail.setStatus("D");
            }
            
            else if (apiCall.equalsIgnoreCase("Y") && isValidationApi.equalsIgnoreCase("N") 
					&& "N".equals(detail.getIsReponseYn())) {

				String apiResp = "";

				apiResp = motSer.callMotorApi(detail, waid);

				detail.setMessage(apiResp.trim());
				
				detail.setStatus("R");
				
				/*if("Y".equalsIgnoreCase(isTemplateMsg))
					isTemplateMsg=detailRepo.getTemplateStatus(waid,detail.getReqDetPk().getCurrentstage().toString()
							,detail.getReqDetPk().getCurrentsubstage().toString(),detail.getRemarks());*/
				
			}
            
                       
			WAWatiReq waReq = WAWatiReq.builder()
					.filepath(detail.getFile_path())
					.msg(detail.getMessage())
					.waid(waid)
					.build();
			

			String url = "";

			if (fileyn.equalsIgnoreCase("Y")) {

				url = commonurl + fileurl;

				if (StringUtils.isNotBlank(waReq.getFilepath())) {

					WAWatiReq waRes = callSendSessionFile(okhttp, url, auth, waReq);

					detail.setIssent("Y");
					detail.setRequest_time(reqtime);
					detail.setResponse_time(new Date());
					detail.setWa_messageid(waRes.getWamsgId());
					detail.setWa_response(waRes.getWaresponse());
					detail.setWa_filepath(waRes.getWafilepath());
					detail.setSessionid(waRes.getSessionid());
				}
			} else {

				String status  = StringUtils.isBlank(detail.getStatus())?"Y":detail.getStatus();
				
				url=cs.getwebserviceurlProperty().getProperty("meta.message.api");
				
				auth=cs.getwebserviceurlProperty().getProperty("meta.message.api.auth");
				
				if(StringUtils.isNotBlank(waReq.getMsg()) && !"R".equals(status)) {

					Long stageCode = detail.getReqDetPk().getCurrentstage();
					Long subStageCode = detail.getReqDetPk().getCurrentsubstage();
					
					WhatsappTemplateMaster wamsgM =getTempMasterStageContent(detail.getRemarks(), "90016",
							Long.valueOf(waid), stageCode, subStageCode);
					
					String msgEn = StringUtils.isBlank(wamsgM.getMessage_content_en()) ? "" : wamsgM.getMessage_content_en();
					String msgAr = StringUtils.isBlank(wamsgM.getMessage_content_ar()) ? "" : wamsgM.getMessage_content_ar();
					String interactive_button_yn=StringUtils.isBlank(wamsgM.getInteractiveButtonYn())?"N":wamsgM.getInteractiveButtonYn();
					String isCtaDynamicYn =StringUtils.isBlank(wamsgM.getIsCtaDynamicYn())?"N":wamsgM.getIsCtaDynamicYn();
					String ctaButtonUrl =StringUtils.isBlank(wamsgM.getCtaButtonUrl())?"N":wamsgM.getCtaButtonUrl();
					String ctaButtonkeys =StringUtils.isBlank(wamsgM.getCtaButtonKeys())?"N":wamsgM.getCtaButtonKeys();
					
					String message_type=StringUtils.isBlank(wamsgM.getMessageType())?"":wamsgM.getMessageType();
					
					String language =contactDataRepo.getLanguage(waid.toString());
					
					String msg ="D".equals(detail.getStatus())?detail.getMessage():"English".equalsIgnoreCase(language)?msgEn:msgAr;
					
					String button1 ="",button2="",button3="",flow_id="",flow_token="",flowRequestDataYn ="",
									flow_api="",flow_api_auth="",flow_api_method ="",flow_button_name="",cta_button_name="",
									location_button_name="",menu_button_name="",flow_index_screen_name="",
									flow_api_request="";
					
					if("Y".equalsIgnoreCase(interactive_button_yn)) {
						message_type =wamsgM.getMessageType();
						if("FLOW".equalsIgnoreCase(message_type)) {
							flow_token =StringUtils.isBlank(wamsgM.getFlowToken())?"":wamsgM.getFlowToken();
							flowRequestDataYn =StringUtils.isBlank(wamsgM.getRequestdataYn())?"N":wamsgM.getRequestdataYn();
							flow_api =StringUtils.isBlank(wamsgM.getFlowApi())?"":wamsgM.getFlowApi();
							flow_api_auth =StringUtils.isBlank(wamsgM.getFlowApiAuth())?"":wamsgM.getFlowApiAuth();
							flow_api_method =StringUtils.isBlank(wamsgM.getFlowApiMethod())?"":wamsgM.getFlowApiMethod();
							flow_id =StringUtils.isBlank(wamsgM.getFlowId())?"":wamsgM.getFlowId();
							flow_index_screen_name=StringUtils.isBlank(wamsgM.getFlow_index_screen_name())?"":wamsgM.getFlow_index_screen_name();
							flow_api_request =StringUtils.isBlank(wamsgM.getFlowApiRequest())?"":wamsgM.getFlowApiRequest();

						}
						
						if("English".equalsIgnoreCase(language)) {
							button1 =StringUtils.isBlank(wamsgM.getButton_1())?"":wamsgM.getButton_1();
							button2 =StringUtils.isBlank(wamsgM.getButton_2())?"":wamsgM.getButton_2();
							button3 =StringUtils.isBlank(wamsgM.getButton_3())?"":wamsgM.getButton_3();
							flow_button_name =StringUtils.isBlank(wamsgM.getFlowButtonName())?"":wamsgM.getFlowButtonName();
							cta_button_name =StringUtils.isBlank(wamsgM.getCtaButtonName())?"":wamsgM.getCtaButtonName();
							location_button_name =StringUtils.isBlank(wamsgM.getLocButtonName())?"":wamsgM.getLocButtonName();
							menu_button_name=StringUtils.isBlank(wamsgM.getMenu_button_name())?"":wamsgM.getMenu_button_name();
					
						}else if("Swahili".equalsIgnoreCase(language)) {
							button1 =StringUtils.isBlank(wamsgM.getButton_1_sw())?"":wamsgM.getButton_1_sw();
							button2 =StringUtils.isBlank(wamsgM.getButton_2_sw())?"":wamsgM.getButton_2_sw();
							button3 =StringUtils.isBlank(wamsgM.getButton_3_sw())?"":wamsgM.getButton_3_sw();
							flow_button_name =StringUtils.isBlank(wamsgM.getFlowButtonNameSw())?"":wamsgM.getFlowButtonNameSw();
							cta_button_name =StringUtils.isBlank(wamsgM.getCtaButtonNameSw())?"":wamsgM.getCtaButtonNameSw();
							location_button_name =StringUtils.isBlank(wamsgM.getLocButtonNameSw())?"":wamsgM.getLocButtonNameSw();
							menu_button_name=StringUtils.isBlank(wamsgM.getMenu_button_name_sw())?"":wamsgM.getMenu_button_name_sw();

						}						
					}
					WAWatiReq waRequest = WAWatiReq.builder()
							.filepath("")
							.msg(msg)
							.waid(String.valueOf(waid))
							.button_1(button1) 
							.button_2(button2) 
							.button_3(button3) 
							.messageId(wamsgM.getRemarks())
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
							.flow_index_screen_name(flow_index_screen_name)
							.flow_api_request(flow_api_request)
							.interactiveYn(interactive_button_yn)
							.isCtaDynamicYn(isCtaDynamicYn)
							.ctaButtonUrl(ctaButtonUrl)
							.ctaButtonKeys(ctaButtonkeys)
							.build();


				
					
					WAWatiReq waRes = callSendSessionMsg(okhttp, body, url, auth, waRequest);
					
					detail.setIssent("Y");
					detail.setRequest_time(reqtime);
					detail.setResponse_time(new Date());
					detail.setWa_messageid(waRes.getWamsgId());
					detail.setWa_response(waRes.getWaresponse());
					detail.setWa_filepath(waRes.getWafilepath());
					detail.setSessionid(waRes.getSessionid());
					detail.setStatus("Y");
					detail.setMessage(waRequest.getMsg());
					
				}else if(StringUtils.isNotBlank(waReq.getMsg()) && "R".equals(status)) {
					
					url=cs.getwebserviceurlProperty().getProperty("meta.message.api");
					
					auth=cs.getwebserviceurlProperty().getProperty("meta.message.api.auth");
					
					waReq =objectMapper.readValue(detail.getMessage(), WAWatiReq.class);
					
					WAWatiReq waRes = callSendSessionMsg(okhttp, body, url, auth, waReq);

					detail.setIssent("Y");
					detail.setRequest_time(reqtime);
					detail.setResponse_time(new Date());
					detail.setWa_messageid(waRes.getWamsgId());
					detail.setWa_response(waRes.getWaresponse());
					detail.setWa_filepath(waRes.getWafilepath());
					detail.setSessionid(waRes.getSessionid());
					detail.setStatus("Y");
					detail.setMessage(waReq.getMsg());
				}
			}

			if(apiCall.equalsIgnoreCase("Y") && StringUtils.isBlank(waReq.getMsg())) {
				detail.setIssent("Y");
				detail.setRequest_time(reqtime);
				detail.setResponse_time(new Date());
			}

			detailRepo.save(detail);

		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	private String  setEmojiResponse(String msg) {
		try {
		String response = msg;

				List<Map<String, Object>> list=detailRepo.getEmojiDetails();
				
				for(Map<String, Object> map :list) {
					
					String key = map.get("KEY_CODE")==null?"": "{"+map.get("KEY_CODE").toString()+"}";
					
						if (response.contains(key)) {
							
							String emoji=EmojiParser.parseToUnicode(map.get("REMARKS")==null?"":map.get("REMARKS").toString().trim());
							
							response = response.replace( key  , emoji);
									
									
						}
					
				}
				return response;
			} catch (Exception e) {
				log.error(e);
			}
			return null;
		}
	
	public String sendValidationMsg(OkHttpClient okhttp, RequestBody body, String commonurl, String msgurl,
			String fileurl, String auth, WhatsappRequestDetail detail, String waid, Date reqtime, WhatsappTemplateMaster tempM) {
		try {

			String apiCall = StringUtils.isBlank(detail.getIsapicall()) ? "N" : detail.getIsapicall();
			String isValidationApi = StringUtils.isBlank(detail.getIsvalidationapi()) ? "N" : detail.getIsvalidationapi();
			String isValid = "N";
			String isResSaveApi = StringUtils.isBlank(detail.getIsResSaveApi()) ? "N" : detail.getIsResSaveApi();
			String isResMsg = StringUtils.isBlank(detail.getIsResMsg()) ? "N" : detail.getIsResMsg();
			//String isButtonMsg= StringUtils.isBlank(tempM.getIsButtonMsg())?"N":tempM.getIsButtonMsg();
			//String msgType= StringUtils.isBlank(tempM.getMsgType())?"":tempM.getMsgType();
			//String imageName = StringUtils.isBlank(tempM.getImageName())?"":tempM.getImageName();
			//String imageUrl = StringUtils.isBlank(tempM.getImageUrl())?"":tempM.getImageUrl();
			//String language =contactDataRepo.getLanguage(waid);
			//String button1 ="",button2="",button3="";
			/*if("Y".equalsIgnoreCase(isButtonMsg)) {
				if("English".equalsIgnoreCase(language)) {
					button1=tempM.getButton1();
					button2=tempM.getButton2();
					button3=StringUtils.isBlank(tempM.getButton3())?"":tempM.getButton3();
				}else if("Swahili".equalsIgnoreCase(language)) {
					button1=tempM.getButtonSw1();
					button2=tempM.getButtonSw2();
					button3=StringUtils.isBlank(tempM.getButtonSw3())?"":tempM.getButtonSw3();
				}						
			}
			String isTemplateMsg =StringUtils.isBlank(detail.getIsTemplateMsg())?"N":detail.getIsTemplateMsg();
			String buttonUrl=cs.getwebserviceurlProperty().getProperty("whatsapp.api.button");
			msgurl ="Y".equalsIgnoreCase(isButtonMsg)?buttonUrl:msgurl;*/
			if("Y".equalsIgnoreCase(isResSaveApi) && "Y".equalsIgnoreCase(apiCall)) {
				String apiResp = "";
				apiResp = motSer.callMotorApi(detail, waid);
				@SuppressWarnings("unchecked")
				Map<String,Object> data =new ObjectMapper().readValue(apiResp, Map.class);
				String url = commonurl + msgurl;
				String error_desc =data.get("ErrorDesc")==null?"":data.get("ErrorDesc").toString();
				if(StringUtils.isNotBlank(error_desc)) {
					String errorResStr =StringUtils.isBlank(tempM.getErrorrespstring())?"":tempM.getErrorrespstring();
					for (Map.Entry<String, Object> entry : data.entrySet()) {
						if (errorResStr.contains(entry.getKey().toString())) {
							errorResStr = errorResStr.replace("{" + entry.getKey().toString() + "}",
									entry.getValue() == null ? "" : entry.getValue().toString());
						}
					}
					WAWatiReq waReq = WAWatiReq.builder()
							.filepath(detail.getFile_path())
							.msg(errorResStr)
							.waid(waid)
							//.isButtonMsg(isButtonMsg)
							//.msgType(msgType)
							//.imageUrl(imageUrl)
							//.imageName(imageName)
							//.button1(button1)
							//.button2(button2)
							//.button3(button3)
							//.isTemplateMsg(isTemplateMsg)
							.build();

					WAWatiReq waRes = callSendSessionMsg(okhttp, body, url, auth, waReq);

					detail.setSessionid(waRes.getSessionid());
					isValid="N";
					detailRepo.save(detail);
				}else {
					String responseText =new Gson().toJson(data);
					detail.setApiMessageText(responseText);
					isValid="Y";
					detailRepo.save(detail);

				}
				
				
			}
			else if (apiCall.equalsIgnoreCase("Y") && isValidationApi.equalsIgnoreCase("Y") && "N".equals(isResMsg)) {

				String apiResp = "";

				apiResp = motSer.callMotorApi(detail, waid);

				String url = commonurl + msgurl;

				if (StringUtils.isNotBlank(apiResp)) {

					/*WAWatiReq waReq = WAWatiReq.builder()
							.filepath(detail.getFile_path())
							.msg(apiResp)
							.waid(waid)
							//.isButtonMsg(isButtonMsg)
							//.msgType(msgType)
							//.imageUrl(imageUrl)
							//.imageName(imageName)
							//.button1(button1)
							//.button2(button2)
							//.button3(button3)
							//.isTemplateMsg(isTemplateMsg)
							.build();*/
					
					
					WAWatiReq waReq =objectMapper.readValue(apiResp, WAWatiReq.class);

					WAWatiReq waRes = callSendSessionMsg(okhttp, body, url, auth, waReq);

					detail.setSessionid(waRes.getSessionid());
					
					isValid = "N";
				} else {
					isValid = "Y";
				}

				detailRepo.save(detail);
			}else {
				String apiResp = "";

				apiResp = motSer.callMotorApi(detail, waid);

				detail.setValidationmessage(apiResp.trim());

				String url = commonurl + msgurl;

				if (StringUtils.isNotBlank(apiResp)) {

					WAWatiReq waReq = WAWatiReq.builder()
							.filepath(detail.getFile_path())
							.msg(apiResp)
							.waid(waid)
							//.isButtonMsg(isButtonMsg)
							//.msgType(msgType)
							//.imageUrl(imageUrl)
							//.imageName(imageName)
							//.button1(button1)
							//.button2(button2)
							//.button3(button3)
							//.isTemplateMsg(isTemplateMsg)
							.build();

					WAWatiReq waRes = callSendSessionMsg(okhttp, body, url, auth, waReq);

					detail.setSessionid(waRes.getSessionid());
					
					isValid = "N";
				} else {
					isValid = "Y";
				}

				detailRepo.save(detail);
			}

			return isValid;
		} catch (Exception e) {
			log.error(e);
		}
		return "N";
	}

	public String sendDocValidationMsg(OkHttpClient okhttp, RequestBody body, String commonurl, String msgurl,
			String fileurl, String auth, WhatsappRequestDetail detail, String waid, Date reqtime, WhatsappTemplateMaster tempM) {
		try {

			/*String isButtonMsg= StringUtils.isBlank(tempM.getIsButtonMsg())?"N":tempM.getIsButtonMsg();
			String msgType= StringUtils.isBlank(tempM.getMsgType())?"":tempM.getMsgType();
			String imageName = StringUtils.isBlank(tempM.getImageName())?"":tempM.getImageName();
			String imageUrl = StringUtils.isBlank(tempM.getImageUrl())?"":tempM.getImageUrl();
			String language =contactDataRepo.getLanguage(waid);
			String button1 ="",button2="",button3="";
			if("Y".equalsIgnoreCase(isButtonMsg)) {
				if("English".equalsIgnoreCase(language)) {
					button1=tempM.getButton1();
					button2=tempM.getButton2();
					button3=StringUtils.isBlank(tempM.getButton3())?"":tempM.getButton3();
				}else if("Swahili".equalsIgnoreCase(language)) {
					button1=tempM.getButtonSw1();
					button2=tempM.getButtonSw2();
					button3=StringUtils.isBlank(tempM.getButtonSw3())?"":tempM.getButtonSw3();
				}						
			}
			String buttonUrl=cs.getwebserviceurlProperty().getProperty("whatsapp.api.button");

			String url ="Y".equalsIgnoreCase(isButtonMsg)? commonurl + buttonUrl:commonurl + msgurl; */
			
			WAWatiReq waReq = WAWatiReq.builder()
					.filepath(detail.getFile_path())
					.msg(StringUtils.isBlank(detail.getValidationmessage())?"":detail.getValidationmessage())
					.waid(waid)
					//.isButtonMsg(isButtonMsg)
					//.msgType(msgType)
					//.imageUrl(imageUrl)
					//.imageName(imageName)
					//.button1(button1)
					//.button2(button2)
					//.button3(button3)
					.build();

			WAWatiReq waRes = callSendSessionMsg(okhttp, body, "", auth, waReq);

		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}

	public WAWatiReq callSendSessionMsg(OkHttpClient okhttp, RequestBody body, String url, String auth, WAWatiReq req) {

		WAWatiReq detail = new WAWatiReq();
		try {
			String waid = req.getWaid();
			String msgs = req.getMsg();
			msgs=setEmojiResponse(msgs);
			req.setMsg(msgs);
			detail.setWaid(waid);
			detail.setMsg(msgs);
						
			if("Y".equals(req.getInteractiveYn())) {
				String message_text =frameWhatsappMetaMsg(req,okhttp,body);
				body = RequestBody.create(message_text,contentType);
			}else {
				
				Map<String,Object> text = new HashMap<String, Object>();
				text.put("body", req.getMsg());
				
				Map<String,Object> sendMsgMap = new HashMap<String, Object>();
				sendMsgMap.put("messaging_product", "whatsapp");
				sendMsgMap.put("recipient_type", "individual");
				sendMsgMap.put("to", req.getWaid());
				sendMsgMap.put("type", "text");
				sendMsgMap.put("text", text);
				
				String twoSlash=cs.getwebserviceurlProperty().getProperty("wa.hit.slash.two");
				String oneSlash=cs.getwebserviceurlProperty().getProperty("wa.hit.slash.one");
			
				String message_text =cs.reqPrint(sendMsgMap);
				
				message_text =message_text.replace(twoSlash, oneSlash);
				
				MediaType contentType =MediaType.parse("application/json");
				body =RequestBody.create(message_text,contentType);
				
			}
			
			
			
			Request request = new Request.Builder()
					.url(url)
					.addHeader("Authorization", auth)
					.post(body)
					.build();

			Response response = okhttp.newCall(request).execute();

			String responseString = response.body().string();

			log.info("callSendSessionMsg--> waid: " + waid + " response: " + responseString);

			SendMessageResponse apiRes = objectMapper.readValue(responseString, SendMessageResponse.class);

			String result = "success";

			String msgid = "", sessionid = "";

			if (result.equalsIgnoreCase("success") || StringUtils.isEmpty(result)) {

				if (apiRes.getMessages()!= null) {
										
					msgid = apiRes.getMessages().get(0).getId();
					
					sessionid = apiRes.getMessages().get(0).getId();
				}

				detail.setIssent("Y");
				detail.setWamsgId(msgid);
				detail.setWaresponse(result);
				detail.setSessionid(sessionid);

			} else {
				String msgRes = apiRes.getMessages() == null ? "" : String.valueOf(apiRes.getMessages().get(0).getId());
				String info ="";

				detail.setIssent("N");
				detail.setWamsgId(msgid);
				detail.setWaresponse(StringUtils.isBlank(info) ? msgRes : info);
				detail.setSessionid(sessionid);
			}

		} catch (HttpStatusCodeException e) {
			log.error(e);
			cs.reqPrint(e.getResponseBodyAsString());

			detail.setIssent("N");
			detail.setWaresponse(e.getLocalizedMessage());

		} catch (Exception e) {
			log.error(e);

			detail.setIssent("N");
			detail.setWaresponse(e.getLocalizedMessage());
		}
		return detail;
	}

	

	public WAWatiReq callSendSessionFile(OkHttpClient okhttp, String url, String auth, WAWatiReq req) {

		WAWatiReq detail = new WAWatiReq();

		try {

			String filePath = req.getFilepath();

			String fileName = FilenameUtils.getName(filePath);

			String waid = req.getWaid();
			String msgs = req.getMsg();

			msgs = URLEncoder.encode(msgs, StandardCharsets.UTF_8.toString());

			url = url.replace("{whatsappNumber}", waid);
			url = url.replace("{pageSize}", "");
			url = url.replace("{pageNumber}", "");
			url = url.replace("{caption}", msgs);
			url = url.trim();

			detail.setWaid(waid);
			detail.setMsg(req.getMsg());
			detail.setFilepath(filePath);

			File file = new File(filePath);

			boolean exist = file.exists();

			log.info("callSendSessionFile--> exist: " + exist);

			if(exist) {
				String mimeType = Files.probeContentType(file.toPath());

				RequestBody body = new MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("file", fileName, RequestBody.create(file, MediaType.parse(mimeType)))
						.build();

				Request request = new Request.Builder()
						.url(url)
						.addHeader("Authorization", auth)
						.post(body)
						.build();

				Response response = okhttp.newCall(request).execute();

				String responseString = response.body().string();
				
				log.info("callSendSessionFile--> mobno: " + waid + " response: " + responseString);

				SendSessionFile apiRes = objectMapper.readValue(responseString, SendSessionFile.class);

				String result = apiRes.getResult();

				String msgid = "", sessionid = "";
				String waFilepath = "";

				if (result.equalsIgnoreCase("success")) {

					if (apiRes.getMessage() != null) {

						MessageFileRes msg = objectMapper.convertValue(apiRes.getMessage(),
								MessageFileRes.class);

						cs.reqPrint(msg);

						msgid = msg.getId();
						sessionid = msg.getTicketId();
						waFilepath = msg.getText();
					}

					detail.setIssent("Y");
					detail.setWamsgId(msgid);
					detail.setWaresponse(result);
					detail.setSessionid(sessionid);

				} else {
					String msgRes = apiRes.getMessage() == null ? "" : String.valueOf(apiRes.getMessage());
					String info = StringUtils.isBlank(apiRes.getInfo()) ? "" : apiRes.getInfo();

					detail.setIssent("N");
					detail.setWamsgId(msgid);
					detail.setWaresponse(StringUtils.isBlank(info) ? msgRes : info);
					detail.setWafilepath(waFilepath);
					detail.setSessionid(sessionid);
				}
			}

		} catch (HttpStatusCodeException e) {
			log.error(e);
			cs.reqPrint(e.getResponseBodyAsString());

			detail.setIssent("N");
			detail.setWaresponse(e.getLocalizedMessage());

		} catch (Exception e) {
			log.error(e);

			detail.setIssent("N");
			detail.setWaresponse(e.getLocalizedMessage());
		}
		return detail;
	}

	public String sendDocmentMsg(OkHttpClient okhttp, RequestBody body, String commonurl, String msgurl, String fileurl,
			String auth, WhatsappRequestDetail detail, String waid, Date date, WhatsappTemplateMaster tempM) {
			String isValid = "N";

		try {
			String apiResp =tempM.getMessage_content_en()+tempM.getMessage_regards_en();
			String apiCall = StringUtils.isBlank(detail.getIsapicall()) ? "N" : detail.getIsapicall();
			String isValidationApi = StringUtils.isBlank(detail.getIsvalidationapi()) ? "N" : detail.getIsvalidationapi();

				//apiResp = motSer.callMotorApi(detail, waid);

				detail.setValidationmessage(apiResp.trim());

				String url = commonurl + msgurl;

				if (StringUtils.isNotBlank(apiResp)) {

					WAWatiReq waReq = WAWatiReq.builder()
							.filepath(detail.getFile_path())
							.msg(apiResp)
							.waid(waid)
							.build();

					WAWatiReq waRes = callSendSessionMsg(okhttp, body, url, auth, waReq);

					detail.setSessionid(waRes.getSessionid());
					
					isValid = "N";
				} else {
					isValid = "Y";
				}

				detailRepo.save(detail);
			
			
		}catch (Exception e) {
			e.printStackTrace();
		}
       
		return isValid;
	}



	public String sendIsResYnMsg(OkHttpClient okhttp, RequestBody body, String commonurl, String msgurl, String fileurl,
			String auth, WhatsappRequestDetail reqDet, String waid, Date date) {
		try {
			String apiResp = "";

			apiResp = motSer.callMotorApi(reqDet, waid);

			String url = commonurl + msgurl;
			
			url =url.replace("{whatsappNumber}", waid);
			
			if(StringUtils.isBlank(apiResp)) {
				
				apiResp="Please enter valid data";

				WAWatiReq waReq = WAWatiReq.builder()
						.filepath(reqDet.getFile_path())
						.msg(reqDet.getMessage())
						.waid(waid)
						.build();


				WAWatiReq waRes = callSendSessionMsg(okhttp, body, url, auth, waReq);

				waRes.setSessionid(waRes.getSessionid());
				
			}else {
				
				WhatsappTemplateMaster temp =getTempMasterStageContent(reqDet.getRemarks(), "90016",
						Long.valueOf(waid), reqDet.getReqDetPk().getCurrentstage(), reqDet.getReqDetPk().getCurrentsubstage());
				
				ButtonHeaderReq header=null;
				
				/*if("Image".equalsIgnoreCase(temp.getMsgType())) {
					
					ButtonMediaReq media =ButtonMediaReq.builder()
							.fileName(temp.getImageName())
							.url(temp.getImageUrl())
							.build();
					
					 header =ButtonHeaderReq.builder().type(temp.getMsgType()).
							text(temp.getButtonHeader())
							.media(media)
							.build();
				}else {
					
					 header =ButtonHeaderReq.builder().type(StringUtils.isBlank(temp.getMsgType())?"Text":temp.getMsgType()).
							 text(temp.getButtonHeader())
							.media(null)
							.build();
				}
				
				
				List<ButtonsNameReq>buttons =new ArrayList<ButtonsNameReq>(); 
				
				buttons.add(new ButtonsNameReq(StringUtils.isBlank(temp.getButton1())?"MainMenu":temp.getButton1()));
				if(StringUtils.isNotBlank(temp.getButton2())) {
					buttons.add(new ButtonsNameReq(temp.getButton2()));
				}
				if(StringUtils.isNotBlank(temp.getButton3())) {
					buttons.add(new ButtonsNameReq(temp.getButton3()));
				}
				WhatsAppButtonReq req = WhatsAppButtonReq.builder()
						.header(header)
						.body(apiResp)
						.footer(StringUtils.isBlank(temp.getButtonFooter())?"":temp.getButtonFooter())
						.buttons(buttons)
						.build();
				
				RequestBody requestBody =RequestBody.create(cs.reqPrint(req), MediaType.parse("application/json"));
				
				Request request = new Request.Builder()
						.url(url)
						.addHeader("Authorization", auth)
						.post(requestBody)
						.build();
	
				Response response = okhttp.newCall(request).execute();
	
				String responseString = response.body().string();
				
				log.info("Whatsapp button api response" +responseString);
				
				reqDet.setIsResponseYnSent("Y");
				
				detailRepo.save(reqDet);*/
			}
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		return "N";
	}
	
	private WAWatiReq sendButtonMsg(WAWatiReq waReq, WhatsappTemplateMaster temp) {
		WAWatiReq detail = new WAWatiReq();
		Gson jsonPrint =new Gson();
		try {
			String commonurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api");
			String button_msg_url = cs.getwebserviceurlProperty().getProperty("whatsapp.api.button");
			String auth = cs.getwebserviceurlProperty().getProperty("whatsapp.auth");

			String url = commonurl + button_msg_url;
			
			url =url.replace("{whatsappNumber}", waReq.getWaid());			
			
			ButtonHeaderReq header=null;
			
			String messageBody ="";
			
			String isApiCall =StringUtils.isBlank(temp.getIsapicall())?"N":temp.getIsapicall();
			
			String Isreplyyn =StringUtils.isBlank(temp.getIsreplyyn())?"N":temp.getIsreplyyn();
			
			/*if("Y".equals(isApiCall) || "N".equals(Isreplyyn)) 
				messageBody =StringUtils.isBlank(waReq.getMsg())?"Something went wrong":waReq.getMsg();
			else 
				messageBody =StringUtils.isBlank(temp.getButtonBody())?"Something went wrong":temp.getButtonBody();
			
			if("Image".equalsIgnoreCase(temp.getMsgType())) {
				
				ButtonMediaReq media =ButtonMediaReq.builder()
						.fileName(temp.getImageName())
						.url(temp.getImageUrl())
						.build();
				
				 header =ButtonHeaderReq.builder().type(temp.getMsgType()).
						text(temp.getButtonHeader())
						.media(media)
						.build();
				 
			}else if("Text".equalsIgnoreCase(temp.getMsgType())) {
				
				header =ButtonHeaderReq.builder().type(temp.getMsgType()).
						text(temp.getButtonHeader())
						.media(null)
						.build();
			}
			
			OkHttpClient okhttp = new OkHttpClient.Builder()
					.readTimeout(30, TimeUnit.SECONDS)
					.build();
			
			List<ButtonsNameReq>buttons =new ArrayList<ButtonsNameReq>(); 
			
			buttons.add(new ButtonsNameReq(StringUtils.isBlank(temp.getButton1())?"MainMenu":temp.getButton1()));
			if(StringUtils.isNotBlank(temp.getButton2())) {
				buttons.add(new ButtonsNameReq(temp.getButton2()));
			}
			if(StringUtils.isNotBlank(temp.getButton3())) {
				buttons.add(new ButtonsNameReq(temp.getButton3()));
			}

			WhatsAppButtonReq req = WhatsAppButtonReq.builder()
					.header(header)
					.body(messageBody)
					.footer(StringUtils.isBlank(temp.getButtonFooter())?"":temp.getButtonFooter())
					.buttons(buttons)
					.build();
			
			String obj =jsonPrint.toJson(req);
			
			String twoSlash=cs.getwebserviceurlProperty().getProperty("wa.hit.slash.two");
			String oneSlash=cs.getwebserviceurlProperty().getProperty("wa.hit.slash.one");
			
			String msgReq =obj.replace(twoSlash, oneSlash);

			System.out.println(msgReq);
			
			RequestBody requestBody =RequestBody.create(msgReq, MediaType.parse("application/json"));			
			
			Request request = new Request.Builder()
					.url(url)
					.addHeader("Authorization", auth)
					.post(requestBody)
					.build();

			Response response = okhttp.newCall(request).execute();

			String responseString = response.body().string();
			
			log.info("Whatsapp button api response" +responseString);
			
			log.info("callSendSessionMsg--> waid: " + waReq.getWaid() + " response: " + responseString);

			SendSessionMsg apiRes = objectMapper.readValue(responseString, SendSessionMsg.class);

			String result = apiRes.getResult();

			String msgid = "", sessionid = "";

			if (apiRes.getMessage()!=null) {

				if (apiRes.getMessage() != null) {
					MessageSendRes msg = objectMapper.convertValue(apiRes.getMessage(),
							MessageSendRes.class);

					cs.reqPrint(msg);
					
					msgid = msg.getId();
					
					sessionid = msg.getTicketId();
				}

				detail.setIssent("Y");
				detail.setWamsgId(msgid);
				detail.setWaresponse(result);
				detail.setSessionid(sessionid);

			} else {
				String msgRes = apiRes.getMessage() == null ? "" : String.valueOf(apiRes.getMessage());
				String info = StringUtils.isBlank(apiRes.getInfo()) ? "" : apiRes.getInfo();

				detail.setIssent("N");
				detail.setWamsgId(msgid);
				detail.setWaresponse(StringUtils.isBlank(info) ? msgRes : info);
				detail.setSessionid(sessionid);
			}*/

			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return detail;
	}
	
	public  WhatsappTemplateMaster getTempMasterStageContent(String msgid, String agencycode, Long waid,
			Long stageCode, Long subStageCode) {
		try {

			QWhatsappTemplateMaster qtempM = QWhatsappTemplateMaster.whatsappTemplateMaster;
			QWhatsappTemplateMasterPK qtempMPk = qtempM.tempMasterPk;

			WhatsappTemplateMaster tempM = jpa
					.selectFrom(qtempM)
					.where(qtempM.remarks.eq(msgid)
							.and(qtempM.status.equalsIgnoreCase("Y"))
							.and(qtempMPk.agencycode.eq(agencycode))
							.and(qtempM.ischatyn.equalsIgnoreCase("Y"))
							.and(qtempMPk.stagecode.eq(stageCode))
							.and(qtempMPk.stagesubcode.eq(subStageCode))
							)
					.orderBy(qtempM.stage_order.asc())
					.fetchFirst();

			return tempM;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}
	
	
	private String frameWhatsappMetaMsg(WAWatiReq req, OkHttpClient okhttp, RequestBody requestBody) {
		String message ="";
		try {
			
			Map<String,Object> interactive =new HashMap<String, Object>();
			
			if("Menu".equalsIgnoreCase(req.getMessageType())) {
				
				List<Map<String,Object>> go_back =new ArrayList<>();
				
				Map<String,Object> main_menu = new HashMap<>();
				main_menu.put("id", "Main Menu");
				main_menu.put("title", "Home");
				main_menu.put("description", "Main Menu");
				
				Map<String,Object> previous_menu = new HashMap<>();
				previous_menu.put("id", "Previous Menu");
				previous_menu.put("title", "Back");
				previous_menu.put("description", "Previous Menu");
				
				Map<String,Object> change_lanuage = new HashMap<>();
				change_lanuage.put("id", "00");
				change_lanuage.put("title", "Language");
				change_lanuage.put("description", "Language Change Option");
				
				go_back.add(main_menu);
				go_back.add(previous_menu);
				go_back.add(change_lanuage);
				
				Map<String,Object> goback_section = new HashMap<>();
				goback_section.put("rows", go_back);
				goback_section.put("title", "Footer");
				
				//=================================
				
				List<WhatsappMessageMenuMaster> menuList = wmmmRpo.findByMessageIdAndStatusIgnoreCaseOrderByDisplayOrder(req.getMessageId(),"Y");
				
				Map<String,Object> header =new HashMap<String, Object>();
				Map<String,Object> body =new HashMap<String, Object>();
				Map<String,Object> footer =new HashMap<String, Object>();
				Map<String,Object> action =new HashMap<String, Object>();
				
				header.put("type", "text");
				header.put("text", "");
				
				body.put("text", req.getMsg());
				
				footer.put("text", "");
				
				
				List<Map<String,Object>> rows =menuList.stream().map(p ->{
					Map<String,Object> map = new HashMap<>();
					map.put("id", p.getOptionNo());
					map.put("title", p.getOptionTitle());
					map.put("description", StringUtils.isBlank(p.getOptionDesc())?"":p.getOptionDesc());
					return map;
				}).collect(Collectors.toList());
				
				Map<String,Object> section_rows = new HashMap<String, Object>();
				section_rows.put("rows", rows);
				section_rows.put("title", "Alliance Products");
				
				List<Map<String,Object>> sections =new ArrayList<>();
				sections.add(section_rows);
				sections.add(goback_section);
				
				action.put("button", req.getMenu_button_name());
				action.put("sections", sections);
				
				interactive.put("type", "list");
				interactive.put("header", header);
				interactive.put("body", body);
				interactive.put("footer", footer);
				interactive.put("action", action);
				
				
		
			}else if("flow".equalsIgnoreCase(req.getMessageType())) {
				
				Map<String,Object> parameters = new HashMap<String, Object>();
				parameters.put("flow_message_version", "3");
				parameters.put("flow_token", req.getFlowToken());
				parameters.put("flow_id", req.getFlowId());
				parameters.put("flow_cta", req.getFlow_button_name());
				parameters.put("flow_action", "navigate");
				//parameters.put("mode", "draft");
				
				if("Y".equals(req.getFlow_requestdata_yn())) {
					
					Map<String,Object> data = new HashMap<String, Object>();
					
					if("GET".equalsIgnoreCase(req.getFlowApiMethod())) {
						
						String url =req.getFlowApi().replace("{mobile_no}", req.getWaid());
						
						Request request = new Request.Builder()
								.url(url.trim())
								.addHeader("Authorization", req.getFlowApiAuth().trim())
								.get()
								.build();

						Response response = okhttp.newCall(request).execute();

						String responseString = response.body().string();
						data = objectMapper.readValue(responseString, Map.class);
						
					}else if("POST".equalsIgnoreCase(req.getFlowApiMethod())) {
						
						MediaType mediaType = MediaType.parse("application/json");
						
						String input_req = StringUtils.isBlank(req.getFlow_api_request())?null:req.getFlow_api_request();
						
						requestBody = RequestBody.create(input_req,mediaType);
						
						Request request = new Request.Builder()
								.url(req.getFlowApi().trim())
								.addHeader("Authorization", req.getFlowApiAuth().trim())
								.post(requestBody)
								.build();

						Response response = okhttp.newCall(request).execute();

						String responseString = response.body().string();
						data = objectMapper.readValue(responseString, Map.class);
					}
					
					parameters.put("flow_action_payload", data);
					
				}else {
					Map<String,Object> flow_action_payload = new HashMap<String, Object>();
					flow_action_payload.put("screen", req.getFlow_index_screen_name());
					flow_action_payload.put("data", null);
					parameters.put("flow_action_payload", flow_action_payload);
				}
				
				Map<String,Object> action = new HashMap<String, Object>();
				action.put("name", "flow");
				action.put("parameters", parameters);
				
				Map<String,Object> body =new HashMap<String, Object>();
				body.put("text", req.getMsg());
				
				interactive.put("type", "flow");
				interactive.put("header", "");
				interactive.put("body", body);
				interactive.put("footer", "");
				interactive.put("action", action);
				
			}else if("button".equalsIgnoreCase(req.getMessageType())) {
				
				List<Map<String,Object>> buttonList =new ArrayList<>();
			
				if(StringUtils.isNotBlank(req.getButton_1())) {
					Map<String,Object> button_text = new HashMap<String, Object>();
					button_text.put("type", "reply");
					Map<String,Object> reply = new HashMap<String, Object>();
					reply.put("id", req.getButton_1());
					reply.put("title", req.getButton_1());
					button_text.put("reply", reply);
					buttonList.add(button_text);
					
				}if(StringUtils.isNotBlank(req.getButton_2())) {
					Map<String,Object> button_text = new HashMap<String, Object>();
					button_text.put("type", "reply");
					Map<String,Object> reply = new HashMap<String, Object>();
					reply.put("id", req.getButton_2());
					reply.put("title", req.getButton_2());
					button_text.put("reply", reply);
					buttonList.add(button_text);

				}if(StringUtils.isNotBlank(req.getButton_3())) {
					Map<String,Object> button_text = new HashMap<String, Object>();
					button_text.put("type", "reply");
					Map<String,Object> reply = new HashMap<String, Object>();
					reply.put("id", req.getButton_3());
					reply.put("title", req.getButton_3());
					button_text.put("reply", reply);
					buttonList.add(button_text);

				}
				
				Map<String,Object> body =new HashMap<String, Object>();
				body.put("text", req.getMsg());
				
				Map<String,Object> actions =new HashMap<String, Object>();
				actions.put("buttons", buttonList);
				
				interactive.put("type", "button");
				interactive.put("body", body);
				interactive.put("action", actions);
				
			}else if("cta_button".equalsIgnoreCase(req.getMessageType())) {
				
				Map<String,Object> cta_data = new HashMap<String, Object>();
				String responseStr ="";
				if("Y".equals(req.getIsCtaDynamicYn())) {
					cta_data = objectMapper.readValue(req.getApiData(), Map.class);
					responseStr =req.getCtaButtonKeys();
					for(Map.Entry<String, Object> entry : cta_data.entrySet()) {
						
						if(responseStr.contains("{"+entry.getKey()+"}")) {
							
							responseStr =responseStr.replace("{"+entry.getKey()+"}", entry.getValue() ==null?"":entry.getValue().toString());
						
							break;
						}
					}
				}else if("N".equals(req.getIsCtaDynamicYn())) {
					
					responseStr =req.getCtaButtonUrl();
				}
				
				Map<String,Object> body =new HashMap<String, Object>();
				Map<String,Object> action =new HashMap<String, Object>();
				Map<String,Object> parameters =new HashMap<String, Object>();
				
				parameters.put("display_text", req.getCta_button_name());
				parameters.put("url", responseStr.trim());
				
				body.put("text", req.getMsg());
				
				action.put("name", "cta_url");
				action.put("parameters", parameters);
				
				interactive.put("type", "cta_url");
				interactive.put("body", body);
				interactive.put("action", action);
				
				
			}else if("template".equalsIgnoreCase(req.getMessageType())) {
				
			}else if("location".equalsIgnoreCase(req.getMessageType())) {
				
				Map<String,Object> body =new HashMap<String, Object>();
				Map<String,Object> action =new HashMap<String, Object>();
				body.put("text", req.getMsg());
				action.put("name", "send_location");
				interactive.put("type", "location_request_message");
				interactive.put("body", body);
				interactive.put("action", action);
				
			}
			
			Map<String,Object> map = new HashMap<String, Object>();
			map.put("messaging_product", "whatsapp");
			map.put("recipient_type", "individual");
			map.put("to",req.getWaid() );
			map.put("type", "interactive");
			map.put("interactive", interactive);
			
			message = cs.reqPrint(map);
			
			log.info("Whatsapp Flow Message Req : "+ message);

			
			String twoSlash=cs.getwebserviceurlProperty().getProperty("wa.hit.slash.two");
			String oneSlash=cs.getwebserviceurlProperty().getProperty("wa.hit.slash.one");
			
			message =message.replace(twoSlash, oneSlash);
			
			
		}catch (Exception e) {
			log.error(e);
			e.printStackTrace();
		}
		return message;
	}
	
	
}
