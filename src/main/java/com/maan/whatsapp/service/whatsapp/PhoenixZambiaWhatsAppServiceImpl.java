package com.maan.whatsapp.service.whatsapp;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.maan.whatsapp.config.exception.WhatsAppValidationException;
import com.maan.whatsapp.entity.master.QWAChatRecipientMaster;
import com.maan.whatsapp.entity.master.QWAMessageMaster;
import com.maan.whatsapp.entity.master.QWhatsappTemplateMaster;
import com.maan.whatsapp.entity.master.QWhatsappTemplateMasterPK;
import com.maan.whatsapp.entity.master.WAChatRecipientMaster;
import com.maan.whatsapp.entity.master.WAChatRecipientMasterPK;
import com.maan.whatsapp.entity.master.WAMessageMaster;
import com.maan.whatsapp.entity.master.WhatsappTemplateMaster;
import com.maan.whatsapp.entity.master.WhatsappTemplateMasterPK;
import com.maan.whatsapp.entity.whatsapp.PhoenixUserDataDetails;
import com.maan.whatsapp.entity.whatsapp.QWhatsappRequestDetail;
import com.maan.whatsapp.entity.whatsapp.WADataDetail;
import com.maan.whatsapp.entity.whatsapp.WADataDetailPK;
import com.maan.whatsapp.entity.whatsapp.WhatsappContactData;
import com.maan.whatsapp.entity.whatsapp.WhatsappRequestDetail;
import com.maan.whatsapp.entity.whatsapp.WhatsappRequestDetailPK;
import com.maan.whatsapp.insurance.UserSession;
import com.maan.whatsapp.insurance.ZambiaInsuranceServiceImpl;
import com.maan.whatsapp.repository.master.WAChatRecipientMasterRepo;
import com.maan.whatsapp.repository.master.WAMessageMasterRepo;
import com.maan.whatsapp.repository.master.WhatsappTemplateMasterRepo;
import com.maan.whatsapp.repository.whatsapp.PhoenixUserDataDetailsRepo;
import com.maan.whatsapp.repository.whatsapp.WADataDetailRepo;
import com.maan.whatsapp.repository.whatsapp.WhatsappContactDataRepo;
import com.maan.whatsapp.repository.whatsapp.WhatsappRequestDetailRepo;
import com.maan.whatsapp.request.wati.getcont.WebhookReq;
import com.maan.whatsapp.request.whatsapp.WASaveReq;
import com.maan.whatsapp.request.whatsapp.WAWatiReq;
import com.maan.whatsapp.request.whatsapp.WhatsAppReq;
import com.maan.whatsapp.service.common.CommonService;
import com.maan.whatsapp.service.motor.MotorServiceImpl;
import com.maan.whatsapp.service.wati.ZambiaWatiApiCall;
import com.maan.whatsapp.service.wati.ZambiaWatiService;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
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
public class PhoenixZambiaWhatsAppServiceImpl implements PhoenixZambiaWhatsAppService{

private Logger log = LogManager.getLogger(getClass());
	
	@Autowired
	private CommonService cs;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private ZambiaWatiApiCall zambWatiApiCall;
	
	@Autowired 
	private MotorServiceImpl motorImpl;
	
	@Autowired
	private ZambiaWatiService zambWatiSer;
	
	@Autowired
	private WhatsappContactDataRepo contactRepo;
	
	@Autowired
	private WADataDetailRepo wddRepo;
	
	@Autowired
	private WAChatRecipientMasterRepo waChatRepo;
	
	@Autowired
	private WhatsappRequestDetailRepo detailRepo;
	
	@Autowired
	private WAMessageMasterRepo wamsgMRepo;
	
	@Autowired
	private WhatsappTemplateMasterRepo wtmRepo;
	
	@Autowired
	private PhoenixUserDataDetailsRepo userDataRepo;
	
	@Autowired
	private ZambiaInsuranceServiceImpl zambiaInsuranceService;
	
	@Autowired
	private Gson objectPrint;
	
	@Autowired
	private JPAQueryFactory jpa;
	
	@Value("${askeva.template.api.zambia}")
	private String askeveApiforZambia;
	
	@Value("${main.menu.button}")
	private String mainMenu;
	
	@Value("${main.menu.button.swahili}")
	private String mainMenuSwahili;
	
	@Value("${previous.menu.button}")
	private String previousMenu;
	
