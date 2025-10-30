package com.maan.whatsapp.document.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maan.whatsapp.document.service.RegistrationDocumentRecognitionService;

@Service
@PropertySource("classpath:WebServiceUrl.properties")
public class RegistrationDocumentRecognitionServiceImpl implements RegistrationDocumentRecognitionService{
	
	@Value("${external.gemini.api.key}")
    String apiKey;
	
	private final RestTemplate restTemplate = new RestTemplate();
	
	@Autowired
	private ObjectMapper mapper;

	@Override
	public JsonNode getDocumentResult(MultipartFile file, Object req) throws IOException {
		
		String contentType = file.getContentType();
		String base64="";
		if(StringUtils.isEmpty(contentType)) {
			throw new IOException("Unknown file type");
		}else if(contentType.contentEquals("application/pdf")) {
			byte[] image = pdfToImage(file);
	    	 base64 = convertImageToBase64(image);
	    	 System.out.println("print Base 64: "+base64);
	    	 contentType = "image/jpeg";
		}else if(contentType.contentEquals("image/jpeg")) {
			 base64 = convertImageToBase64(file.getBytes());
		}else if(contentType.contentEquals("image/png")) {
			base64 = convertImageToBase64(file.getBytes());
		}else if(contentType.contentEquals("image/gif")) {
			base64 = convertImageToBase64(file.getBytes());
		}else if(contentType.contentEquals("image/webp")) {
			base64 = convertImageToBase64(file.getBytes());
		}else {
			throw new IOException("Unsupported file type: " + contentType);
		}
		return extractReplyFromGeminiAi(base64, contentType, req);
	}
	
private JsonNode extractReplyFromGeminiAi(String base64, String fileType , Object req) {
		
		String url = getGeminiUrl();
		
		String prompt ="";
		
		String request = req.toString();
		
		Map<String, Object> mapReq=null;
		try {
			mapReq = mapper.readValue(request, Map.class);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String companyId = mapReq.get("companyId") == null ? null : mapReq.get("companyId").toString();
		
		if(!StringUtils.isBlank(companyId)) {
			if("100046".equals(companyId)) {
				prompt = "Analyze the provided import declaration data. Extract only the English values. Find the exact JSON keys. If a key's value is not available, set it to null. Respond ONLY in valid JSON with a single top-level object containing a data key. The data object should include the following keys: RegistrationMark, ChassisNumber, EngineNumber, Make, Model, ModelNumber, Colour, VehicleCategory, ProbelledBy, NetWeighgt, GVMKg, Class, EngineCapacity, SeatingCapacity, RegistrationAuthority, YearOfMake, FirstRegistrationDate, CustomsClearanceNumber, InterpolNumber. Do not include any extra explanation or text outside the JSON block";
			}else if("100047".equals(companyId)) {
				
			}else if("100048".equals(companyId)) {
				
			}else if("100049".equals(companyId)) {
				
			}else if("100050".equals(companyId)) {
				prompt = "Analyze the provided import declaration data. Extract only the English values. Find the exact JSON keys. If a key's value is not available, set it to null. Respond ONLY in valid JSON with a single top-level object containing a data key. The data object should include the following keys: RegistrationAuthority, VehicleRegistrationNumber, VehicleIdendicationNumber, EngineNumber, Make, SeriesName, VehicleCategory, Driven, VehicleDescription, Tare, TypeOfIdentication, VehicleStatus, IdenticationNumber. Do not include any extra explanation or text outside the JSON block";
			}
		}
		
	 
		
		Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt),
                                        Map.of("inlineData", Map.of(
                                                "mimeType", fileType,
                                                "data", base64
                                        ))
                                )
                        )
                )
        );
		
		 HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);

	        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

	        ResponseEntity<JsonNode> response = restTemplate.exchange(url,HttpMethod.POST,entity,JsonNode.class);
	        
	        try {
	            ObjectMapper mapper = new ObjectMapper();
	            JsonNode root = response.getBody();

	            String rawText = root
	                    .path("candidates").get(0)
	                    .path("content")
	                    .path("parts").get(0)
	                    .path("text").asText();

	            // Remove json and trailing
	            if (rawText.startsWith("```json") || rawText.startsWith("```")) {
	                rawText = rawText.replaceFirst("^```json\\s*", "").replaceFirst("\\s*```$", "");
	                rawText = rawText.replace("\\", "");
	                
	            }

	            return mapper.readTree(rawText.trim());

	        }
	        catch (Exception e) {
	        	e.printStackTrace();
	            throw new RuntimeException("Failed to parse Gemini response", e);

	        }
	}
	
	private byte[] pdfToImage(MultipartFile file) throws IOException {
        try
            (PDDocument document = PDDocument.load(file.getInputStream()))
            {
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage image = renderer.renderImageWithDPI(0, 300);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", outputStream);
                return outputStream.toByteArray();
            }
    }
	
	private String convertImageToBase64(byte[] imageByte) {
        return Base64.getEncoder().encodeToString(imageByte);
    }
   
   private String getGeminiUrl() {
        return "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;
    }

