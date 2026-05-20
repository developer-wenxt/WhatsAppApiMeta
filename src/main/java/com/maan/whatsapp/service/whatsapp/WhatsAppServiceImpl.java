package com.maan.whatsapp.service.whatsapp;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.maan.whatsapp.entity.master.QWAChatRecipientMaster;
import com.maan.whatsapp.entity.master.QWAMessageMaster;
import com.maan.whatsapp.entity.master.QWhatsappTemplateMaster;
import com.maan.whatsapp.entity.master.QWhatsappTemplateMasterPK;
import com.maan.whatsapp.entity.master.WAChatRecipientMaster;
import com.maan.whatsapp.entity.master.WAChatRecipientMasterPK;
import com.maan.whatsapp.entity.master.WAMessageMaster;
import com.maan.whatsapp.entity.master.WhatsappTemplateMaster;
import com.maan.whatsapp.entity.master.WhatsappTemplateMasterPK;
import com.maan.whatsapp.entity.whatsapp.QWhatsappRequestDetail;
import com.maan.whatsapp.entity.whatsapp.WADataDetail;
import com.maan.whatsapp.entity.whatsapp.WADataDetailPK;
import com.maan.whatsapp.entity.whatsapp.WhatsappContactData;
import com.maan.whatsapp.entity.whatsapp.WhatsappRequestDetail;
import com.maan.whatsapp.entity.whatsapp.WhatsappRequestDetailPK;
import com.maan.whatsapp.repository.master.WAChatRecipientMasterRepo;
import com.maan.whatsapp.repository.master.WAMessageMasterRepo;
import com.maan.whatsapp.repository.master.WhatsappTemplateMasterRepo;
import com.maan.whatsapp.repository.whatsapp.WADataDetailRepo;
import com.maan.whatsapp.repository.whatsapp.WhatsappContactDataRepo;
import com.maan.whatsapp.repository.whatsapp.WhatsappRequestDetailRepo;
import com.maan.whatsapp.request.motor.WAQuoteReq;
import com.maan.whatsapp.request.wati.getcont.WebhookReq;
import com.maan.whatsapp.request.whatsapp.ButtonHeaderReq;
import com.maan.whatsapp.request.whatsapp.ButtonsNameReq;
import com.maan.whatsapp.request.whatsapp.WASaveReq;
import com.maan.whatsapp.request.whatsapp.WAWatiReq;
import com.maan.whatsapp.request.whatsapp.WhatsAppButtonReq;
import com.maan.whatsapp.request.whatsapp.WhatsAppReq;
import com.maan.whatsapp.response.motor.WAQuoteRes;
import com.maan.whatsapp.service.common.CommonService;
import com.maan.whatsapp.service.motor.MotorServiceImpl;
import com.maan.whatsapp.service.wati.WatiApiCall;
import com.maan.whatsapp.service.wati.WatiService;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
@PropertySource("classpath:WebServiceUrl.properties")
public class WhatsAppServiceImpl implements WhatsAppService {

	@Autowired
	private WhatsappRequestDetailRepo detailRepo;
	
	@Autowired
	private WhatsappContactDataRepo contactRepo;
	
	@Autowired
	private WhatsappTemplateMasterRepo wtmRepo;
	
	@Autowired
	private WADataDetailRepo wddRepo;
	
	@Autowired
	private WAMessageMasterRepo wamsgMRepo;
	
	@Autowired
	private WAChatRecipientMasterRepo waChatRepo;

	@Autowired
	private CommonService cs;
	
	@Autowired
	private WatiService watiSer;
	
	@Autowired
	private WatiApiCall watiApiCall;
	
	@Autowired 
	private MotorServiceImpl motorImpl;

	@Autowired
	private JPAQueryFactory jpa;
	
	@Value("${main.menu.button}")
	private String mainMenu;
	
	@Value("${previous.menu.button}")
	private String previousMenu;
	
	@Value("${main.menu.button.swahili}")
	private String mainMenuSwahili;
	
	@Value("${previous.menu.button.swahili}")
	private String preMenuSwahili;
	

	private Logger log = LogManager.getLogger(getClass());

