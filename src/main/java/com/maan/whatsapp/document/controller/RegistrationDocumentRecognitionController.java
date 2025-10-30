package com.maan.whatsapp.document.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.tika.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maan.whatsapp.document.service.RegistrationDocumentRecognitionService;

@RestController
@RequestMapping("/api/recognition")
public class RegistrationDocumentRecognitionController {

	@Autowired
	private RegistrationDocumentRecognitionService service;
	
	@Autowired
	private ObjectMapper mapper;
	
	@PostMapping("/registration/document")
	public ResponseEntity<?> regDocRegistration(@RequestParam("file") MultipartFile file,@RequestParam("req") Object req){
		System.out.println("Document inserts");
		
		System.out.println("request: "+req);
		
		try {
			JsonNode resp = service.getDocumentResult(file,req);
			boolean flag = false;
            boolean allNull = false;
            String request = req.toString();
            Map<String, Object> mapReq = mapper.readValue(request, Map.class);
            String companyId = mapReq.get("companyId") == null ? null : mapReq.get("companyId").toString();
            if(!resp.get("data").isEmpty()) {
            	String jsonResp = resp.toString();
	            ObjectMapper mapper = new ObjectMapper();
	            Map<String,Object> root = mapper.readValue(jsonResp, Map.class);
	            Map<String, Object> data = (Map<String, Object>) root.get("data");
	            if(!StringUtils.isBlank(companyId)) {
	            	String regNo="",chassisNo="",make="",model="",engineNo="";
	    			if("100046".equals(companyId)) {
	    				 regNo = data.get("RegistrationMark") == null ? null : data.get("RegistrationMark").toString();
	    	             chassisNo = data.get("ChassisNumber") == null ? null : data.get("ChassisNumber").toString();
	    	             make = data.get("Make") == null ? null : data.get("Make").toString();
	    	             model = data.get("Model") == null ? null : data.get("Model").toString();
	    	            engineNo=data.get("EngineNumber") == null ? null : data.get("EngineNumber").toString();
	    			}else if("100047".equals(companyId)) {
	    				
	    			}else if("100048".equals(companyId)) {
	    				
	    			}else if("100049".equals(companyId)) {
	    				
	    			}else if("100050".equals(companyId)) {
	    				regNo = data.get("VehicleRegistrationNumber") == null ? null : data.get("VehicleRegistrationNumber").toString();
	    	             chassisNo = data.get("VehicleIdendicationNumber") == null ? null : data.get("VehicleIdendicationNumber").toString();
	    	             make = data.get("Make") == null ? null : data.get("Make").toString();
	    	             model = data.get("SeriesName") == null ? null : data.get("SeriesName").toString();
	    	            engineNo=data.get("EngineNumber") == null ? null : data.get("EngineNumber").toString();
	    			}
	    			if(regNo == null) {
		            	flag = true;
		            } if(chassisNo == null) {
		            	flag = true;
		            } if(make == null) {
		            	flag = true;
		            }if(model == null) {
		            	flag = true;
		            }if(engineNo == null) {
		            	flag = true;
		            }
		            
		            allNull = data.values().stream().allMatch(value -> value == null);
	            }
	            if(resp.get("data").isEmpty() || allNull || flag) {
	   			 Map<String,Object> errorMap = new HashMap<>();
	   			 errorMap.put("Message", "Failed");
	   			 errorMap.put("IsError", true);
	   			 errorMap.put("Result", null);
	   			 errorMap.put("ErroCode", 0);
	   			 Map<String,Object> errorMessage = new HashMap<>();
	   			 errorMessage.put("Code", "101");
	   			 errorMessage.put("Field", "PASSPORT UPLOAD");
	   			 errorMessage.put("Message", "INVALID DOCUMENT: PLEASE UPLOAD PASSPORT");
	   			 errorMessage.put("FieldLocal", "");
	   			 errorMessage.put("MessageLocal", "");
	   			 errorMap.put("ErrorMessage", Arrays.asList(errorMessage));
	   			 
		            return ResponseEntity.status(200).body(errorMap);
		            }
	            return ResponseEntity.ok(resp);
	    		}      
		}catch(Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(417).body("Error Processing document: "+e.getMessage());
		}
		return null;
	}
	