	@Value("${previous.menu.button.swahili}")
	private String preMenuSwahili;

	
	@Override
	public String zambiaWebhookRes(WebhookReq webhookReq) {
		try {

			log.info("Zambia webhookRes--> request: ");
			cs.reqPrint(webhookReq);
 
			List<WebhookReq> list = new ArrayList<WebhookReq>();
			list.add(webhookReq);

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

			long count = zambWatiSer.checkSessionStatus(waid);

			if (count > 0) {
				String timezoneHours = cs.getwebserviceurlProperty().getProperty("timezone.hours");
				String timezoneMinutes = cs.getwebserviceurlProperty().getProperty("timezone.minutes");

				Date sessStartTime = cs.addHours(watiDate, Integer.valueOf(timezoneHours));
				sessStartTime = cs.addMinutes(sessStartTime, Integer.valueOf(timezoneMinutes));

				Date sessEndTime = cs.addHours(sessStartTime, 24);

				WhatsappContactData data = WhatsappContactData.builder().entry_date(new Date())
						.session_end_time(sessEndTime).session_start_time(sessStartTime).status("Y")
						.wa_messageid(request.getId()).whatsappid(waid).remarks("")
						.sendername(StringUtils.isBlank(request.getSenderName()) ? "" : request.getSenderName())
						.language("English".equalsIgnoreCase(request.getText()) ? "English"
								// :"Swahili".equalsIgnoreCase(request.getText())?"Swahili"
								: StringUtils.isBlank(contactRepo.getLanguage(waid.toString())) ? "English"
										: contactRepo.getLanguage(waid.toString()))
						.build();

				contactRepo.save(data);

				List<Long> list = new ArrayList<>();
				list.add(waid);

				Flux.fromIterable(list).map(i -> sendChatMsg(i, request)).subscribeOn(Schedulers.boundedElastic())
						.subscribe();

				String waStatus = cs.getapplicationProperty().getProperty("wa.hit.status");
				waStatus = StringUtils.isBlank(waStatus) ? "N" : waStatus.trim();

				if (waStatus.equalsIgnoreCase("Y")) {
					String url = cs.getwebserviceurlProperty().getProperty("wa.webhook");
					String auth = cs.getwebserviceurlProperty().getProperty("wa.webhook.auth");
					String method = "POST";

					Flux.fromIterable(list).map(i -> cs.callApi(url, auth, method, request))
							.subscribeOn(Schedulers.boundedElastic()).subscribe();
				}
			} else {
				String timezoneHours = cs.getwebserviceurlProperty().getProperty("timezone.hours");
				String timezoneMinutes = cs.getwebserviceurlProperty().getProperty("timezone.minutes");

				Date sessStartTime = cs.addHours(watiDate, Integer.valueOf(timezoneHours));
				sessStartTime = cs.addMinutes(sessStartTime, Integer.valueOf(timezoneMinutes));

				Date sessEndTime = cs.addHours(sessStartTime, 24);

				WhatsappContactData data = WhatsappContactData.builder().entry_date(new Date())
						// .entry_date(detailRepo.getCurrentDate()) // uat
						.session_end_time(sessEndTime).session_start_time(sessStartTime).status("Y")
						.wa_messageid(request.getId()).whatsappid(waid).remarks("")
						.sendername(StringUtils.isBlank(request.getSenderName()) ? "" : request.getSenderName())
						.language("")
						// .sessionexpiryMsg("N")
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
			long count = zambWatiSer.checkSessionStatus(waid);

			log.info("sendChatMsg--> count: " + count);

			if (!"00".equalsIgnoreCase(request.getText())) {
				if (count > 0) {

					String reply = StringUtils.isBlank(request.getText()) ? "" : request.getText().trim();
					String data = StringUtils.isBlank(request.getData()) ? "" : request.getData().trim();
					String type = StringUtils.isBlank(request.getType()) ? "" : request.getType().trim();

					String nxtMsgId = "";

					WhatsAppReq cntRes = getCntWAMsgMaster(reply);

					String replyMsgId = "";
					String replyIsJob = "N";

					if (cntRes != null) {

						replyMsgId = StringUtils.isBlank(cntRes.getType()) ? "" : cntRes.getType().trim();
						replyIsJob = StringUtils.isBlank(cntRes.getIsJobYN()) ? "N" : cntRes.getIsJobYN().trim();
					}

					LocalDate localDate = LocalDate.now().minusDays(1);
					Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
					List<WADataDetail> list = wddRepo.findByWaddPk_WaidAndEntrydateBetweenOrderByEntrydateDesc(waid,
							date, new Date());
					String userButtonReply = reply.replace(" ", "");

					if (list.size() <= 0 || StringUtils.isNotBlank(replyMsgId)) {
						if (StringUtils.isBlank(replyMsgId) || StringUtils.isBlank(reply)
								|| userButtonReply.replace("\\s", "").equalsIgnoreCase(mainMenu)
								|| "Swahili".equalsIgnoreCase(reply) || "English".equalsIgnoreCase(reply)
								|| mainMenuSwahili.equalsIgnoreCase(userButtonReply.replaceAll("\\s", ""))) {

							nxtMsgId = "ZMB001";//MZB001   COM001
						} else {
							nxtMsgId = replyMsgId;
						}
						if (replyIsJob.equalsIgnoreCase("N")) {
							WAMessageMaster msgM = getMsgMasterContent(nxtMsgId);

							WAWatiReq sended_res = zambWatiSer.sendSessMsg(msgM, waid);

							sended_res.setParentMsgId(msgM.getMessageid());

							saveMsg(sended_res);
						} else {
							WAWatiReq res = WAWatiReq.builder().msg(cntRes.getMessage())
									.sessionid(request.getDisplayMobileNo()).waid(String.valueOf(waid))
									.wamsgId(request.getId()).parentMsgId(cntRes.getParentMsgId())
									.userReply(cntRes.getUserReply()).messageId(nxtMsgId).isjobyn(cntRes.getIsJobYN())
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

						if (reply.equals("0") || userButtonReply.replace("\\s", "").equalsIgnoreCase(mainMenu)
								|| reply.equalsIgnoreCase("hi") || "Swahili".equalsIgnoreCase(reply)
								|| "English".equalsIgnoreCase(reply)
								|| mainMenuSwahili.equalsIgnoreCase(userButtonReply.replace("\\s", ""))) {
							nxtMsgId = "ZMB001";//MZB001   COM001
						}

						if (isJob.equalsIgnoreCase("N") || nxtMsgId.equals("ZMB001")) {   //COM001
							wdd.setUserreply(reply);
							wdd.setWausermessageid(request.getId());

							if (NumberUtils.isCreatable(reply) || userButtonReply.equalsIgnoreCase(previousMenu)
									|| "Swahili".equalsIgnoreCase(reply) || "English".equalsIgnoreCase(reply)
									|| preMenuSwahili.equalsIgnoreCase(userButtonReply)) {

								Long userReply = 0L;
								if (userButtonReply.equalsIgnoreCase(previousMenu)
										|| preMenuSwahili.equalsIgnoreCase(userButtonReply))
									userReply = 9L;
								else if ("Swahili".equalsIgnoreCase(reply) || "English".equalsIgnoreCase(reply))
									userReply = 0L;
								else
									userReply = Long.valueOf(reply);

								if (userReply == 0) {
									nxtMsgId = "ZMB001";  //COM001
								} else {
									//Map<String, Object> map = waChatRepo.getNextMsgId(wdd.getParentmessageid(),userReply);
									Map<String, Object> map = getNextMsgId(wdd.getParentmessageid(),userReply);

									if (map.size() > 0) {

										nxtMsgId = map.get("messageid") == null ? "" : map.get("messageid").toString();
										isJob = map.get("isjobyn") == null ? "N" : map.get("isjobyn").toString();
										replyMsg = map.get("description") == null ? ""
												: map.get("description").toString();

										if (StringUtils.isNotBlank(nxtMsgId)) {
											//Map<String, Object> map2 = waChatRepo.getChatInputs(nxtMsgId);
											Map<String, Object> map2 = getChatInputs(nxtMsgId);

											isinput = map2.get("isinput") == null ? "N"
													: map2.get("isinput").toString();
											inputValue = map2.get("input_value") == null ? ""
													: map2.get("input_value").toString();
											requestKey = map2.get("request_key") == null ? ""
													: map2.get("request_key").toString();
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

							if (isJob.equalsIgnoreCase("N") || nxtMsgId.equals("ZMB001")) { //COM001

								WAMessageMaster msgM = getMsgMasterContent(nxtMsgId);

								WAWatiReq sended_res = zambWatiSer.sendSessMsg(msgM, waid);

								sended_res.setParentMsgId(msgM.getMessageid());

								saveMsg(sended_res);
							} else {
								List<WhatsappTemplateMaster> tempM = getTempMasterContent(nxtMsgId, "90016", waid);

								List<WhatsAppReq> reqList = setRequestList(tempM, waid);

								insertReqDet(reqList);
							}
						} else if ((isJob.equalsIgnoreCase("Y"))
								&& (userButtonReply.replace("\\s", "").equalsIgnoreCase(previousMenu)
										|| preMenuSwahili.equalsIgnoreCase(userButtonReply)
										|| "9".equals(userButtonReply))) {

							WAMessageMaster msgM = getMsgMasterContent(wdd.getParentmessageid());

							WAWatiReq sended_res = zambWatiSer.sendSessMsg(msgM, waid);

							sended_res.setParentMsgId(wdd.getParentmessageid());

							saveMsg(sended_res);
						} else {
							String msgid = StringUtils.isBlank(wdd.getUsermessageid()) ? "" : wdd.getUsermessageid();

							List<WhatsappRequestDetail> reqDetList = getWaReqDet(waid, "Y", msgid);

							log.info("sendChatMsg--> reqDetList size: " + reqDetList.size());

							List<Long> waidList = new ArrayList<>();

							waidList.add(waid);

							if (reqDetList.size() > 0) {

								WhatsappRequestDetail reqDet = reqDetList.get(0);

								WhatsappRequestDetailPK detPk = reqDet.getReqDetPk();

								String isReply = StringUtils.isBlank(reqDet.getIsreplyyn()) ? "N"
										: reqDet.getIsreplyyn();
								String isDocUpl = StringUtils.isBlank(reqDet.getIsdocuplyn()) ? "N"
										: reqDet.getIsdocuplyn();

								String isSkip = StringUtils.isBlank(reqDet.getIsskipyn()) ? "N" : reqDet.getIsskipyn();
								String isApiCall = StringUtils.isBlank(reqDet.getIsapicall()) ? "N"
										: reqDet.getIsapicall();
								String isValidationApi = StringUtils.isBlank(reqDet.getIsvalidationapi()) ? "N"
										: reqDet.getIsvalidationapi();
								String isValid = StringUtils.isBlank(reqDet.getIsvalid()) ? "N" : reqDet.getIsvalid();

								String IsResYn = StringUtils.isBlank(reqDet.getIsReponseYn()) ? "N"
										: reqDet.getIsReponseYn();
								String IsResYnSent = StringUtils.isBlank(reqDet.getIsResponseYnSent()) ? "N"
										: reqDet.getIsResponseYnSent();
								String isResSaveApi = StringUtils.isBlank(reqDet.getIsResSaveApi()) ? "N"
										: reqDet.getIsResSaveApi();

								Long stageCode = detPk.getCurrentstage();
								Long subStageCode = detPk.getCurrentsubstage();

								WhatsappTemplateMaster tempM = getTempMasterStageContent(msgid, "90016", waid,
										stageCode, subStageCode);

								String isskipped = "N";
								String isProcessComp = "N";

								if (IsResYn.equals("Y") && "Y".equals(IsResYnSent))
									reqDet.setUserreply(reqDet.getUserreply());
								else
									reqDet.setUserreply(reply);

								reqDet.setWausermessageid(request.getId());

								if (isSkip.equalsIgnoreCase("Y")
										&& (reply.equals("99") || reply.equalsIgnoreCase("Skip")
												|| reply.equalsIgnoreCase("Skip Image"))
										&& isReply.equalsIgnoreCase("Y")) {
									isskipped = "Y";
								}

								String commonurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api");
								String msgurl = cs.getwebserviceurlProperty()
										.getProperty("whatsapp.api.sendSessionMessage");
								String fileurl = cs.getwebserviceurlProperty()
										.getProperty("whatsapp.api.sendSessionFile");
								String button_msg_url = cs.getwebserviceurlProperty()
										.getProperty("whatsapp.api.button");

								String auth = cs.getwebserviceurlProperty().getProperty("whatsapp.auth");

								OkHttpClient okhttp = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS)
										.build();

								RequestBody body = RequestBody.create(new byte[0], null);

								detailRepo.save(reqDet);

								if ("Y".equalsIgnoreCase(isResSaveApi) && "Y".equalsIgnoreCase(isApiCall)
										&& "N".equalsIgnoreCase(isskipped)
										&& !"location".equalsIgnoreCase(request.getType())) {

									isValid = zambWatiApiCall.sendValidationMsg(okhttp, body, commonurl, msgurl, fileurl,
											auth, reqDet, String.valueOf(waid), new Date(), tempM);

								}

								else if (isApiCall.equalsIgnoreCase("Y") && isValidationApi.equalsIgnoreCase("Y")
										&& isskipped.equalsIgnoreCase("N") && "N".equalsIgnoreCase(isDocUpl)
										&& !"location".equalsIgnoreCase(request.getType())) {

									isValid = zambWatiApiCall.sendValidationMsg(okhttp, body, commonurl, msgurl, fileurl,
											auth, reqDet, String.valueOf(waid), new Date(), tempM);

								} else if (IsResYn.equals("Y") && isskipped.equalsIgnoreCase("N")
										&& isApiCall.equalsIgnoreCase("Y") && "N".equals(IsResYnSent)
										&& !"location".equalsIgnoreCase(request.getType())) {

									isValid = zambWatiApiCall.sendIsResYnMsg(okhttp, body, commonurl, button_msg_url,
											fileurl, auth, reqDet, String.valueOf(waid), new Date());

								} else if (IsResYn.equals("Y") && "Y".equals(IsResYnSent)
										&& "No".equalsIgnoreCase(reply)
										&& !"location".equalsIgnoreCase(request.getType())) {

									Map<String, Object> map = waChatRepo.getParentMessageId(reqDet.getRemarks());

									String parentMsgId = map.get("PARENTMESSAGEID") == null ? ""
											: map.get("PARENTMESSAGEID").toString();

									WAMessageMaster msgM = getMsgMasterContent(parentMsgId);

									WAWatiReq sended_res = zambWatiSer.sendSessMsg(msgM, waid);

									sended_res.setParentMsgId(msgM.getMessageid());

									saveMsg(sended_res);

									isValid = "Y";

								} else {

									isValid = "Y";
								}
								if (isValid.equalsIgnoreCase("Y")) {
									isProcessComp = "Y";
								}

								reqDet.setIsprocesscompleted(isProcessComp);
								reqDet.setIsskipped(isskipped);
								reqDet.setIsvalid(isValid);
								if (isDocUpl.equalsIgnoreCase("Y") && StringUtils.isNotBlank(data)
										&& isApiCall.equalsIgnoreCase("N") && type.equalsIgnoreCase("image")
										&& isskipped.equalsIgnoreCase("N")) {

									// String fileurls[] = data.split("fileName=");

									reqDet.setWa_userfilepath(data);

									String localPath = zambWatiSer.storeWAFile(data);

									reqDet.setUserreply(localPath.replace("\\", "//"));
									reqDet.setLocwa_userfilepath(localPath);

								} else if (isDocUpl.equalsIgnoreCase("Y") && StringUtils.isNotBlank(data)
										&& isApiCall.equalsIgnoreCase("Y") && (type.equalsIgnoreCase("image")
												|| type.equalsIgnoreCase("document") || type.equalsIgnoreCase("video"))
										&& isskipped.equalsIgnoreCase("N")) {

									// String fileurls[] = data.split("fileName=");
									motorImpl.saveClaimDocument(data, tempM, type, waid, reqDet);
									reqDet.setIsprocesscompleted("Y");

								} else if (isDocUpl.equalsIgnoreCase("Y")
										&& (StringUtils.isBlank(data) || !type.equalsIgnoreCase("image"))
										&& isskipped.equalsIgnoreCase("N")) {

									reqDet.setValidationmessage(tempM.getErrorrespstring());
									reqDet.setIsprocesscompleted("N");

									zambWatiApiCall.sendDocValidationMsg(okhttp, body, commonurl, msgurl, fileurl, auth,
											reqDet, String.valueOf(waid), new Date(), tempM);
								} else if ("location".equalsIgnoreCase(request.getType())) {

									reqDet.setUserreply(request.getData());
									reqDet.setIsprocesscompleted("Y");

								}

								detailRepo.save(reqDet);

								nxtMsgId = reqDet.getRemarks();

								if ("Y".equals(reqDet.getIsprocesscompleted())) {
									Flux.fromIterable(waidList).map(i -> zambWatiSer.callSendSessionMsg(i, msgid))
											.subscribeOn(Schedulers.boundedElastic()).subscribe();
								}

							} else {

								Flux.fromIterable(waidList).map(i -> zambWatiSer.callSendSessionMsg(i, msgid))
										.subscribeOn(Schedulers.boundedElastic()).subscribe();

							}
						}
					}
				}
			} else {
				sendLanguageMsg(request);
			}
		} catch (Exception e) {
			log.error(e);
		}
		return re;
	}

	private Map<String, Object> getChatInputs(String nxtMsgId) {
		try {
			QWAChatRecipientMaster qchatres = QWAChatRecipientMaster.wAChatRecipientMaster;
			//QWAChatRecipientMasterPK qchatpk = QWAChatRecipientMasterPK.wAChatRecipientMasterPK;
			
			List<Path<?>> allFields = Arrays.asList(
					qchatres.chatPk.parentmessageid,
					qchatres.chatPk.messageid,
					qchatres.description,
					qchatres.useroptted_messageid,
					qchatres.validationapi,
					qchatres.apiusername,
					qchatres.apipassword,
					qchatres.requeststring,
					qchatres.status,
					qchatres.effectivedate,
					qchatres.entrydate,
					qchatres.remarks,
					qchatres.isjobyn,
					qchatres.isinput,
					qchatres.input_value,
					qchatres.request_key,
					qchatres.iscommonmsg,
					qchatres.commonmsgid
					);
			
			Tuple chatmsg =  (Tuple) jpa.from(qchatres)
					.select(allFields.toArray(new Path<?>[0]))
					.where(qchatres.chatPk.messageid.eq(nxtMsgId),
							qchatres.status.equalsIgnoreCase("Y"),
							qchatres.effectivedate.loe(new Date()),
							qchatres.useroptted_messageid.notIn(0,9,99)
							).fetchOne();

			Map<String, Object> resultMap = new HashMap<>();
			if(chatmsg == null){
				return resultMap;	
			}else {
	        for (Path<?> field : allFields) {
	            resultMap.put(field.getMetadata().getName(), chatmsg.get(field));
	        }
	        return resultMap;
			}
		}catch(Exception e) {
			log.error(e);
		}
		return null;
	}

	private Map<String, Object> getNextMsgId(String parentmessageid, Long userReply) {
		try {
			QWAChatRecipientMaster qchatres = QWAChatRecipientMaster.wAChatRecipientMaster;
			//QWAChatRecipientMasterPK qchatpk = QWAChatRecipientMasterPK.wAChatRecipientMasterPK;
			
			List<Path<?>> allFields = Arrays.asList(
					qchatres.chatPk.parentmessageid,
					qchatres.chatPk.messageid,
					qchatres.description,
					qchatres.useroptted_messageid,
					qchatres.validationapi,
					qchatres.apiusername,
					qchatres.apipassword,
					qchatres.requeststring,
					qchatres.status,
					qchatres.effectivedate,
					qchatres.entrydate,
					qchatres.remarks,
					qchatres.isjobyn,
					qchatres.isinput,
					qchatres.input_value,
					qchatres.request_key,
					qchatres.iscommonmsg,
					qchatres.commonmsgid
					);
					
			
			Tuple chatmsg =  (Tuple) jpa.from(qchatres)
					.select(allFields.toArray(new Path<?>[0]))
					.where(qchatres.chatPk.parentmessageid.eq(parentmessageid),
							qchatres.useroptted_messageid.eq(userReply),
							qchatres.status.equalsIgnoreCase("Y"),
							qchatres.effectivedate.loe(new Date())
							).fetchOne();
			
			Map<String, Object> resultMap = new HashMap<>();
	        for (Path<?> field : allFields) {
	            resultMap.put(field.getMetadata().getName(), chatmsg.get(field));
	        }
	        return resultMap;
		}catch(Exception e) {
			log.error(e);
		}
		return null;
	}

	private String sendLanguageMsg(WebhookReq req) {
		String buttonMessage="";
		String request="";
		
		try {
			String message ="*Phoenix Zambia Corporate Limited*\n\nPlease choose your language";
			
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
			
			String meta_message_api=cs.getwebserviceurlProperty().getProperty("zambia.message.api");
			String meta_message_api_auth=cs.getwebserviceurlProperty().getProperty("zambia.message.auth");
		
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
	
	private WhatsAppReq getCntWAMsgMaster(String msgId) {
		try {

			WhatsAppReq response = new WhatsAppReq();

			QWAMessageMaster qwamsgM = QWAMessageMaster.wAMessageMaster;

			Predicate pred = qwamsgM.commonmsgid.eq(msgId).and(qwamsgM.status.equalsIgnoreCase("Y"))
					.and(qwamsgM.iscommonmsg.equalsIgnoreCase("Y"));

			List<WAMessageMaster> list = (List<WAMessageMaster>) wamsgMRepo.findAll(pred, qwamsgM.entrydate.desc());

			String replyMsgId = "";

			if (list.size() > 0) {
				WAMessageMaster msgM = list.get(0);

				replyMsgId = msgM.getMessageid();

				response = WhatsAppReq.builder().type(replyMsgId).isJobYN("N").build();
			} else {

				QWAChatRecipientMaster qChat = QWAChatRecipientMaster.wAChatRecipientMaster;

				BooleanExpression and = qChat.status.equalsIgnoreCase("Y").and(qChat.isjobyn.equalsIgnoreCase("Y"))
						.and(qChat.iscommonmsg.equalsIgnoreCase("Y")).and(qChat.commonmsgid.eq(msgId));

				List<WAChatRecipientMaster> listChat = (List<WAChatRecipientMaster>) waChatRepo.findAll(and,
						qChat.entrydate.desc());

				if (listChat.size() > 0) {

					WAChatRecipientMaster chat = listChat.get(0);
					WAChatRecipientMasterPK chatPk = chat.getChatPk();

					replyMsgId = chatPk.getMessageid();

					response = WhatsAppReq.builder().type(replyMsgId).isJobYN(chat.getIsjobyn())
							.message(chat.getDescription()).parentMsgId(chatPk.getParentmessageid())
							.userReply(chat.getUseroptted_messageid() == null ? ""
									: String.valueOf(chat.getUseroptted_messageid()))
							.build();
				}
			}
			return response;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	private WAMessageMaster getMsgMasterContent(String msgid) {
		try {

            /*QWAMessageMaster qwamsgM = QWAMessageMaster.wAMessageMaster;
			
			WAMessageMaster wamsgM = jpa.selectFrom(qwamsgM)
					.where(qwamsgM.messageid.eq(msgid)
							.and(qwamsgM.status.equalsIgnoreCase("Y")))
					.fetchOne();*/
			QWAMessageMaster qwamsgM = QWAMessageMaster.wAMessageMaster;
			
			WAMessageMaster wamsgM = (WAMessageMaster) jpa.select(qwamsgM).from(qwamsgM)
					.where(qwamsgM.messageid.eq(msgid),
							qwamsgM.status.equalsIgnoreCase("Y"),
							qwamsgM.effectivedate.loe(new Date())
                  ).fetchOne();
					
			//WAMessageMaster wamsgM = wamsgMRepo.getMsgCont(msgid);

			return wamsgM;
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
						.map(i -> zambWatiSer.sendSessionMsg(i))
						.subscribeOn(Schedulers.boundedElastic())
						.subscribe();
				} else {
					Flux.fromIterable(list)
						.map(i -> zambWatiSer.callSendSessionMsg(i, requestList.get(0).getType()))
						.subscribeOn(Schedulers.boundedElastic())
						.subscribe();
				}
			}

		} catch (Exception e) {
			log.error(e);
		}
		return "";
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

	@Override
	public String zambiaWebhookFlowRes(WebhookReq webhookReq) {
		try {

			log.info("Zambia webhookRes--> request: "+webhookReq);
			cs.reqPrint(webhookReq);
 
			List<WebhookReq> list = new ArrayList<WebhookReq>();
			list.add(webhookReq);

			Flux.fromIterable(list)
				.map(i -> saveUserData(i))
				.subscribeOn(Schedulers.boundedElastic())
				.subscribe();

		} catch (Exception e) {
			log.error(e);
		}
		return null;	
	}
	
	List<String> questionTexts = Arrays.asList(
			"Please Choose Your Vehicle Usage",
			"Please Enter the Registration Number",
			"Please Choose your ID-Type",
			"Please Enter your ID-Number",
			"Please Choose the Insured Period",
			"Please Enter your Name",
			"Please Enter your Payment Mobile Number",
			"Thank you! You've completed the questionnaire. please type OK"
      );
	
	List<String> apiKeys = Arrays.asList(
			"motor_usage","reg_no","id_type","id_num","ins_period","cus_name","mob_no","thank_you"
				);
	
	List<String> questionTextsforComprehensive = Arrays.asList(
			"Please Choose Your Vehicle Usage",
			"Please Enter the Registration Number",
			"Please Choose your ID-Type",
			"Please Enter your ID-Number",
			"Please Choose the Insured Period",
			"Please Enter the Sum Insured Amount",
			"Please Enter your Name",
			"Please Enter your Payment Mobile Number",
			"Thank you! You've completed the questionnaire. please type OK"
      );
	
	List<String> apiKeysforComprehensive = Arrays.asList(
			"motor_usage","reg_no","id_type","id_num","ins_period","sum_insured","cus_name","mob_no","thank_you"
				);
	private final Map<String, UserSession> sessionMap = new HashMap<>();
	
	private String saveUserData(WebhookReq request) {
		try {
			Long waid = Long.valueOf(request.getWaId());

			log.info("saveUserData--> waid: " + waid);

			Date watiDate = new Date();

			log.info("saveUserData--> watiDate: " + watiDate);
			
			PhoenixUserDataDetails datas = new PhoenixUserDataDetails();
			if(request.getType().equalsIgnoreCase("text")) {
				if(request.getText().equalsIgnoreCase("hi")) {
					
					datas.setCompanyId("100046");
					datas.setEntryDate(watiDate);
					datas.setWaid(waid);
					datas.setWamessageid(request.getWhatsappMessageId());
					datas.setUserReply(request.getText());
					datas.setStatus("Y");
					datas.setParentMessageId("MSG001");
					datas.setUserMessageId("MSG001");
					sessionMap.remove(waid.toString());
				}else {
					datas.setCompanyId("100046");
					datas.setEntryDate(watiDate);
					datas.setWaid(waid);
					datas.setWamessageid(request.getWhatsappMessageId());
					datas.setUserReply(request.getText());
					datas.setStatus("Y");
					datas.setParentMessageId("MSG010");
					datas.setUserMessageId("MSG010");
					
					List<PhoenixUserDataDetails> userDetails = null;
					List<PhoenixUserDataDetails> userDetails2 = null;
					PhoenixUserDataDetails details = null;
					String insType = "", resp ="";
					
					
					List<String> messageIds = Arrays.asList("MSG200", "MSG100");
					 userDetails = userDataRepo.findTop1ByWaidAndCompanyIdAndUserMessageIdInOrderByEntryDateDesc(waid,"100046",messageIds);
					 
					
					if(userDetails.isEmpty()) {
					//	userDetails2 = userDataRepo.findByWaidAndParentMessageIdAndUserMessageIdAndCompanyIdInOrderByEntryDateDesc(waid,"MSG100","MSG100","100046");					
					//	details = userDetails2.get(0);
					//	 insType = details.getUserReply();
								System.out.println(insType);
					}else {
						details = userDetails.get(0);
						 insType = details.getUserReply();
								System.out.println(insType);
					}
					if(insType.equalsIgnoreCase("Comprehensive")) {
						
						resp = handleIncomingMessageForComp(waid.toString(),request.getText(),insType);
						datas.setFlowRequest(resp);
						
					}else if(insType.equalsIgnoreCase("Act Only") || insType.equalsIgnoreCase("Full Third Party")) {
						resp = handleIncomingMessageForOthers(waid.toString(),request.getText(),insType);
						datas.setFlowRequest(resp);
					}
				}
			}else if(request.getType().equalsIgnoreCase("interactive")) {
				
				 if(request.getButtonTitle().equalsIgnoreCase("Motor Insurance")) {
					datas.setCompanyId("100046");
					datas.setEntryDate(watiDate);
					datas.setWaid(waid);
					datas.setWamessageid(request.getWhatsappMessageId());
					datas.setUserReply(request.getButtonTitle());
					datas.setStatus("Y");
					datas.setParentMessageId("MSG001");
					datas.setUserMessageId("MSG002");
					sessionMap.remove(waid.toString());
				}else if(request.getButtonTitle().equalsIgnoreCase("Buy New Insurance")) {
					datas.setCompanyId("100046");
					datas.setEntryDate(watiDate);
					datas.setWaid(waid);
					datas.setWamessageid(request.getWhatsappMessageId());
					datas.setUserReply(request.getButtonTitle());
					datas.setStatus("Y");
					datas.setParentMessageId("MSG002");
					datas.setUserMessageId("MSG003");
					sessionMap.remove(waid.toString());
				}else if(request.getButtonTitle().equalsIgnoreCase("Search Vehicle")) {
					datas.setCompanyId("100046");
					datas.setEntryDate(watiDate);
					datas.setWaid(waid);
					datas.setWamessageid(request.getWhatsappMessageId());
					datas.setUserReply(request.getButtonTitle());
					datas.setStatus("Y");
					datas.setParentMessageId("MSG003");
					datas.setUserMessageId("MSG004");
					sessionMap.remove(waid.toString());
				}else if(request.getButtonTitle().equalsIgnoreCase("Add New Vehicle")) {
					datas.setCompanyId("100046");
					datas.setEntryDate(watiDate);
					datas.setWaid(waid);
					datas.setWamessageid(request.getWhatsappMessageId());
					datas.setUserReply(request.getButtonTitle());
					datas.setStatus("Y");
					datas.setParentMessageId("MSG003");
					datas.setUserMessageId("MSG005");
					sessionMap.remove(waid.toString());
				}else if(request.getButtonTitle().equalsIgnoreCase("Act Only")){
					datas.setCompanyId("100046");
					datas.setEntryDate(watiDate);
					datas.setWaid(waid);
					datas.setWamessageid(request.getWhatsappMessageId());
					datas.setUserReply(request.getButtonTitle());
					datas.setStatus("Y");
					datas.setParentMessageId("MSG100");
					datas.setUserMessageId("MSG100");
					sessionMap.remove(waid.toString());
				}else if(request.getButtonTitle().equalsIgnoreCase("Comprehensive")) {
					datas.setCompanyId("100046");
					datas.setEntryDate(watiDate);
					datas.setWaid(waid);
					datas.setWamessageid(request.getWhatsappMessageId());
					datas.setUserReply(request.getButtonTitle());
					datas.setStatus("Y");
					datas.setParentMessageId("MSG200");
					datas.setUserMessageId("MSG200");
					sessionMap.remove(waid.toString());
				}else if(request.getButtonTitle().equalsIgnoreCase("Full Third Party")) {
					datas.setCompanyId("100046");
					datas.setEntryDate(watiDate);
					datas.setWaid(waid);
					datas.setWamessageid(request.getWhatsappMessageId());
					datas.setUserReply(request.getButtonTitle());
					datas.setStatus("Y");
					datas.setParentMessageId("MSG100");
					datas.setUserMessageId("MSG100");
					sessionMap.remove(waid.toString());
				}
			}
						
			userDataRepo.saveAndFlush(datas);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String handleIncomingMessageForComp(String waIds, String textBody,String insClass) {
		UserSession session = sessionMap.getOrDefault(waIds, new UserSession());
		
		String apiResult ="";

        int index = session.getCurrentQuestionIndex();
        if (index >= apiKeysforComprehensive.size()) {
            sendWhatsappMessage(waIds, "You have already completed the form.");
            return "";
        }
        
     // Store the answer with the correct key
        session.addAnswer(apiKeysforComprehensive.get(index), textBody);
        session.nextQuestion();
        sessionMap.put(waIds, session);
        Map<String,String> currentSessionResp = session.getResponses();
        try {
			String sessionResp = mapper.writeValueAsString(currentSessionResp);
			apiResult = sessionResp ;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // Send next question or finalize
        if (index + 1 < questionTextsforComprehensive.size()) {
            sendWhatsappMessage(waIds, questionTextsforComprehensive.get(index + 1));
        } else {
            try {
				apiResult = callYourAPI(session.getResponses(),insClass,waIds);
			} catch (JsonProcessingException | WhatsAppValidationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            sessionMap.remove(waIds);
            sendWhatsappMessage(waIds, "✅ Thank you! All your details have been received.");
            
            if(StringUtils.isNoneBlank(apiResult)) {
            	responseMethodCall(apiResult,waIds);
            }
        }
		return apiResult;
	}
	
	public String handleIncomingMessageForOthers(String waIds, String textBody, String insClass) {
		UserSession session = sessionMap.getOrDefault(waIds, new UserSession());
		
		String apiResult ="";

        int index = session.getCurrentQuestionIndex();
        if (index >= apiKeys.size()) {
            sendWhatsappMessage(waIds, "You have already completed the form.");
            return "";
        }
        
     // Store the answer with the correct key
        session.addAnswer(apiKeys.get(index), textBody);
        session.nextQuestion();
        sessionMap.put(waIds, session);
        Map<String,String> currentSessionResp = session.getResponses();
        try {
			String sessionResp = mapper.writeValueAsString(currentSessionResp);
			apiResult = sessionResp ;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // Send next question or finalize
        if (index + 1 < questionTexts.size()) {
            sendWhatsappMessage(waIds, questionTexts.get(index + 1));
        } else {
            try {
				apiResult = callYourAPI(session.getResponses(),insClass,waIds);
			} catch (JsonProcessingException | WhatsAppValidationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            sessionMap.remove(waIds);
            sendWhatsappMessage(waIds, "✅ Thank you! All your details have been received.");
            
            if(StringUtils.isNoneBlank(apiResult)) {
            	responseMethodCall(apiResult,waIds);
            }
        }
		return apiResult;
	}

	private String callYourAPI(Map<String, String> data, String insClass,String waIds) throws JsonMappingException, JsonProcessingException, WhatsAppValidationException{
	        //String apiUrl = "http://localhost:6060/WhatsAppApiMeta/insurance/generate/swaziland/quote";
	        
	        log.info("Create Quote Api Req " + data);
	        
	       Map<String,Object> responsce= (Map<String, Object>) zambiaInsuranceService.generateZambiaQuote(data,insClass,waIds);
	       
	       String resp = objectPrint.toJson(responsce);
		return resp;
	}

	private void sendWhatsappMessage(String waId, String message) {
		System.out.println("Sending to " + waId + ": " + message);
		log.info("Sending to " + waId + ": " + message);
		// Here, you can call the Meta WhatsApp API to actually send the message.
	}
	
	private void responseMethodCall(String apiResult, String waIds) {
		log.info("Response Send No: " + waIds);
		
		Map<String,Object> result =  null;
		try {
			 result = mapper.readValue(apiResult, Map.class);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String displayName = result.get("customer_name") == null ? "" : result.get("customer_name").toString();
		String registrationNo = result.get("registration") == null ? "" : result.get("registration").toString();
		String chassisNo = result.get("chassis") == null ? "" : result.get("chassis").toString();
		String vehicleUsage = result.get("usage") == null ? "" : result.get("usage").toString();
		String vehicleType = result.get("vehtype") == null ? "" : result.get("vehtype").toString();
		String vehicleColor = result.get("color") == null ? "" : result.get("color").toString();
		String sumInsured = result.get("suminsured") == null ? "" : result.get("suminsured").toString();
		String premium = result.get("premium") == null ? "" : result.get("premium").toString();
		String vat = result.get("vat") == null ? "" : result.get("vat").toString();
		String vatAmt = result.get("vatamt") == null ? "" : result.get("vatamt").toString();
		String totalPremium = result.get("totalpremium") == null ? "" : result.get("totalpremium").toString();
		String inceptionDate = result.get("inceptiondate") == null ? "" : result.get("inceptiondate").toString();
		String expiryDate = result.get("expirydate") == null ? "" : result.get("expirydate").toString();
		String referenceNo = result.get("referenceno") == null ? "" : result.get("referenceno").toString();
		String payment = result.get("url") == null ? "" : result.get("url").toString();
		String make = result.get("veh_make_desc") == null ? "" : result.get("veh_make_desc").toString();
		String model = result.get("veh_model_desc") == null ? "" : result.get("veh_model_desc").toString();
		
		
		Map<String,Object> respMap = new HashMap<>();
		respMap.put("to", waIds);
		respMap.put("type", "template");
		
		Map<String,Object> langMap = new HashMap<>();
		langMap.put("policy", "deterministic");
		langMap.put("code", "en");
		
		Map<String,Object> componentsMap = new HashMap<>();
		componentsMap.put("type", "body");
		
		//Map<String,Object> nameMapping = new HashMap<>();
		//nameMapping.put("name", "motor_quotation_res");
		
		Map<String,Object> nameMap = new HashMap<>();
		nameMap.put("type", "text");
		nameMap.put("text", displayName);
		
		Map<String,Object> regNoMap = new HashMap<>();
		regNoMap.put("type", "text");
		regNoMap.put("text", registrationNo);
		
		Map<String,Object> chassisNoMap = new HashMap<>();
		chassisNoMap.put("type", "text");
		chassisNoMap.put("text", chassisNo);
		
		Map<String,Object> vehUsageMap = new HashMap<>();
		vehUsageMap.put("type", "text");
		vehUsageMap.put("text", vehicleUsage);
		
		
		Map<String,Object> vehTypeMap = new HashMap<>();
		vehTypeMap.put("type", "text");
		vehTypeMap.put("text", vehicleType);
		
	//	Map<String,Object> colorMap = new HashMap<>();
	//	colorMap.put("type", "text");
	//	colorMap.put("text", vehicleColor);
		
		Map<String,Object> sumInsuredMap = new HashMap<>();
		sumInsuredMap.put("type", "text");
		sumInsuredMap.put("text", sumInsured);
		
		Map<String,Object> premiumMap = new HashMap<>();
		premiumMap.put("type", "text");
		premiumMap.put("text", premium);
		
		Map<String,Object> vatMap = new HashMap<>();
		vatMap.put("type", "text");
		vatMap.put("text", vat);
		
		Map<String,Object> vatAmtMap = new HashMap<>();
		vatAmtMap.put("type", "text");
		vatAmtMap.put("text", vatAmt);
		
		Map<String,Object> totPremiumMap = new HashMap<>();
		totPremiumMap.put("type", "text");
		totPremiumMap.put("text", totalPremium);
		
		Map<String,Object> incDateMap = new HashMap<>();
		incDateMap.put("type", "text");
		incDateMap.put("text", inceptionDate);
		
	//	Map<String,Object> expDateMap = new HashMap<>();
	//	expDateMap.put("type", "text");
	//put("text", expiryDate);
		
		Map<String,Object> refNoMap = new HashMap<>();
		refNoMap.put("type", "text");
		refNoMap.put("text", referenceNo);
		
		Map<String,Object> urlMap = new HashMap<>();
		urlMap.put("type", "text");
		urlMap.put("text", payment);
		
		Map<String,Object> makeMap = new HashMap<>();
		makeMap.put("type", "text");
		makeMap.put("text", make);
		
		Map<String,Object> modelMap = new HashMap<>();
		modelMap.put("type", "text");
		modelMap.put("text", model);
		
		componentsMap.put("parameters", Arrays.asList(nameMap,regNoMap,chassisNoMap,vehUsageMap,vehTypeMap,makeMap,modelMap,sumInsuredMap,
				premiumMap,vatMap,vatAmtMap,totPremiumMap,incDateMap,refNoMap,urlMap));
		
		Map<String,Object> tempMap = new HashMap<>();
		tempMap.put("language", langMap);
		tempMap.put("name", "quote_response");
		tempMap.put("components", Arrays.asList(componentsMap));
		
		respMap.put("template", tempMap);
		
		String respMapreq = objectPrint.toJson(respMap);
		
		String apiUrl = askeveApiforZambia;
		RestTemplate restTemp = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		HttpEntity<String> request = new HttpEntity<>(respMapreq,headers);
		
		try {
			ResponseEntity<String> response = restTemp.postForEntity(apiUrl, request, String.class);
			log.info("Template Api Response :"+response.getBody());
			
		}catch(Exception e) {
			e.printStackTrace();
			log.info(e);
		}
		
	}
}