	@Override
	public String saveRequestDetail(List<WhatsAppReq> request) {
		try {
			log.info("saveRequestDetail--> request: ");
			cs.reqPrint(request);

			if (request != null) {
				if (request.size() > 0) {
					insertReqDet(request);
				}
			}

		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}

	private String insertReqDet(List<WhatsAppReq> requestList) {
		try {

			List<Long> list = new ArrayList<>();

			String isJobyn = "";

			for(WhatsAppReq request : requestList) {

				Long whatsAppno = Long.valueOf(request.getWhatsappno());
				Long whatsAppCode = Long.valueOf(request.getWhatsappCode());
				Long whatsAppid = Long.valueOf(String.valueOf(whatsAppCode) + String.valueOf(whatsAppno));

				isJobyn = StringUtils.isBlank(request.getIsJobYN()) ? "N" : request.getIsJobYN();

				WhatsappRequestDetailPK detailPk = WhatsappRequestDetailPK.builder()
						.currentstage(Long.valueOf(request.getCurrentStage()))
						.currentsubstage(Long.valueOf(request.getSubStage()))
						.mobileno(whatsAppno)
						.productid(Long.valueOf(request.getProductid()))
						.quoteno(Long.valueOf(request.getQuoteNo()))
						.whatsappcode(whatsAppCode)
						.build();

				WhatsappRequestDetail detail = WhatsappRequestDetail.builder()
						.entry_date(new Date())
						.file_path(request.getFilePath())
						.file_yn(StringUtils.isBlank(request.getFileYN()) ? "N" : request.getFileYN())
						.isread(null)
						.issent("N")
						.message(StringUtils.isBlank(request.getMessage()) ? "" : request.getMessage().trim())
						.remarks(request.getType())
						.reqDetPk(detailPk)
						.request_time(null)
						.response_time(null)
						.status("Y")
						.stage_order(StringUtils.isBlank(request.getStageOrder()) ? 1L : Long.valueOf(request.getStageOrder()))
						.whatsappid(whatsAppid)
						.isjobyn(StringUtils.isBlank(request.getIsJobYN()) ? "N" : request.getIsJobYN())
						.isreplyyn(StringUtils.isBlank(request.getIsreplyyn()) ? "N" : request.getIsreplyyn())
						.isprocesscompleted("N")
						.isapicall(StringUtils.isBlank(request.getIsapicall()) ? "N" : request.getIsapicall())
						.requestkey(StringUtils.isBlank(request.getRequestkey()) ? "" : request.getRequestkey())
						.isskipyn(StringUtils.isBlank(request.getIsskipyn()) ? "" : request.getIsskipyn())
						.isdocuplyn(StringUtils.isBlank(request.getIsdocuplyn()) ? "" : request.getIsdocuplyn())
						.isskipped("N")
						.isvalidationapi(StringUtils.isBlank(request.getIsValidationApi()) ? "N" : request.getIsValidationApi())
						.isvalid("N")
						.stageDesc(request.getStageDesc())
						.isReponseYn(request.getIsResponseYn())
						.isResponseYnSent(request.getIsResponseYnSent())
						.isButtonMsg(request.getIsButtonMsgYn()) 
						.isResSaveApi(request.getIsResSaveApi())
						.isResMsg(request.getIsResMsg())
						.isResMsgApi(StringUtils.isBlank(request.getIsResMsgApi())?"":request.getIsResMsgApi())
						.isTemplateMsg(request.getIsTemplateMsg())
						.formpageUrl(StringUtils.isBlank(request.getFormPageUrl())?"":request.getFormPageUrl())
						.formpageYn(StringUtils.isBlank(request.getFormPageYn())?"":request.getFormPageYn())
						.build();

				detailRepo.save(detail);

				if (!list.contains(whatsAppid))
					list.add(whatsAppid);
			}

			if (requestList.size() > 0) {

				if(isJobyn.equalsIgnoreCase("N")) {
					Flux.fromIterable(list)
						.map(i -> watiSer.sendSessionMsg(i))
						.subscribeOn(Schedulers.boundedElastic())
						.subscribe();
				} else {
					Flux.fromIterable(list)
						.map(i -> watiSer.callSendSessionMsg(i, requestList.get(0).getType()))
						.subscribeOn(Schedulers.boundedElastic())
						.subscribe();
				}
			}

		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}

	@Override
	public String webhookRes(WebhookReq request) {
		try {

			log.info("webhookRes--> request: ");
			cs.reqPrint(request);
 
			List<WebhookReq> list = new ArrayList<WebhookReq>();
			list.add(request);

			Flux.fromIterable(list)
				.map(i -> saveContactData(i))
				.subscribeOn(Schedulers.boundedElastic())
				.subscribe();

		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	private String saveContactData(WebhookReq request) {
		try {

			Long waid = Long.valueOf(request.getWaId());

			log.info("saveContactData--> waid: " + waid);

			Date watiDate = new Date();

			log.info("saveContactData--> watiDate: " + watiDate);
			
			long count = watiSer.checkSessionStatus(waid);
			
			if(count>0) {
				String timezoneHours = cs.getwebserviceurlProperty().getProperty("timezone.hours");
				String timezoneMinutes = cs.getwebserviceurlProperty().getProperty("timezone.minutes");
	
				Date sessStartTime = cs.addHours(watiDate, Integer.valueOf(timezoneHours));
				sessStartTime = cs.addMinutes(sessStartTime, Integer.valueOf(timezoneMinutes));
	
				Date sessEndTime = cs.addHours(sessStartTime, 24);
				
				WhatsappContactData data = WhatsappContactData.builder()
						.entry_date(new Date())
						.session_end_time(sessEndTime)
						.session_start_time(sessStartTime)
						.status("Y")
						.wa_messageid(request.getId())
						.whatsappid(waid)
						.remarks("")
						.sendername(StringUtils.isBlank(request.getSenderName()) ? "" : request.getSenderName())
						.language("English".equalsIgnoreCase(request.getText())?"English"
								:"Swahili".equalsIgnoreCase(request.getText())?"Swahili"
								:StringUtils.isBlank(contactRepo.getLanguage(waid.toString()))?"English"
								:contactRepo.getLanguage(waid.toString()))
						.build();
	
				contactRepo.save(data);
	
				List<Long> list = new ArrayList<>();
				list.add(waid);
	
				Flux.fromIterable(list)
					.map(i -> sendChatMsg(i, request))
					.subscribeOn(Schedulers.boundedElastic())
					.subscribe();
	
				String waStatus = cs.getapplicationProperty().getProperty("wa.hit.status");
				waStatus = StringUtils.isBlank(waStatus) ? "N" : waStatus.trim();
	
				if(waStatus.equalsIgnoreCase("Y")) {
	
					String url = cs.getwebserviceurlProperty().getProperty("wa.webhook");
					String auth = cs.getwebserviceurlProperty().getProperty("wa.webhook.auth");
					String method = "POST";
	
					Flux.fromIterable(list)
						.map(i -> cs.callApi(url, auth, method, request))
						.subscribeOn(Schedulers.boundedElastic())
						.subscribe();
				}
			}else {
				String timezoneHours = cs.getwebserviceurlProperty().getProperty("timezone.hours");
				String timezoneMinutes = cs.getwebserviceurlProperty().getProperty("timezone.minutes");
	
				Date sessStartTime = cs.addHours(watiDate, Integer.valueOf(timezoneHours));
				sessStartTime = cs.addMinutes(sessStartTime, Integer.valueOf(timezoneMinutes));
	
				Date sessEndTime = cs.addHours(sessStartTime, 24);
	
				
				WhatsappContactData data = WhatsappContactData.builder()
						.entry_date(new Date())
						//.entry_date(detailRepo.getCurrentDate()) // uat
						.session_end_time(sessEndTime)
						.session_start_time(sessStartTime)
						.status("Y")
						.wa_messageid(request.getId())
						.whatsappid(waid)
						.remarks("")
						.sendername(StringUtils.isBlank(request.getSenderName()) ? "" : request.getSenderName())
						.language("")
						//.sessionexpiryMsg("N")
						.build();
	
				contactRepo.save(data); 
				sendLanguageMsg(request);
			}
			
			

		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}

	private String sendChatMsg(Long waid, WebhookReq request) {
		String re = "";
		try {

			long count = watiSer.checkSessionStatus(waid);

			log.info("sendChatMsg--> count: " + count);

			if(!"00".equalsIgnoreCase(request.getText())) {
				if (count > 0) {
					
					String reply = StringUtils.isBlank(request.getText()) ? "" : request.getText().trim();
					String data = StringUtils.isBlank(request.getData()) ? "" : request.getData().trim();
					String type = StringUtils.isBlank(request.getType()) ? "" : request.getType().trim();
	
					String nxtMsgId = "";
									
					WhatsAppReq cntRes = getCntWAMsgMaster(reply);
					
					String replyMsgId = "";
					String replyIsJob = "N";
					
					if(cntRes != null) {
						
						replyMsgId = StringUtils.isBlank(cntRes.getType()) ? "" : cntRes.getType().trim();
						replyIsJob = StringUtils.isBlank(cntRes.getIsJobYN()) ? "N" : cntRes.getIsJobYN().trim();
					}
					
					LocalDate localDate = LocalDate.now().minusDays(1);
					Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
					List<WADataDetail> list = wddRepo.findByWaddPk_WaidAndEntrydateBetweenOrderByEntrydateDesc(waid,date,new Date());
					String userButtonReply=reply.replace(" ", "");
					
					if (list.size() <= 0 || StringUtils.isNotBlank(replyMsgId)) {
											
						if (StringUtils.isBlank(replyMsgId) || StringUtils.isBlank(reply) || userButtonReply.replace("\\s", "").equalsIgnoreCase(mainMenu) ||
								"Swahili".equalsIgnoreCase(reply) || "English".equalsIgnoreCase(reply) || mainMenuSwahili.equalsIgnoreCase(userButtonReply.replaceAll("\\s", "")))
								 {
								
							nxtMsgId = "COM001";
						} else {
							nxtMsgId = replyMsgId;
						}
	
						if (replyIsJob.equalsIgnoreCase("N")) {
	
							WAMessageMaster msgM = getMsgMasterContent(nxtMsgId);
	
							WAWatiReq sended_res = watiSer.sendSessMsg(msgM, waid);
	
							sended_res.setParentMsgId(msgM.getMessageid());
	
							saveMsg(sended_res);
	
						} else {
	
							WAWatiReq res = WAWatiReq.builder()
									.msg(cntRes.getMessage())
									.sessionid(request.getDisplayMobileNo())
									.waid(String.valueOf(waid))
									.wamsgId(request.getId())
									.parentMsgId(cntRes.getParentMsgId())
									.userReply(cntRes.getUserReply())
									.messageId(nxtMsgId)
									.isjobyn(cntRes.getIsJobYN())
									.build();
	
							saveMsg(res);
	
							List<WhatsappTemplateMaster> tempM = getTempMasterContent(nxtMsgId, "90016", waid);
							
							List<WhatsAppReq> reqList = setRequestList(tempM, waid);
	
							insertReqDet(reqList);
						}
					} else {
						WADataDetail wdd = list.get(0);
	
						String isJob = StringUtils.isBlank(wdd.getIsjobyn()) ? "N" : wdd.getIsjobyn();
						String isinput = "N";
						String inputValue = "";
						String requestKey = "";
						String replyMsg = "";
	
						String msgStart = cs.getwebserviceurlProperty().getProperty("start.message");
						msgStart = StringUtils.isBlank(msgStart) ? "" : msgStart;
						
						if (reply.equals("0") || userButtonReply.replace("\\s", "").equalsIgnoreCase(mainMenu)|| reply.equalsIgnoreCase("hi")
								||"Swahili".equalsIgnoreCase(reply) || "English".equalsIgnoreCase(reply) || mainMenuSwahili.equalsIgnoreCase(userButtonReply.replace("\\s", ""))
								)
						{		 
								nxtMsgId = "COM001";
						}
	
						if (isJob.equalsIgnoreCase("N") || nxtMsgId.equals("COM001")) {
	
							wdd.setUserreply(reply);
							wdd.setWausermessageid(request.getId());
	
							if (NumberUtils.isCreatable(reply) || userButtonReply.equalsIgnoreCase(previousMenu) ||
									"Swahili".equalsIgnoreCase(reply) || "English".equalsIgnoreCase(reply)
									|| preMenuSwahili.equalsIgnoreCase(userButtonReply)) {
								Long userReply=0L;
								if(userButtonReply.equalsIgnoreCase(previousMenu) || preMenuSwahili.equalsIgnoreCase(userButtonReply))
									userReply =9L;
								else if("Swahili".equalsIgnoreCase(reply) || "English".equalsIgnoreCase(reply))
									userReply=0L;
								else
									userReply = Long.valueOf(reply);
	
								if (userReply == 0) {
									nxtMsgId = "COM001";
								} else {
									Map<String, Object> map = waChatRepo.getNextMsgId(wdd.getParentmessageid(), userReply);
	
									if (map.size() > 0) {
										nxtMsgId = map.get("MESSAGEID") == null ? "" : map.get("MESSAGEID").toString();
										isJob = map.get("ISJOBYN") == null ? "N" : map.get("ISJOBYN").toString();
										replyMsg = map.get("DESCRIPTION") == null ? "" : map.get("DESCRIPTION").toString();
										
										if (StringUtils.isNotBlank(nxtMsgId)) {
	
											Map<String, Object> map2 = waChatRepo.getChatInputs(nxtMsgId);
	
											isinput = map2.get("ISINPUT") == null ? "N" : map2.get("ISINPUT").toString();
											inputValue = map2.get("INPUT_VALUE") == null ? ""
													: map2.get("INPUT_VALUE").toString();
											requestKey = map2.get("REQUEST_KEY") == null ? ""
													: map2.get("REQUEST_KEY").toString();
										}
									}
								}
	
								nxtMsgId = StringUtils.isBlank(nxtMsgId) ? wdd.getParentmessageid() : nxtMsgId;
	
							} else if (StringUtils.isBlank(nxtMsgId)) {
								nxtMsgId = wdd.getParentmessageid();
							}
	
							replyMsg = StringUtils.isBlank(replyMsg) ? reply : replyMsg;
	
							wdd.setIsjobyn(isJob);
							wdd.setUsermessageid(nxtMsgId);
							wdd.setUserreply_msg(replyMsg);
							wdd.setIsinput(isinput);
							wdd.setInput_value(inputValue);
							wdd.setRequest_key(requestKey);
	
							wddRepo.save(wdd);
	
							if (isJob.equalsIgnoreCase("N") || nxtMsgId.equals("COM001")) {
								
								WAMessageMaster msgM = getMsgMasterContent(nxtMsgId);
	
								WAWatiReq sended_res = watiSer.sendSessMsg(msgM, waid);
	
								sended_res.setParentMsgId(msgM.getMessageid());
	
								saveMsg(sended_res);
							} else {
								List<WhatsappTemplateMaster> tempM = getTempMasterContent(nxtMsgId, "90016", waid);
								
								List<WhatsAppReq> reqList = setRequestList(tempM, waid);
	
								insertReqDet(reqList);
							}
						}else if((isJob.equalsIgnoreCase("Y")) && 
								(userButtonReply.replace("\\s", "").equalsIgnoreCase(previousMenu) || preMenuSwahili.equalsIgnoreCase(userButtonReply) || "9".equals(userButtonReply))) {
							  
							WAMessageMaster msgM  = getMsgMasterContent(wdd.getParentmessageid());
													  
							WAWatiReq sended_res = watiSer.sendSessMsg(msgM, waid);
							  
							sended_res.setParentMsgId(wdd.getParentmessageid());					
							  
							saveMsg(sended_res); 				
						
						}else {
							String msgid = StringUtils.isBlank(wdd.getUsermessageid()) ? "" : wdd.getUsermessageid();
	
							List<WhatsappRequestDetail> reqDetList = getWaReqDet(waid, "Y", msgid);
	
							log.info("sendChatMsg--> reqDetList size: " + reqDetList.size());
	
							List<Long> waidList = new ArrayList<>();
	
							waidList.add(waid);
	
							if (reqDetList.size() > 0) {
								
								WhatsappRequestDetail reqDet = reqDetList.get(0);
								
								WhatsappRequestDetailPK detPk = reqDet.getReqDetPk();
	
								String isReply = StringUtils.isBlank(reqDet.getIsreplyyn()) ? "N" : reqDet.getIsreplyyn();
								String isDocUpl = StringUtils.isBlank(reqDet.getIsdocuplyn()) ? "N"
										: reqDet.getIsdocuplyn();
	
								String isSkip = StringUtils.isBlank(reqDet.getIsskipyn()) ? "N" : reqDet.getIsskipyn();
								String isApiCall = StringUtils.isBlank(reqDet.getIsapicall()) ? "N" : reqDet.getIsapicall();
								String isValidationApi = StringUtils.isBlank(reqDet.getIsvalidationapi()) ? "N" : reqDet.getIsvalidationapi();
								String isValid = StringUtils.isBlank(reqDet.getIsvalid()) ? "N" : reqDet.getIsvalid();
										
								String IsResYn =StringUtils.isBlank(reqDet.getIsReponseYn()) ? "N" : reqDet.getIsReponseYn();
								String IsResYnSent =StringUtils.isBlank(reqDet.getIsResponseYnSent()) ? "N" : reqDet.getIsResponseYnSent();
								String isResSaveApi = StringUtils.isBlank(reqDet.getIsResSaveApi()) ? "N" : reqDet.getIsResSaveApi();
	
								Long stageCode = detPk.getCurrentstage();
								Long subStageCode = detPk.getCurrentsubstage();
	
								WhatsappTemplateMaster tempM = getTempMasterStageContent(msgid, "90016", waid, stageCode,
										subStageCode);
								
								String isskipped = "N";
								String isProcessComp = "N";
	
								if(IsResYn.equals("Y") && "Y".equals(IsResYnSent) )
									reqDet.setUserreply(reqDet.getUserreply());
								else
									reqDet.setUserreply(reply);
								
								reqDet.setWausermessageid(request.getId());
	
								if (isSkip.equalsIgnoreCase("Y") && (reply.equals("99") || reply.equalsIgnoreCase("Skip") ||  reply.equalsIgnoreCase("Skip Image")) && isReply.equalsIgnoreCase("Y")) {
									isskipped = "Y";
								}
															
								String commonurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api");
								String msgurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api.sendSessionMessage");
								String fileurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api.sendSessionFile");
								String button_msg_url = cs.getwebserviceurlProperty().getProperty("whatsapp.api.button");
								
								String auth = cs.getwebserviceurlProperty().getProperty("whatsapp.auth");
	
								OkHttpClient okhttp = new OkHttpClient.Builder()
										.readTimeout(30, TimeUnit.SECONDS)
										.build();
	
								RequestBody body = RequestBody.create(new byte[0], null);
	
								detailRepo.save(reqDet);
								
								
								if("Y".equalsIgnoreCase(isResSaveApi) && "Y".equalsIgnoreCase(isApiCall) &&
										"N".equalsIgnoreCase(isskipped) && !"location".equalsIgnoreCase(request.getType())) {
									
									isValid = watiApiCall.sendValidationMsg(okhttp, body, commonurl, msgurl, fileurl, auth, reqDet, String.valueOf(waid), new Date(),tempM);
	
								}
								
								else if (isApiCall.equalsIgnoreCase("Y") && isValidationApi.equalsIgnoreCase("Y")
										&& isskipped.equalsIgnoreCase("N") && "N".equalsIgnoreCase(isDocUpl) && !"location".equalsIgnoreCase(request.getType())) {
	
									isValid = watiApiCall.sendValidationMsg(okhttp, body, commonurl, msgurl, fileurl, auth, reqDet, String.valueOf(waid), new Date(),tempM);
	
								} 
								
								else if(IsResYn.equals("Y") && isskipped.equalsIgnoreCase("N") && isApiCall.equalsIgnoreCase("Y") && "N".equals(IsResYnSent) && !"location".equalsIgnoreCase(request.getType())) {
									
									isValid = watiApiCall.sendIsResYnMsg(okhttp, body, commonurl, button_msg_url, fileurl, auth, reqDet, String.valueOf(waid), new Date());
	                                
								}else if(IsResYn.equals("Y") && "Y".equals(IsResYnSent) && "No".equalsIgnoreCase(reply) && !"location".equalsIgnoreCase(request.getType())) {
									
									Map<String,Object> map =waChatRepo.getParentMessageId(reqDet.getRemarks());
									
									String parentMsgId =map.get("PARENTMESSAGEID")==null?"":map.get("PARENTMESSAGEID").toString();
									
									WAMessageMaster msgM = getMsgMasterContent(parentMsgId);
	
									WAWatiReq sended_res = watiSer.sendSessMsg(msgM, waid);
	
									sended_res.setParentMsgId(msgM.getMessageid());
	
									saveMsg(sended_res);
									
									isValid ="Y";
	
								}else {
									
									isValid ="Y";
								}
								
								if(isValid.equalsIgnoreCase("Y")) {
									isProcessComp = "Y";
								}
								
	
								reqDet.setIsprocesscompleted(isProcessComp);
								reqDet.setIsskipped(isskipped);
								reqDet.setIsvalid(isValid);
								if (isDocUpl.equalsIgnoreCase("Y") && StringUtils.isNotBlank(data) &&isApiCall.equalsIgnoreCase("N")
										&& type.equalsIgnoreCase("image") && isskipped.equalsIgnoreCase("N")) {
	
									//String fileurls[] = data.split("fileName=");
	
									reqDet.setWa_userfilepath(data);
	
									String localPath = watiSer.storeWAFile(data);
									
									reqDet.setUserreply(localPath.replace("\\", "//"));
									reqDet.setLocwa_userfilepath(localPath);
									
								}else if (isDocUpl.equalsIgnoreCase("Y") && StringUtils.isNotBlank(data) &&isApiCall.equalsIgnoreCase("Y")
										&&(type.equalsIgnoreCase("image") || type.equalsIgnoreCase("document")|| type.equalsIgnoreCase("video"))&&isskipped.equalsIgnoreCase("N")) {
	
									//String fileurls[] = data.split("fileName=");
									motorImpl.saveClaimDocument(data,tempM,type,waid,reqDet);
									reqDet.setIsprocesscompleted("Y");
									
									
								}else if (isDocUpl.equalsIgnoreCase("Y")
										&& (StringUtils.isBlank(data) || !type.equalsIgnoreCase("image"))
										&& isskipped.equalsIgnoreCase("N")) {
	
									reqDet.setValidationmessage(tempM.getErrorrespstring());
									reqDet.setIsprocesscompleted("N");
									
									watiApiCall.sendDocValidationMsg(okhttp, body, commonurl, msgurl, fileurl, auth, reqDet,
											String.valueOf(waid), new Date(),tempM);
								} else if("location".equalsIgnoreCase(request.getType())) {
									
									reqDet.setUserreply(request.getData());
									reqDet.setIsprocesscompleted("Y");

								}
								
								
	
								detailRepo.save(reqDet);
	
								nxtMsgId = reqDet.getRemarks();
	
								if("Y".equals(reqDet.getIsprocesscompleted())) {
									Flux.fromIterable(waidList)
										.map(i -> watiSer.callSendSessionMsg(i, msgid))
										.subscribeOn(Schedulers.boundedElastic())
										.subscribe();
									}
								
								
								}else {
	
								Flux.fromIterable(waidList)
								.map(i -> watiSer.callSendSessionMsg(i, msgid))
								.subscribeOn(Schedulers.boundedElastic())
								.subscribe();
								
								
							}
						}	
					}
				
				}	
			}else {
				sendLanguageMsg(request);	
			}
		} catch (Exception e) {
			log.error(e);
		}
		
		return re;
	}


	private WAMessageMaster getMsgMasterContent(String msgid) {
		try {

			/*QWAMessageMaster qwamsgM = QWAMessageMaster.wAMessageMaster;
			
			WAMessageMaster wamsgM = jpa.selectFrom(qwamsgM)
					.where(qwamsgM.messageid.eq(msgid)
							.and(qwamsgM.status.equalsIgnoreCase("Y")))
					.fetchOne();*/
			
			WAMessageMaster wamsgM = wamsgMRepo.getMsgCont(msgid);

			return wamsgM;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}
	
	private WhatsAppReq getCntWAMsgMaster(String msgId) {
		try {

			WhatsAppReq response = new WhatsAppReq();

			QWAMessageMaster qwamsgM = QWAMessageMaster.wAMessageMaster;

			Predicate pred = qwamsgM.commonmsgid.eq(msgId)
					.and(qwamsgM.status.equalsIgnoreCase("Y"))
					.and(qwamsgM.iscommonmsg.equalsIgnoreCase("Y"));

			List<WAMessageMaster> list = (List<WAMessageMaster>) wamsgMRepo.findAll(pred, qwamsgM.entrydate.desc()) ;

			String replyMsgId = "";

			if (list.size() > 0) {
				WAMessageMaster msgM = list.get(0);

				replyMsgId = msgM.getMessageid();
				
				response = WhatsAppReq.builder()
						.type(replyMsgId)
						.isJobYN("N")
						.build();
			} else {
				
				QWAChatRecipientMaster qChat = QWAChatRecipientMaster.wAChatRecipientMaster;
				
				BooleanExpression and = qChat.status.equalsIgnoreCase("Y")
						.and(qChat.isjobyn.equalsIgnoreCase("Y"))
						.and(qChat.iscommonmsg.equalsIgnoreCase("Y"))
						.and(qChat.commonmsgid.eq(msgId));

				List<WAChatRecipientMaster> listChat = (List<WAChatRecipientMaster>) waChatRepo.findAll(and, qChat.entrydate.desc());

				if(listChat.size() > 0) {
					
					WAChatRecipientMaster chat = listChat.get(0);
					WAChatRecipientMasterPK chatPk = chat.getChatPk();

					replyMsgId = chatPk.getMessageid();

					response = WhatsAppReq.builder()
							.type(replyMsgId)
							.isJobYN(chat.getIsjobyn())
							.message(chat.getDescription())
							.parentMsgId(chatPk.getParentmessageid())
							.userReply(chat.getUseroptted_messageid() == null ? "" : String.valueOf(chat.getUseroptted_messageid()))
							.build();
				}
			}

			return response;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	private List<WhatsappTemplateMaster> getTempMasterContent(String msgid, String agencycode, Long waid) {
		try {

			QWhatsappTemplateMaster qtempM = QWhatsappTemplateMaster.whatsappTemplateMaster;
			QWhatsappTemplateMasterPK qtempMPk = qtempM.tempMasterPk;

			List<WhatsappTemplateMaster> tempM = jpa
					.selectFrom(qtempM)
					.where(qtempM.remarks.eq(msgid)
							.and(qtempM.status.equalsIgnoreCase("Y"))
							.and(qtempMPk.agencycode.eq(agencycode))
							.and(qtempM.ischatyn.equalsIgnoreCase("Y"))
							)
					.orderBy(qtempM.stage_order.asc())
					.fetch();

			return tempM;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
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

	private List<WhatsappRequestDetail> getWaReqDet(Long waid, String isJobyn, String msgid) {
		try {

			QWhatsappRequestDetail qreqDet = QWhatsappRequestDetail.whatsappRequestDetail;

			List<WhatsappRequestDetail> fetch = jpa
					.selectFrom(qreqDet)
					.where(qreqDet.whatsappid.eq(waid)
							.and(qreqDet.isjobyn.equalsIgnoreCase(isJobyn))
							.and(qreqDet.remarks.isNotNull())
							//.and(qreqDet.requestkey.isNotNull())
							.and(qreqDet.isprocesscompleted.equalsIgnoreCase("N"))
							.and(qreqDet.issent.equalsIgnoreCase("Y"))
							.and(qreqDet.remarks.equalsIgnoreCase(msgid)))
					.orderBy(qreqDet.stage_order.desc())
					.fetch();

			return fetch;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	private String saveMsg(WAWatiReq res) {
		try {
			
			WADataDetailPK wddPk = WADataDetailPK.builder()
					.waid(Long.valueOf(res.getWaid()))
					.wamessageid(res.getWamsgId())
					.build();

			WADataDetail wdd = WADataDetail.builder()
					.apivalidationresponse(null)
					.entrydate(new Date())
					.messagecontent(res.getMsg())
					.parentmessageid(res.getParentMsgId())
					.usermessageid(res.getMessageId())
					.userreply(res.getUserReply())
					.isjobyn(res.getIsjobyn())
					.wausermessageid("")
					.remarks(null)
					.sessionid(res.getSessionid())
					.status("Y")
					.userreply(null)
					.waddPk(wddPk)
					.build();

			wddRepo.save(wdd);
			
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	private List<WhatsAppReq> setRequestList(List<WhatsappTemplateMaster> list, Long waid) {
		try {

			List<WhatsAppReq> resList = new ArrayList<>();

			List<String> remarks = wtmRepo.getMobCode();

			WASaveReq waRes = getWANoCode(waid, remarks);

			String whatsappCode = waRes.getWhatsAppCode();
			String whatsappno = waRes.getWhatsAppno();
			
			String language=contactRepo.getLanguage(waid.toString());

			for (WhatsappTemplateMaster tempM : list) {

				WhatsappTemplateMasterPK tempMPk = tempM.getTempMasterPk();

				String msgEn = StringUtils.isBlank(tempM.getMessage_content_en()) ? ""
						: tempM.getMessage_content_en().trim();
				String msgAr = StringUtils.isBlank(tempM.getMessage_content_ar()) ? ""
						: tempM.getMessage_content_ar().trim();

				String regardsEn = StringUtils.isBlank(tempM.getMessage_regards_en()) ? ""
						: tempM.getMessage_regards_en().trim();
				String regardsAr = StringUtils.isBlank(tempM.getMessage_regards_ar()) ? ""
						: tempM.getMessage_regards_ar().trim();
				
				//String templateMsg =StringUtils.isBlank(tempM.getIsTemplateMsg())?"N":tempM.getIsTemplateMsg();

				String imageName =StringUtils.isBlank(tempM.getStage_desc())?"":tempM.getStage_desc();

				//String msg = msgEn + "\\n" + regardsEn + (StringUtils.isBlank(msgAr) ? "" : (msgAr + "\\n" + regardsAr));
				
				String isResYn =StringUtils.isBlank(tempM.getIsReponseYn())?"N": tempM.getIsReponseYn(); 
				
				//if(StringUtils.isNotBlank(tempM.getRequeststring()) && tempM.getRequeststring().contains("100")) {

			
				String msg ="";
				if(language.equalsIgnoreCase("English")) {
					 msg =msgEn + "\\n" + regardsEn;
				}else if(language.equalsIgnoreCase("Swahili")) {
					 msg =msgAr + "\\n" + regardsAr ;
				}
				
				msg = msg.trim();
				
				WhatsAppReq req = WhatsAppReq.builder()
						.agencycode(tempMPk.getAgencycode())
						.currentStage(String.valueOf(tempMPk.getStagecode()))
						.filePath(tempM.getFile_path())
						.fileYN(tempM.getFile_yn())
						.message(msg)
						.productid(String.valueOf(tempMPk.getProductid()))
						.quoteNo(String.valueOf(waid))
						.stageOrder(String.valueOf(tempM.getStage_order()))
						.subStage(String.valueOf(tempMPk.getStagesubcode()))
						.type(tempM.getRemarks())
						.whatsappCode(whatsappCode)
						.whatsappno(whatsappno)
						.isJobYN(StringUtils.isBlank(tempM.getIschatyn()) ? "N" : tempM.getIschatyn())
						.isreplyyn(StringUtils.isBlank(tempM.getIsreplyyn()) ? "N" : tempM.getIsreplyyn())
						.isapicall(StringUtils.isBlank(tempM.getIsapicall()) ? "N" : tempM.getIsapicall())
						.requestkey(StringUtils.isBlank(tempM.getRequestkey()) ? "" : tempM.getRequestkey())
						.isskipyn(StringUtils.isBlank(tempM.getIsskipyn()) ? "N" : tempM.getIsskipyn())
						.isdocuplyn(StringUtils.isBlank(tempM.getIsdocuplyn()) ? "N" : tempM.getIsdocuplyn())
						.isValidationApi(StringUtils.isBlank(tempM.getIsvalidationapi()) ? "N" : tempM.getIsvalidationapi())
						.stageDesc(imageName)
						.isResponseYn(isResYn)
						.isResponseYnSent("N")
						.isResSaveApi(StringUtils.isBlank(tempM.getIsResSaveApi()) ? "N" : tempM.getIsResSaveApi())
						.isResMsg(StringUtils.isBlank(tempM.getIsResMsg()) ? "N" : tempM.getIsResMsg())
						//.isButtonMsgYn(StringUtils.isBlank(tempM.getIsButtonMsg())?"N":tempM.getIsButtonMsg())
						.isResMsgApi(StringUtils.isBlank(tempM.getIsResMsgApi())?"":tempM.getIsResMsgApi())
						//.isTemplateMsg(templateMsg)
						.formPageUrl(StringUtils.isBlank(tempM.getFormpageUrl())?"":tempM.getFormpageUrl())
						.formPageYn(StringUtils.isBlank(tempM.getFormpageYn())?"N":tempM.getFormpageYn())
						.build();

				resList.add(req);
			}
			//}
		
			return resList;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	@Override
	public String sendSessExpMsg() {
		try {

			/*QWhatsappContactData qContacts = QWhatsappContactData.whatsappContactData;

			JPAQuery<WhatsappContactData> jpaContacts = jpa.selectFrom(qContacts);

			DateTimeExpression<Date> currentTimestamp = Expressions.currentTimestamp();

			DateTimeExpression<Date> timebef30 = Expressions.dateTimeOperation(Date.class, Ops.DateTimeOps.ADD_MINUTES,
					qContacts.session_end_time,currentTimestamp);

			List<Long> list = jpaContacts.select(qContacts.whatsappid)
					.where(qContacts.status.equalsIgnoreCase("Y")
							.and(currentTimestamp.between(timebef30, qContacts.session_end_time)))
					.fetch();*/

			List<Long> list = contactRepo.getWhatsAppIds();

			log.info("sendSessExpMsg--> list: " + list.size());

			if(list.size() > 0) {

				List<String> remarks = wtmRepo.getMobCode();

				Flux.fromIterable(list)
					.map(i -> callWAApi(i, remarks))
					.subscribeOn(Schedulers.boundedElastic())
					.subscribe();
			}

		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}

	private String callWAApi(Long waid, List<String> remarks) {
		try {

			String waids = waid.toString();

			WASaveReq waRes = getWANoCode(waid, remarks);
			
			String whatsappCode = waRes.getWhatsAppCode();
			String whatsappno = waRes.getWhatsAppno();

			if (StringUtils.isNotBlank(whatsappCode) && StringUtils.isNotBlank(whatsappno)) {

				WASaveReq req = WASaveReq.builder()
						.type("SESS_EXPMSG")
						.whatsAppCode(whatsappCode)
						.whatsAppno(whatsappno)
						.productid("99999")
						.quoteNo(waids)
						.build();

				callWhatsAppSaveApi(req);
			}

		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}

	private WASaveReq getWANoCode(Long waid, List<String> remarks) {
		try {

			String waids = waid.toString();

			String whatsappCode = "";
			String whatsappno = "";

			for (String remark : remarks) {
				String tempRem = waids.substring(0, remark.length());

				if (remark.equals(tempRem)) {

					whatsappCode = remark;
					whatsappno = waids.substring(remark.length(), waids.length());

					break;
				}
			}

			WASaveReq res = WASaveReq.builder()
					.whatsAppCode(whatsappCode)
					.whatsAppno(whatsappno)
					.build();

			return res;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	private String callWhatsAppSaveApi(WASaveReq request) {
		try {

			String url = cs.getwebserviceurlProperty().getProperty("whatsapp.save");
			String auth = cs.getwebserviceurlProperty().getProperty("whatsapp.save.auth");

			ResponseEntity<String> resEnt = null;

			RestTemplate restTemplate = new RestTemplate();

			HttpHeaders headers = new HttpHeaders();

			headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", auth);

			HttpEntity<WASaveReq> entityReq = new HttpEntity<>(request, headers);

			resEnt = restTemplate.postForEntity(url, entityReq, String.class);

			String res = resEnt.getBody();

			log.info("callWhatsAppSaveApi--> response: " + res);

		} catch (HttpClientErrorException e) {
			log.error(e);
			cs.reqPrint(e.getResponseBodyAsString());
		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}

	@Override
	public List<WAQuoteRes> getWAMsgDet(WAQuoteReq request) {
		try {

			List<WAQuoteRes> response = new ArrayList<>();

			String whatsappid = StringUtils.isBlank(request.getWhatsappid()) ? "" : request.getWhatsappid();
			String entryDate = StringUtils.isBlank(request.getEntryDate()) ? "" : request.getEntryDate();

			log.info("getWAMsgDet--> whatsappid: " + whatsappid + " entryDate: " + entryDate);

			List<Map<String, Object>> list = wddRepo.getMsgDet(whatsappid, entryDate);

			for (Map<String, Object> map : list) {
				
				String isChat = map.get("ISCHAT") == null ? "N" : map.get("ISCHAT").toString();
				String answerMsg = map.get("USERREPLY") == null ? "" : map.get("USERREPLY").toString();
				String entryDateWTime = map.get("ENTRYDATE") == null ? "" : map.get("ENTRYDATE").toString();

				entryDateWTime = cs.formatdatewithtime4(entryDateWTime);

				WAQuoteRes res = WAQuoteRes.builder()
						.answerMsg(answerMsg)
						.entryDate(entryDateWTime)
						.fileYN(map.get("FILE_YN") == null ? "N" : map.get("FILE_YN").toString())
						.isDocUplYN(map.get("ISDOCUPLYN") == null ? "N" : map.get("ISDOCUPLYN").toString())
						.isJobYN(map.get("ISJOBYN") == null ? "N" : map.get("ISJOBYN").toString())
						.isReplyYn(map.get("ISREPLYYN") == null ? "" : map.get("ISREPLYYN").toString())
						.questionMsg(map.get("MESSAGE") == null ? "" : map.get("MESSAGE").toString())
						.receivedFile(map.get("LOCWA_USERFILEPATH") == null ? "" : map.get("LOCWA_USERFILEPATH").toString())
						.sendedFile(map.get("FILE_PATH") == null ? "" : map.get("FILE_PATH").toString())
						.validationMessage(map.get("VALMSG") == null ? "" : map.get("VALMSG").toString())
						.isChatYN(isChat)
						.build();

				response.add(res);
			}

			return response;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}
	
	private String sendLanguageMsg(WebhookReq req) {
		String buttonMessage="";
		String request="";
		try {		

			String message ="*Alliance Insurance Corporate Limited*\n\nPlease choose your language";
				
				
			Map<String,Object> button_req = new HashMap<String, Object>();
			List<Map<String,Object>> buttonList =new ArrayList<>();
			Map<String,Object> button_text_1 = new HashMap<String, Object>();
			button_text_1.put("type", "reply");

		
			Map<String,Object> reply_1 = new HashMap<String, Object>();
			reply_1.put("id", "Swahili");
			reply_1.put("title", "Swahili");
			button_text_1.put("reply", reply_1);
			//buttonList.add(button_text_1);
			
			Map<String,Object> button_text_2 = new HashMap<String, Object>();
			button_text_2.put("type", "reply");

			
			Map<String,Object> reply_2 = new HashMap<String, Object>();
			reply_2.put("id", "English");
			reply_2.put("title", "English");
			button_text_2.put("reply", reply_2);
			buttonList.add(button_text_2);
			
		
			Map<String,Object> body_text =new HashMap<String, Object>();
			body_text.put("text", message);
		
			Map<String,Object> actions =new HashMap<String, Object>();
			actions.put("buttons", buttonList);
		
			Map<String,Object> button_interactive =new HashMap<String, Object>();
			button_interactive.put("type", "button");
			button_interactive.put("body", body_text);
			button_interactive.put("action", actions);
		
		
		button_req.put("messaging_product", "whatsapp");
		button_req.put("recipient_type", "individual");
		button_req.put("to", req.getWaId());
		button_req.put("type", "interactive");
		button_req.put("interactive", button_interactive);
		
		buttonMessage =cs.reqPrint(button_req);
		
		String meta_message_api=cs.getwebserviceurlProperty().getProperty("meta.message.api");
		String meta_message_api_auth=cs.getwebserviceurlProperty().getProperty("meta.message.api.auth");
	
		okhttp3.MediaType contentType =okhttp3.MediaType.parse("application/json");
		RequestBody body =RequestBody.create(buttonMessage,contentType);
		
		Request requestBuiler = new Request.Builder()
				.url(meta_message_api)
				.addHeader("Authorization", meta_message_api_auth)
				.post(body)
				.build();

		OkHttpClient okhttp = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();

		Response response = okhttp.newCall(requestBuiler).execute();

		String responseString = response.body().string();
		
		System.out.println(responseString);
		}catch (Exception e) {
			log.error(e);
		}
		
		return buttonMessage;
		
	}
	
	


}