	@PostMapping("/document")
	public ResponseEntity<?> regDocunentRegistration(@RequestBody Object req){
		System.out.println("Document inserts");
		
		System.out.println("request: "+req);
		
		try {
			JsonNode resp = service.getResult(req);
			boolean flag = false;
            boolean allNull = false;
          //  String request = req.toString();
            Map<String,Object>	mapReq = mapper.convertValue(req, Map.class);
            String companyId = mapReq.get("companyId") == null ? null : mapReq.get("companyId").toString();
            if(!resp.get("data").isEmpty()) {
            	String jsonResp = resp.toString();
	            ObjectMapper mapper = new ObjectMapper();
	            Map<String,Object> root = mapper.readValue(jsonResp, Map.class);
	            Map<String, Object> data = (Map<String, Object>) root.get("data");
	            if(!StringUtils.isBlank(companyId)) {
	            	String regNo="",chassisNo="",make="",model="",engineNo="";
	    			if("100046".equals(companyId)) {
	    				 regNo = data.get("RegistrationMark") == null ? null : data.get("RegistrationMark").toString();
	    	             chassisNo = data.get("ChassisNumber") == null ? null : data.get("ChassisNumber").toString();
	    	             make = data.get("Make") == null ? null : data.get("Make").toString();
	    	             model = data.get("Model") == null ? null : data.get("Model").toString();
	    	            engineNo=data.get("EngineNumber") == null ? null : data.get("EngineNumber").toString();
	    			}else if("100047".equals(companyId)) {
	    				
	    			}else if("100048".equals(companyId)) {
	    				
	    			}else if("100049".equals(companyId)) {
	    				
	    			}else if("100050".equals(companyId)) {
	    				regNo = data.get("VehicleRegistrationNumber") == null ? null : data.get("VehicleRegistrationNumber").toString();
	    	             chassisNo = data.get("VehicleIdendicationNumber") == null ? null : data.get("VehicleIdendicationNumber").toString();
	    	             make = data.get("Make") == null ? null : data.get("Make").toString();
	    	             model = data.get("SeriesName") == null ? null : data.get("SeriesName").toString();
	    	            engineNo=data.get("EngineNumber") == null ? null : data.get("EngineNumber").toString();
	    			}
	    			if(regNo == null) {
		            	flag = true;
		            } if(chassisNo == null) {
		            	flag = true;
		            } if(make == null) {
		            	flag = true;
		            }if(model == null) {
		            	flag = true;
		            }if(engineNo == null) {
		            	flag = true;
		            }
		            
		            allNull = data.values().stream().allMatch(value -> value == null);
	            }
	            if(resp.get("data").isEmpty() || allNull || flag) {
	   			 Map<String,Object> errorMap = new HashMap<>();
	   			 errorMap.put("Message", "Failed");
	   			 errorMap.put("IsError", true);
	   			 errorMap.put("Result", null);
	   			 errorMap.put("ErroCode", 0);
	   			 Map<String,Object> errorMessage = new HashMap<>();
	   			 errorMessage.put("Code", "101");
	   			 errorMessage.put("Field", "PASSPORT UPLOAD");
	   			 errorMessage.put("Message", "INVALID DOCUMENT: PLEASE UPLOAD PASSPORT");
	   			 errorMessage.put("FieldLocal", "");
	   			 errorMessage.put("MessageLocal", "");
	   			 errorMap.put("ErrorMessage", Arrays.asList(errorMessage));
	   			 
		            return ResponseEntity.status(200).body(errorMap);
		            }
	            return ResponseEntity.ok(resp.get("data"));
	    		}      
		}catch(Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(417).body("Error Processing document: "+e.getMessage());
		}
		return null;
	}
}
