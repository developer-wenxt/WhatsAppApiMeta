package com.maan.whatsapp.service.whatsapp;

import com.maan.whatsapp.request.wati.getcont.WebhookReq;

public interface PhoenixZambiaWhatsAppService {

	String zambiaWebhookRes(WebhookReq webhookReq);

	String zambiaWebhookFlowRes(WebhookReq webhookReq);

}
