package com.maan.whatsapp.service.wati;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maan.whatsapp.entity.master.WAMessageMaster;
import com.maan.whatsapp.entity.whatsapp.QWhatsappContactData;
import com.maan.whatsapp.entity.whatsapp.QWhatsappRequestData;
import com.maan.whatsapp.entity.whatsapp.QWhatsappRequestDataPK;
import com.maan.whatsapp.entity.whatsapp.QWhatsappRequestDetail;
import com.maan.whatsapp.entity.whatsapp.QWhatsappRequestDetailPK;
import com.maan.whatsapp.entity.whatsapp.WhatsappContactData;
import com.maan.whatsapp.entity.whatsapp.WhatsappRequestData;
import com.maan.whatsapp.entity.whatsapp.WhatsappRequestDataPK;
import com.maan.whatsapp.entity.whatsapp.WhatsappRequestDetail;
import com.maan.whatsapp.repository.whatsapp.WhatsappContactDataRepo;
import com.maan.whatsapp.repository.whatsapp.WhatsappRequestDataRepo;
import com.maan.whatsapp.repository.whatsapp.WhatsappRequestDetailRepo;
import com.maan.whatsapp.request.whatsapp.WAWatiReq;
import com.maan.whatsapp.response.wati.getmsg.GetMessageRes;
import com.maan.whatsapp.response.wati.getmsg.MessageItems;
import com.maan.whatsapp.response.wati.getmsg.MessageRes;
import com.maan.whatsapp.service.common.CommonService;
import com.maan.whatsapp.service.motor.MotorService;
import com.maan.whatsapp.service.motor.MotorServiceImpl;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public class SwazilandWatiServiceImpl implements SwazilandWatiService{

	
	@Autowired
	private WhatsappRequestDataRepo dataRepo;
	@Autowired
	private WhatsappRequestDetailRepo detailRepo;
	@Autowired
	private WhatsappContactDataRepo contactRepo;

	@Autowired
	private CommonService cs;
	@Autowired
	private SwazilandWatiApiCall swaziWatiApiCall;

	@Autowired
	private JPAQueryFactory jpa;

	@Autowired
	private MotorService motSer;
	
	@Autowired
	private MotorServiceImpl motorImpl;
	@Autowired
	private ObjectMapper objectMapper;

	private Logger log = LogManager.getLogger(getClass());
	
	
	@Override
	public String updateMsgStatus() {
		try {

			QWhatsappRequestDetail qDet = QWhatsappRequestDetail.whatsappRequestDetail;

			JPAQuery<WhatsappRequestDetail> jpa_reqData = jpa.selectFrom(qDet);

			List<Long> whatsappnos = jpa_reqData.select(qDet.whatsappid)
					.distinct()
					.where(qDet.issent.equalsIgnoreCase("N")
							.and(qDet.status.equalsIgnoreCase("Y"))
							.and(qDet.wa_response.isNotNull()))
					.fetch();

			log.info("updateMsgStatus--> whatsappnos: " + whatsappnos.size());

			if(whatsappnos.size() > 0) {

				String commonurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api");
				String msgurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api.getMessages");

				String auth = cs.getwebserviceurlProperty().getProperty("whatsapp.auth");

				String url = commonurl + msgurl;

				OkHttpClient okhttp = new OkHttpClient.Builder()
						.readTimeout(30, TimeUnit.SECONDS)
						.build();

				Flux.fromIterable(whatsappnos)
					.map(i -> callgetMessage(i, url, auth, okhttp))
					.subscribeOn(Schedulers.boundedElastic())
					.subscribe();
			}

			return "Updated";
		} catch (Exception e) {
			log.error(e);
		}
		return "Not Updated";	
	}

	@Override
	public String sendSessionMsg() {
		try {

			QWhatsappRequestData qdata = QWhatsappRequestData.whatsappRequestData;
			QWhatsappRequestDataPK qdataPk = qdata.reqDataPk;

			JPAQuery<WhatsappRequestData> jpa_reqData = jpa.selectFrom(qdata);

			List<WhatsappRequestData> datas = jpa_reqData.select(qdata)
					.where((qdata.isprocesscompleted.notEqualsIgnoreCase("Y")
							.or(qdata.isprocesscompleted.isNull()))
							.and(qdata.issessionactive.equalsIgnoreCase("Y"))
							.and(qdata.status.equalsIgnoreCase("Y")))
					.orderBy(qdata.entry_date.asc(), qdataPk.currentstage.asc())
					.fetch();

			log.info("sendSessionMsg--> messageNotSendedCount: " + datas.size());

			callsendSessionMsg(datas);

			return "";
		} catch (Exception e) {
			log.error(e);
		}
		return "";	
	}

	@Override
	public String sendSessionMsg(Long waid) {
		try {

			long count = checkSessionStatus(waid);

			log.info("sendSessionMsg--> waid: " + waid + " count: " + count);

			if (count > 0) {
				callSendSessionMsg(waid, "");
			}

		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}

	@Override
	public long checkSessionStatus(Long waid) {
		try {

			QWhatsappContactData qContactData = QWhatsappContactData.whatsappContactData;

			JPAQuery<WhatsappContactData> jpa_contactData = jpa.selectFrom(qContactData);

			DateTimeExpression<Date> currentTimestamp = Expressions.currentTimestamp();

			long count = jpa_contactData.where(
					qContactData.status.equalsIgnoreCase("Y")
					.and(qContactData.whatsappid.eq(waid))
					.and(currentTimestamp.between(qContactData.session_start_time, qContactData.session_end_time)))
					.fetchCount();

			return count;
		} catch (Exception e) {
			log.error(e);
		}
		return 0;
	}

	@Override
	public String callSendSessionMsg(Long waid, String msgid) {
		try {

			QWhatsappRequestDetail qDet = QWhatsappRequestDetail.whatsappRequestDetail;

			BooleanExpression bool_det = qDet.whatsappid.eq(waid)
					.and(qDet.status.equalsIgnoreCase("Y"))
					.and(qDet.isskipped.equalsIgnoreCase("N"))
					.and((qDet.isreplyyn.equalsIgnoreCase("Y").and(
							qDet.isprocesscompleted.notEqualsIgnoreCase("Y").or(qDet.issent.notEqualsIgnoreCase("Y"))))
									.or(qDet.isreplyyn.equalsIgnoreCase("N")
											.and(qDet.issent.notEqualsIgnoreCase("Y"))))
					.and(StringUtils.isBlank(msgid) ? qDet.remarks.isNotNull() : qDet.remarks.equalsIgnoreCase(msgid));

			List<WhatsappRequestDetail> detail_list = (List<WhatsappRequestDetail>) detailRepo.findAll(bool_det,
					qDet.entry_date.asc(), qDet.stage_order.asc());

			log.info("callsendSessionMsg--> list: " + detail_list.size());

			if(detail_list.size() > 0) {
				
				String commonurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api");
				String msgurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api.sendSessionMessage");
				String fileurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api.sendSessionFile");

				String auth = cs.getwebserviceurlProperty().getProperty("whatsapp.auth");

				OkHttpClient okhttp = new OkHttpClient.Builder()
						.readTimeout(30, TimeUnit.SECONDS)
						.build();

				RequestBody body = RequestBody.create(new byte[0], null);

				List<CompletableFuture<String>> callList = new ArrayList<>();

				String isReplyyn = "N";

				for (WhatsappRequestDetail detail : detail_list) {
					if(StringUtils.isNotEmpty(detail.getIsread())) {
						detail.setIsread("");
						detailRepo.save(detail);
					}
					if (isReplyyn.equalsIgnoreCase("N")) {
						CompletableFuture<String> call;

						call = swaziWatiApiCall.sendMsg(okhttp, body, commonurl, msgurl, fileurl, auth, detail,
								String.valueOf(waid), new Date());

						callList.add(call);

						isReplyyn = StringUtils.isBlank(detail.getIsreplyyn()) ? "N" : detail.getIsreplyyn();
	  				}

					if (isReplyyn.equalsIgnoreCase("Y"))
						break;
				}

				CompletableFuture.allOf(callList.toArray(new CompletableFuture[callList.size()])).join();
			}

			return "";
		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}

	@Override
	public WAWatiReq sendSessMsg(WAMessageMaster wamsgM, Long waid) {
		try {

			String url = cs.getwebserviceurlProperty().getProperty("swaziland.message.api");
			
			String auth = cs.getwebserviceurlProperty().getProperty("swaziland.message.auth");

			OkHttpClient okhttp = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();

			RequestBody body = RequestBody.create(new byte[0], null);
			
			String msgEn = StringUtils.isBlank(wamsgM.getMessagedescen()) ? "" : wamsgM.getMessagedescen();
			String msgAr = StringUtils.isBlank(wamsgM.getMessagedescar()) ? "" : wamsgM.getMessagedescar();
			String interactive_button_yn=StringUtils.isBlank(wamsgM.getInteractiveButtonYn())?"N":wamsgM.getInteractiveButtonYn();
			String language =contactRepo.getLanguage(waid.toString());
			String msg ="";
			
			msg ="English".equalsIgnoreCase(language)?msgEn:msgAr;
			
			
			String button1 ="",button2="",button3="",flow_id="",flow_token="",flowRequestDataYn ="",
							flow_api="",flow_api_auth="",flow_api_method ="",flow_button_name="",cta_button_name="",
							location_button_name="",menu_button_name="",message_type="",flow_index_screen_name="",
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
			WAWatiReq waReq = WAWatiReq.builder()
					.filepath("")
					.msg(msg)
					.interactiveYn(interactive_button_yn)
					.waid(String.valueOf(waid))
					.button_1(button1) 
					.button_2(button2) 
					.button_3(button3) 
					.messageId(wamsgM.getMessageid())
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
					.build();

			
			WAWatiReq response = swaziWatiApiCall.callSendSessionMsg(okhttp, body, url, auth, waReq);

			return response;
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	@Override
	public String storeWAFile(String wafile) {
		try {
			log.info("storeWAFile--> wafile: " + wafile);

			String image_url = cs.getwebserviceurlProperty().getProperty("meta.get.image.api");
			
			String auth = cs.getwebserviceurlProperty().getProperty("meta.message.api.auth");
	
			OkHttpClient okhttp = new OkHttpClient.Builder()
						.readTimeout(30, TimeUnit.SECONDS)
						.build();
				
			image_url = image_url.replace("{IMAGE_ID}", wafile);
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
			
			String mime_type =meta_image.get("mime_type")==null?"":meta_image.get("mime_type").toString().split("/")[1];
			
			URL url = new URL(image);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("GET");
	        connection.setRequestProperty("Authorization", auth);
	        
			InputStream is = connection.getInputStream();
			
			
			String path = cs.getwebserviceurlProperty().getProperty("wa.preins.image.path");

			String date = cs.formatdatewithtime4(new Date());
			//String fileName = FilenameUtils.getBaseName(wafile);
			//String extension = FilenameUtils.getExtension(wafile);
			 
			String name =date +"_"+ System.currentTimeMillis() + "." + mime_type.trim();
			path = path + name;

			log.info("storeWAFile--> path: " + path);

			File toFile = new File(path);

			FileUtils.copyInputStreamToFile(is, toFile);

			log.info("storeWAFile--> wafile: " + wafile + " path: " + path);

			return path;
		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}
	
	private String callsendSessionMsg(List<WhatsappRequestData> datas) {
		try {

			if (datas.size() > 0) {

				String commonurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api");
				String msgurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api.sendSessionMessage");
				String fileurl = cs.getwebserviceurlProperty().getProperty("whatsapp.api.sendSessionFile");

				String auth = cs.getwebserviceurlProperty().getProperty("whatsapp.auth");

				OkHttpClient okhttp = new OkHttpClient.Builder()
						.readTimeout(30, TimeUnit.SECONDS)
						.build();

				RequestBody body = RequestBody.create(new byte[0], null);

				for (WhatsappRequestData data : datas) {

					Date reqtime = new Date();

					WhatsappRequestDataPK dataPk = data.getReqDataPk();

					Long quoteno = dataPk.getQuoteno();
					Long currentStage = dataPk.getCurrentstage();
					Long productid = dataPk.getProductid();
					Long mobileno = dataPk.getMobileno();
					Long whatsappcode = dataPk.getWhatsappcode();

					String mobno = String.valueOf(whatsappcode) + String.valueOf(mobileno);

					QWhatsappRequestDetail qDet = QWhatsappRequestDetail.whatsappRequestDetail;
					QWhatsappRequestDetailPK qDetPk = qDet.reqDetPk;

					BooleanExpression bool_det = qDetPk.currentstage.eq(currentStage)
								.and(qDetPk.quoteno.eq(quoteno))
								.and(qDetPk.productid.eq(productid))
								.and(qDet.status.equalsIgnoreCase("Y"))
								.and(qDet.issent.notEqualsIgnoreCase("Y")
										.or(qDet.issent.isNull()));

					List<WhatsappRequestDetail> detail_list = (List<WhatsappRequestDetail>) detailRepo.findAll(bool_det,
							qDet.stage_order.asc());

					log.info("callsendSessionMsg--> list: " + detail_list.size());

					List<CompletableFuture<String>> callList = new ArrayList<>();

					for (WhatsappRequestDetail detail : detail_list) {

						CompletableFuture<String> call;

						call = swaziWatiApiCall.sendMsg(okhttp, body, commonurl, msgurl, fileurl, auth, detail, mobno,
								reqtime);

						callList.add(call);
					}

					CompletableFuture.allOf(callList.toArray(new CompletableFuture[callList.size()])).join();

					if(detail_list.size() > 0) {
						BooleanExpression bool_detIssent = qDetPk.currentstage.eq(currentStage)
								.and(qDetPk.quoteno.eq(quoteno))
								.and(qDetPk.productid.eq(productid))
								.and(qDet.status.equalsIgnoreCase("Y"))
								.and(qDet.issent.notEqualsIgnoreCase("Y")
										.or(qDet.issent.isNull()));

						long count = detailRepo.count(bool_detIssent);

						if (count > 0) {
							data.setIsprocesscompleted("N");
							data.setRequest_time(reqtime);
							data.setResponse_time(new Date());
						} else {
							data.setIsprocesscompleted("Y");
							data.setRequest_time(reqtime);
							data.setResponse_time(new Date());
						}

						dataRepo.save(data);
					}
				}				
			}

			return "";
		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}
	
	private String callgetMessage(Long whatsappid, String url, String auth, OkHttpClient okhttp) {
		try {

			String waid = String.valueOf(whatsappid);

			url = url.replace("{whatsappNumber}", waid);
			url = url.replace("{pageSize}", "");
			url = url.replace("{pageNumber}", "");
			url = url.trim();

			Request request = new Request.Builder()
					.url(url)
					.addHeader("Authorization", auth)
					.get()
					.build();

			Response response = okhttp.newCall(request)
					.execute();

			String responseString = response.body().string();

			log.info("callgetMessage--> waid: " + waid + " response: " + responseString);

			GetMessageRes apiRes = objectMapper.readValue(responseString, GetMessageRes.class);

			String result = StringUtils.isBlank(apiRes.getResult()) ? "" : apiRes.getResult();

			if (result.equalsIgnoreCase("success")) {

				MessageRes messages = apiRes.getMessages();

				List<MessageItems> items = messages.getItems();

				if (items.size() > 0) {

					QWhatsappRequestDetail qDet = QWhatsappRequestDetail.whatsappRequestDetail;

					JPAQuery<WhatsappRequestDetail> jpa_reqData = jpa.selectFrom(qDet);

					List<WhatsappRequestDetail> details = jpa_reqData.select(qDet)
							.where(qDet.issent.equalsIgnoreCase("N")
									.and(qDet.status.equalsIgnoreCase("Y"))
									.and(qDet.whatsappid.eq(whatsappid))
									.and(qDet.wa_response.isNotNull()))
							.fetch();

					for (WhatsappRequestDetail det : details) {

						String message = StringUtils.isBlank(det.getMessage()) ? ""
								: det.getMessage().replaceAll("\\s", "").trim();

						Predicate<MessageItems> pred = i -> true;

						Optional<MessageItems> op_item = items.stream()
								.filter(pred
										.and(i -> (StringUtils.isBlank(i.getOwner()) ? false
												: i.getOwner().equalsIgnoreCase("true")))
										.and(i -> !NumberUtils.isCreatable(i.getType()))
										.and(i -> i.getEventType().equalsIgnoreCase("message"))
										.and(i -> (StringUtils.isBlank(i.getStatusString()) ? false
												: !i.getStatusString().equalsIgnoreCase("FAILED")))
										.and(i -> (StringUtils.isBlank(i.getText()) ? false
												: i.getText().replaceAll("\\s", "").trim().equalsIgnoreCase(message))))
								.findFirst();

						if (op_item.isPresent()) {
							MessageItems item = op_item.get();

							det.setIssent("Y");
							det.setWa_messageid(item.getId());
							det.setWa_filepath(item.getData());
							det.setWa_response(item.getStatusString());

							detailRepo.save(det);
						}
					}
				}
			}

		} catch (Exception e) {
			log.error(e);
		}
		return "";
	}

}
