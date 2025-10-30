package com.maan.whatsapp.controller.whatsapp;

import java.io.IOException;
import java.util.Base64;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.maan.whatsapp.meta.Messages;
import com.maan.whatsapp.meta.MetaWebhookRequest;
import com.maan.whatsapp.request.wati.getcont.WebhookReq;
import com.maan.whatsapp.service.whatsapp.PhoenixBoatswanaWhatsAppService;
import com.maan.whatsapp.service.whatsapp.PhoenixMozambiqueWhatsAppService;
import com.maan.whatsapp.service.whatsapp.PhoenixNamibiaWhatsAppService;
import com.maan.whatsapp.service.whatsapp.PhoenixSwazilandWhatsAppService;
import com.maan.whatsapp.service.whatsapp.PhoenixZambiaWhatsAppService;

import jakarta.servlet.http.HttpServletResponse;
import okhttp3.OkHttpClient;

@RestController
@RequestMapping("/phoenix/whatsapp")
public class PhoenixWhatsAppController {

	Logger log = LogManager.getLogger(WhatsAppController.class);

	private static OkHttpClient okhttp = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();

	ObjectMapper mapper = new ObjectMapper();

	public static Gson printReq = new Gson();

	@Autowired
	private PhoenixMozambiqueWhatsAppService whatsappMozambiqueSer;
	
	@Autowired
	private PhoenixZambiaWhatsAppService whatsappZambiaSer;
	
	@Autowired
	private PhoenixSwazilandWhatsAppService whatsappSwazilandSer;
	
	@Autowired
	private PhoenixNamibiaWhatsAppService whatsappNamibiaSer;
	
	@Autowired
	private PhoenixBoatswanaWhatsAppService whatsappBoatswanaSer;