@Override
public JsonNode getResult(Object req) {
	String companyId="", base64="",fileType="";
	try {
		//String request = req.toString();
	    Map<String,Object>	mapReq = mapper.convertValue(req, Map.class);
	    
	     companyId = mapReq.get("companyId") == null ? null : mapReq.get("companyId").toString();
	     base64 = mapReq.get("base64") == null ? null : mapReq.get("base64").toString();
	     fileType = mapReq.get("fileType") == null ? null : mapReq.get("fileType").toString();
	     
	     return ReplyFromGeminiAi(base64, companyId, fileType);
	     
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	return null;
}

private JsonNode ReplyFromGeminiAi(String base64, String companyId, String fileType) {
	String url = getGeminiUrl();
	
	String prompt ="";
	
	if(!StringUtils.isBlank(companyId)) {
		if("100046".equals(companyId)) {
			prompt = "Analyze the provided import declaration data. Extract only the English values. Find the exact JSON keys. If a key's value is not available, set it to null. Respond ONLY in valid JSON with a single top-level object containing a data key. The data object should include the following keys: RegistrationMark, ChassisNumber, EngineNumber, Make, Model, ModelNumber, Colour, VehicleCategory, ProbelledBy, NetWeighgt, GVMKg, Class, EngineCapacity, SeatingCapacity, RegistrationAuthority, YearOfMake, FirstRegistrationDate, CustomsClearanceNumber, InterpolNumber. Do not include any extra explanation or text outside the JSON block";
		}else if("100047".equals(companyId)) {
			
		}else if("100048".equals(companyId)) {
			
		}else if("100049".equals(companyId)) {
			
		}else if("100050".equals(companyId)) {
			prompt = "Analyze the provided import declaration data. Extract only the English values. Find the exact JSON keys. If a key's value is not available, set it to null. Respond ONLY in valid JSON with a single top-level object containing a data key. The data object should include the following keys: RegistrationAuthority, VehicleRegistrationNumber, VehicleIdendicationNumber, EngineNumber, Make, SeriesName, VehicleCategory, Driven, VehicleDescription, Tare, TypeOfIdentication, VehicleStatus, IdenticationNumber. Do not include any extra explanation or text outside the JSON block";
		}
	}
	
 
	
	Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                    Map.of(
                            "parts", List.of(
                                    Map.of("text", prompt),
                                    Map.of("inlineData", Map.of(
                                            "mimeType", fileType,
                                            "data", base64
                                    ))
                            )
                    )
            )
    );
	
	 HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url,HttpMethod.POST,entity,JsonNode.class);
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = response.getBody();

            String rawText = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            // Remove json and trailing
            if (rawText.startsWith("```json") || rawText.startsWith("```")) {
                rawText = rawText.replaceFirst("^```json\\s*", "").replaceFirst("\\s*```$", "");
                rawText = rawText.replace("\\", "");
                
            }

            return mapper.readTree(rawText.trim());

        }
        catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException("Failed to parse Gemini response", e);

        }
}
	


}