	@PostMapping("/webhook/mozambique")
	public ResponseEntity<String> mozambiqueWebhook(@RequestBody MetaWebhookRequest req, HttpServletResponse res)
			throws IOException {
		WebhookReq webhookReq = new WebhookReq();
		try {
			if (req != null) {

				List<Messages> msg = req.getEntry() == null ? null
						: req.getEntry().get(0).getChanges() == null ? null
								: req.getEntry().get(0).getChanges().get(0).getValue().getMessages() == null ? null
										: req.getEntry().get(0).getChanges().get(0).getValue().getMessages();

				if (msg != null) {
					log.info("Mozambique/Webhook request ==>" + printReq.toJson(req));

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

						webhookReq.setType("text");
					}

					webhookReq.setTimestamp(msg.get(0).getTimestamp());
					webhookReq.setSenderName(req.getEntry().get(0).getChanges().get(0).getValue().getContacts().get(0)
							.getProfile().getName());

					log.info("Modfied Mozambique Webhook request || " + printReq.toJson(webhookReq));

					whatsappMozambiqueSer.mozambiqueWebhookRes(webhookReq);

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
	
	@GetMapping("/webhook/mozambique")
    public ResponseEntity<String> webhookPhonixMozambiqueGet(@RequestParam("hub.mode") String hubMode,
                                             @RequestParam("hub.verify_token") String verifyToken,
                                             @RequestParam("hub.challenge") String hubChallenge) {
		System.out.println( "hub_mode  "+hubMode +" || hub_challenge " +hubChallenge +" || hub_verify_token"+verifyToken);

        if ("subscribe".equals(hubMode) && "PhoenixInsurance@001".equals(verifyToken)) {
            return ResponseEntity.ok(hubChallenge);
        } else {
            return ResponseEntity.status(403).body("Success");
        }
    }
	
	@PostMapping("/webhook/zambia")
	public ResponseEntity<String> zambiaWebhook(@RequestBody MetaWebhookRequest req, HttpServletResponse res)
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

						webhookReq.setType("text");
					}

					webhookReq.setTimestamp(msg.get(0).getTimestamp());
					webhookReq.setSenderName(req.getEntry().get(0).getChanges().get(0).getValue().getContacts().get(0)
							.getProfile().getName());

					log.info("Modfied Zambia Webhook request || " + printReq.toJson(webhookReq));

					whatsappZambiaSer.zambiaWebhookRes(webhookReq);

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
	
	@GetMapping("/webhook/zambia")
    public ResponseEntity<String> webhookPhonixZambiaGet(@RequestParam("hub.mode") String hubMode,
                                             @RequestParam("hub.verify_token") String verifyToken,
                                             @RequestParam("hub.challenge") String hubChallenge) {
		System.out.println( "hub_mode  "+hubMode +" || hub_challenge " +hubChallenge +" || hub_verify_token"+verifyToken);

        if ("subscribe".equals(hubMode) && "PhoenixInsurance@001".equals(verifyToken)) {
            return ResponseEntity.ok(hubChallenge);
        } else {
            return ResponseEntity.status(403).body("Success");
        }
    }
	
	@PostMapping("/webhook/swaziland")
	public ResponseEntity<String> swazilandWebhook(@RequestBody MetaWebhookRequest req, HttpServletResponse res)
			throws IOException {
		WebhookReq webhookReq = new WebhookReq();
		try {
			if (req != null) {

				List<Messages> msg = req.getEntry() == null ? null
						: req.getEntry().get(0).getChanges() == null ? null
								: req.getEntry().get(0).getChanges().get(0).getValue().getMessages() == null ? null
										: req.getEntry().get(0).getChanges().get(0).getValue().getMessages();

				if (msg != null) {
					log.info("Swaziland/Webhook request ==>" + printReq.toJson(req));

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

						webhookReq.setType("text");
					}

					webhookReq.setTimestamp(msg.get(0).getTimestamp());
					webhookReq.setSenderName(req.getEntry().get(0).getChanges().get(0).getValue().getContacts().get(0)
							.getProfile().getName());

					log.info("Modfied Swaziland Webhook request || " + printReq.toJson(webhookReq));

					whatsappSwazilandSer.swazilandWebhookRes(webhookReq);

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
	
	@GetMapping("/webhook/swaziland")
    public ResponseEntity<String> webhookPhonixSwazilandGet(@RequestParam("hub.mode") String hubMode,
                                             @RequestParam("hub.verify_token") String verifyToken,
                                             @RequestParam("hub.challenge") String hubChallenge) {
		System.out.println( "hub_mode  "+hubMode +" || hub_challenge " +hubChallenge +" || hub_verify_token"+verifyToken);

        if ("subscribe".equals(hubMode) && "PhoenixInsurance@001".equals(verifyToken)) {
            return ResponseEntity.ok(hubChallenge);
        } else {
            return ResponseEntity.status(403).body("Success");
        }
    }
	
	@PostMapping("/webhook/namibia")
	public ResponseEntity<String> namibiaWebhook(@RequestBody MetaWebhookRequest req, HttpServletResponse res)
			throws IOException {
		WebhookReq webhookReq = new WebhookReq();
		try {
			if (req != null) {

				List<Messages> msg = req.getEntry() == null ? null
						: req.getEntry().get(0).getChanges() == null ? null
								: req.getEntry().get(0).getChanges().get(0).getValue().getMessages() == null ? null
										: req.getEntry().get(0).getChanges().get(0).getValue().getMessages();

				if (msg != null) {
					log.info("Namibia/Webhook request ==>" + printReq.toJson(req));

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

					}else if ("document".equalsIgnoreCase(type)) {

						webhookReq.setImageId(msg.get(0).getId());
						webhookReq.setData(msg.get(0).getId());
					//	webhookReq.setMimeType(msg.get(0).getImage().getMime_type());

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

						webhookReq.setType("text");
					}

					webhookReq.setTimestamp(msg.get(0).getTimestamp());
					webhookReq.setSenderName(req.getEntry().get(0).getChanges().get(0).getValue().getContacts().get(0)
							.getProfile().getName());

					log.info("Modfied Namibia Webhook request || " + printReq.toJson(webhookReq));

					whatsappNamibiaSer.namibiaWebhookRes(webhookReq);

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
	
	@GetMapping("/webhook/namibia")
    public ResponseEntity<String> webhookPhonixNamibiaGet(@RequestParam("hub.mode") String hubMode,
                                             @RequestParam("hub.verify_token") String verifyToken,
                                             @RequestParam("hub.challenge") String hubChallenge) {
		System.out.println( "hub_mode  "+hubMode +" || hub_challenge " +hubChallenge +" || hub_verify_token"+verifyToken);

        if ("subscribe".equals(hubMode) && "PhoenixInsurance@001".equals(verifyToken)) {
            return ResponseEntity.ok(hubChallenge);
        } else {
            return ResponseEntity.status(403).body("Success");
        }
    }
	
	@PostMapping("/webhook/boatswana")
	public ResponseEntity<String> boatswanaWebhook(@RequestBody MetaWebhookRequest req, HttpServletResponse res)
			throws IOException {
		WebhookReq webhookReq = new WebhookReq();
		try {
			if (req != null) {

				List<Messages> msg = req.getEntry() == null ? null
						: req.getEntry().get(0).getChanges() == null ? null
								: req.getEntry().get(0).getChanges().get(0).getValue().getMessages() == null ? null
										: req.getEntry().get(0).getChanges().get(0).getValue().getMessages();

				if (msg != null) {
					log.info("Boatswana/Webhook request ==>" + printReq.toJson(req));

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

						webhookReq.setType("text");
					}

					webhookReq.setTimestamp(msg.get(0).getTimestamp());
					webhookReq.setSenderName(req.getEntry().get(0).getChanges().get(0).getValue().getContacts().get(0)
							.getProfile().getName());

					log.info("Modfied Boatswana Webhook request || " + printReq.toJson(webhookReq));

					whatsappBoatswanaSer.boatswanaWebhookRes(webhookReq);

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

	@GetMapping("/webhook/boatswana")
    public ResponseEntity<String> webhookPhonixBoatswanaGet(@RequestParam("hub.mode") String hubMode,
                                             @RequestParam("hub.verify_token") String verifyToken,
                                             @RequestParam("hub.challenge") String hubChallenge) {
		System.out.println( "hub_mode  "+hubMode +" || hub_challenge " +hubChallenge +" || hub_verify_token"+verifyToken);

        if ("subscribe".equals(hubMode) && "PhoenixInsurance@001".equals(verifyToken)) {
            return ResponseEntity.ok(hubChallenge);
        } else {
            return ResponseEntity.status(403).body("Success");
        }
    }

}
