package com.maan.whatsapp.metacontroller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.maan.whatsapp.insurance.PhoenixAsyncProcessThread;
import com.maan.whatsapp.service.common.CommonService;

import okhttp3.OkHttpClient;

@Service
@PropertySource("classpath:WebServiceUrl.properties")
public class PhoenixBoatswanaFlowServiceImpl implements PhoenixBoatswanaFlowService{
	
Logger log = LogManager.getLogger(PhoenixBoatswanaFlowServiceImpl.class);
	
	ObjectMapper mapper = new ObjectMapper();

	public static Gson printReq = new Gson();

	@Autowired
	private CommonService cs;
	
	@Autowired
	private PhoenixAsyncProcessThread thread;
	
	private static OkHttpClient okhttp = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();
	
	private static List<Map<String, String>> SAMPLE_DATA = new ArrayList<>();
	
	@Value("${wh.phoenix.customer.district}")
	private String district;
	
	@Value("${wh.get.phoenix.validation.api}")
	private String wh_get_phoenix_validation_api;
	
	@Value("${wh.phoenix.stp.motormake.api}")
	private String motorMake;
	
	@Value("${wh.phoenix.stp.motormodel.api}")
	private String motorModel;

	@Override
	public String createBoatswanaQuote(Map<String, Object> request) {

		String response="";
		String apiResponse="";
		String apiRequest="";
		
		try {
			
			Map<String,Object> data = (Map<String, Object>) request.get("data");
			String version = request.get("version") == null ? "" : request.get("version").toString();
			String screenName = request.get("screen") == null ? "" : request.get("screen").toString();
			String componentAction = request.get("component_action") == null ? "" : request.get("component_action").toString();
			String flowToken = request.get("flow_token") == null ? "" : request.get("flow_token").toString();
			
			Map<String,Object> returnResponse = new HashMap<String,Object>();
			returnResponse.put("version", version);
			returnResponse.put("screen", screenName);
			
			Map<String,Object> inputValidation = new HashMap<>();
			
			String sample_data = "[ {\"id\": \"0\", \"title\": \"--SELECT--\"} ]";
			String error_messages_1 = " {\"id\": \"\", \"\": \"\"}";
			
			List<Map<String,Object>> list = mapper.readValue(sample_data, List.class);
			
			String token = this.thread.getNamibiaToken();
			if("CUSTOMER_CREATION".equalsIgnoreCase(componentAction)) {
				
				String title = data.get("title") == null ? "" : data.get("title").toString().trim();
				String customerName = data.get("customer_name") == null ? "" : data.get("customer_name").toString().trim();
				String gender = data.get("gender") == null ? "" : data.get("gender").toString().trim();
				String occupation = data.get("occupation") == null ? "" : data.get("occupation").toString().trim();
				String mobileNo = data.get("mobile_number") == null ? "" : data.get("mobile_number").toString().trim();
				String emailId = data.get("email_id") == null ? "" : data.get("email_id").toString().trim();
				String idType = data.get("id_type") == null ? "" : data.get("id_type").toString().trim();
				String idNumber = data.get("id_number") == null ? "" : data.get("id_number").toString().trim();
				String region = data.get("region") == null ? "" : data.get("region").toString().trim();
				String distict = data.get("district") == null ? "" : data.get("district").toString().trim();
				String address = data.get("address") == null ? "" : data.get("address").toString().trim();
				
				//validations for  boxes
				
				if((!customerName.matches("[a-zA-Z.&() ]+")) && (!customerName.matches("^[a-zA-ZÀ-ÿ\\s'-]+$"))) {
					inputValidation.put("customer_name", "Please enter valid name");
				}
				if(!mobileNo.matches("[0-9]+")) {
					inputValidation.put("mobile_number", "Please enter Mobile Number");
				}else if(mobileNo.length()>10 || mobileNo.length()<8) {
					inputValidation.put("mobile_number", "Please enter valid Mobile Number");
				}
				if((!emailId.matches("^[a-zA-ZÀ-ÿ\\s'-]+$") || (!emailId.matches("^[.@]+$")))) {
					inputValidation.put("email_id", "Please enter valid Email Id");
				}
				if(!idNumber.matches("[a-zA-Z0-9-]+")) {
					inputValidation.put("id_number", "Please enter valid Id Number");
				}
				
				if(inputValidation.size() > 0) {
					
					Map<String,Object> requestMap = new HashMap<String,Object>();
					requestMap.put("BranchCode", "120");
					requestMap.put("InsuranceId", "100050");
					
					Map<String,Object> countryId = new HashMap<String, Object>();
					countryId.put("CountryId", "NAM");
					
					Map<String,Object> occReq = new HashMap<String,Object>();
					occReq.put("BranchCode", "120");
					occReq.put("InsuranceId", "100050");
					occReq.put("ProductId", "5");
					occReq.put("TitleType", "I");
					
					Map<String,Object> idtype = new HashMap<String,Object>();
					idtype.put("BranchCode", "120");
					idtype.put("InsuranceId", "100050");
					idtype.put("PolicyTypeId", "1");
					
					String req_1 = printReq.toJson(requestMap);
					String regionReq = printReq.toJson(countryId);
					String occupationReq = printReq.toJson(occReq);
					String idTypeReq = printReq.toJson(occReq);
					
					CompletableFuture<List<Map<String, String>>> titleDropDown = thread.getCustomerTitle(req_1,token);
					CompletableFuture<List<Map<String, String>>> regionDropDown = thread.getCustomerRegion(regionReq,token);
					CompletableFuture<List<Map<String,String>>> occupationDropDown = thread.getCustomerOccupation(occupationReq,token);
					CompletableFuture<List<Map<String,String>>> idTypeDropDown = thread.getCustomerIdType(idTypeReq,token);
					CompletableFuture<List<Map<String,String>>> genderDropDown = thread.getGender(req_1,token);
					//CompletableFuture<List<Map<String,String>>> districtDropDown = thread.getCustomerDistrict();
					
					CompletableFuture.allOf(titleDropDown,regionDropDown,occupationDropDown,idTypeDropDown,genderDropDown).join();
					
					Map<String, Object> error_messages = new HashMap<String, Object>();
					error_messages.put("error_messages", inputValidation);
					error_messages.put("title", titleDropDown.get().isEmpty() ? SAMPLE_DATA : titleDropDown.get());
					error_messages.put("region", regionDropDown.get().isEmpty() ? SAMPLE_DATA : regionDropDown.get());
					error_messages.put("occupation", occupationDropDown.get().isEmpty() ? SAMPLE_DATA : occupationDropDown.get());
					error_messages.put("id_type", idTypeDropDown.get().isEmpty() ? SAMPLE_DATA : idTypeDropDown.get());
					error_messages.put("customer_name", customerName);
					error_messages.put("gender", genderDropDown.get().isEmpty() ? SAMPLE_DATA : genderDropDown.get());
					error_messages.put("mobile_number", mobileNo);
					error_messages.put("email_id", emailId);
					error_messages.put("id_number", idNumber);
					error_messages.put("region", region);
					error_messages.put("district", distict);
					error_messages.put("address", address);
					returnResponse.put("action", "data_exchange");
					returnResponse.put("data", error_messages);
					
					response = printReq.toJson(returnResponse);

					return response;

				}
				else {
					Map<String, Object> map_vehicle = new HashMap<String, Object>();
					Map<String, Object> error_messages = new HashMap<String, Object>();

					Map<String, String> request_map = new HashMap<String, String>();
					request_map.put("BranchCode", "120");
					request_map.put("InsuranceId", "100050");

					String request_1 = printReq.toJson(request_map);
					
					CompletableFuture<List<Map<String, String>>> motorUsage = thread.getMotorUsage(request_1,token);
					CompletableFuture<List<Map<String, String>>> bodyType = thread.getMotorType(request_1,token);
					//CompletableFuture<List<Map<String, String>>> motorCategory = thread.getMotorCategory(request_1,token);
					CompletableFuture<List<Map<String, String>>> manufactureYear = thread.getManufactureYear();
					CompletableFuture<List<Map<String, String>>> vehColor = thread.getVehicleColor(request_1,token);
					
					CompletableFuture.allOf(motorUsage,bodyType,manufactureYear,vehColor).join();
					
					//return to next screen
					Map<String, Object> return_map = new HashMap<>();
					return_map.put("title", title);
					return_map.put("customer_name", customerName);
					return_map.put("gender", gender);
					return_map.put("occupation", occupation);
					return_map.put("mobile_number", mobileNo);
					return_map.put("email_id", emailId);
					return_map.put("id_type", idType);
					return_map.put("id_number", idNumber);
					return_map.put("region", region);
					return_map.put("district", distict);
					return_map.put("address", address);
					return_map.put("body_type", bodyType.get().isEmpty() ? list : bodyType.get());
					return_map.put("vehicle_make", list);
					return_map.put("veh_model", list);
					return_map.put("manufacture_year", manufactureYear.get().isEmpty() ? list : manufactureYear.get());
					return_map.put("vehicle_color", vehColor.get().isEmpty() ? list : vehColor.get());
					return_map.put("vehicle_usage", motorUsage.get().isEmpty() ? list : motorUsage.get());
					//return_map.put("motor_category", motorCategory.get().isEmpty() ? list : motorCategory.get());
					return_map.put("error_messages", error_messages);
					
					returnResponse.put("data", return_map);
					returnResponse.put("screen", "VEHICLE_INFORMATION");
					
					response = printReq.toJson(returnResponse);
					
					return response;
				}
			}
				else if("DISTRICT".equalsIgnoreCase(componentAction)) {
					String regionValue = data.get("region") == null ? "" : data.get("region").toString().trim();
					
					List<Map<String, String>> data_list = new ArrayList<Map<String, String>>();
					
					if(StringUtils.isBlank(regionValue)) {
						String api = this.district;
						
						Map<String,Object> districtReq = new HashMap<String,Object>();
						districtReq.put("CountryId", "NAM");
						districtReq.put("RegionCode", regionValue);
						
						apiRequest = printReq.toJson(districtReq);
						
						apiResponse = thread.callPhoenixApi(api,apiRequest,token);
						
						Map<String,Object> viewRes = mapper.readValue(apiResponse, Map.class);
						List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
								: (List<Map<String, String>>) viewRes.get("Result");
						
						data_list = apiData.stream().map(p -> {
							Map<String, String> values = new HashMap<>();
							values.put("id", p.get("CodeDesc") == null ? "" : p.get("Code").toString()+"~"+p.get("CodeDesc").toString());
							values.put("title", p.get("CodeDesc") == null ? "" : p.get("CodeDesc").toString());
							return values;
						}).collect(Collectors.toList());
						
					}else {
						data_list = SAMPLE_DATA;
					}
					Map<String, Object> district_list = new HashMap<String, Object>();
					district_list.put("district", data_list);
					returnResponse.put("data", district_list);
					response = printReq.toJson(returnResponse);
					return response;
				}
				else if("SAVE_VEHICLE".equalsIgnoreCase(componentAction)) {
					//Customer Screen Datas
					String title = data.get("title") == null ? "" : data.get("title").toString().trim();
					String customerName = data.get("customer_name") == null ? "" : data.get("customer_name").toString().trim();
					String gender = data.get("gender") == null ? "" : data.get("gender").toString().trim();
					String occupation = data.get("occupation") == null ? "" : data.get("occupation").toString().trim();
					String mobileNo = data.get("mobile_number") == null ? "" : data.get("mobile_number").toString().trim();
					String emailId = data.get("email_id") == null ? "" : data.get("email_id").toString().trim();
					String idType = data.get("id_type") == null ? "" : data.get("id_type").toString().trim();
					String idNumber = data.get("id_number") == null ? "" : data.get("id_number").toString().trim();
					String region = data.get("region") == null ? "" : data.get("region").toString().trim();
					String distict = data.get("district") == null ? "" : data.get("district").toString().trim();
					String address = data.get("address") == null ? "" : data.get("address").toString().trim();
					
					//Vehicle Screen Datas
					String motorUsage = data.get("vehicle_usage") == null ? "" :data.get("vehicle_usage").toString().trim();
					String bodyType = data.get("body_type") == null ? "" : data.get("body_type").toString().trim();
					String make = data.get("vehicle_make") == null ? "" : data.get("vehicle_make").toString().trim();
					String model = data.get("vehicle_model") == null ? "" : data.get("vehicle_model").toString().trim();
					//String motorCategory = data.get("motor_category") == null ? "" : data.get("motor_category").toString().trim();
					String regNo = data.get("registration_no") == null ? "" : data.get("registration_no").toString().trim();
					String chassisNo = data.get("chassis_number") == null ? "" : data.get("chassis_number").toString().trim();
					String engineNo = data.get("engine_number") == null ? "" : data.get("engine_number").toString().trim();
					String engineCapcaity = data.get("engine_capacity") == null ? "" : data.get("engine_capacity").toString().trim();
					String seatingCapacity = data.get("seating_capacity") == null ? "" : data.get("seating_capacity").toString().trim();
					String manufactureYear = data.get("manufacture_year") == null ? "" : data.get("manufacture_year").toString().trim();
					String color = data.get("vehicle_color") == null ? "" : data.get("vehicle_color").toString().trim();
					String grossWeight = data.get("gross_weight") == null ? "" : data.get("gross_weight").toString().trim();
					
					//validation Block
					if(!regNo.matches("[a-zA-Z0-9]+")) {
						inputValidation.put("registration_no", "Special characters not allowed,Please enter valid Reg No" );
					}
					if(!chassisNo.matches("[a-zA-Z0-9-]+")) {
						inputValidation.put("chassis_number", "Special characters not allowed,Please enter valid Chassis No");
					}
					if(!engineNo.matches("[a-zA-Z0-9-]+")) {
						inputValidation.put("engine_number", "Special characters not allowed,Please enter valid Engine No");
					}
					if(!engineCapcaity.matches("[0-9]+")) {
						inputValidation.put("engine_capacity", "Digits only allowed");
					}
					if(!seatingCapacity.matches("[0-9]+")) {
						inputValidation.put("seating_capacity", "Digits only allowed");
					}else {
						Map<String, String> request_map = new HashMap<String, String>();
						request_map.put("Type", "SEAT_CAPACITY");
						request_map.put("SeatingCapacity", seatingCapacity);
						request_map.put("InsuranceId", "100050");
						request_map.put("BranchCode", "120");
						request_map.put("BodyType", bodyType);
						String validationApi = wh_get_phoenix_validation_api;
						apiResponse = thread.callPhoenixApi(validationApi, mapper.writeValueAsString(request_map), token);

						Map<String, Object> validation_map = mapper.readValue(apiResponse, Map.class);
						Boolean status = (Boolean) validation_map.get("IsError");
						if (status) {
							Map<String, Object> seat_map = (Map<String, Object>) validation_map.get("Result");
							String seats = seat_map.get("Seating Capacity").toString();
							inputValidation.put("seating_capacity", "should be under " + seats + " or equal ");
						}

					}
					if(!grossWeight.matches("[0-9]+")) {
						inputValidation.put("gross_weight", "Digits only allowed");
					}
					
					if(!inputValidation.isEmpty() && inputValidation.size() > 0) {
						Map<String, String> request_map = new HashMap<String, String>();
						request_map.put("BranchCode", "120");
						request_map.put("InsuranceId", "100050");
						request_map.put("BodyId", bodyType);
						request_map.put("MakeId", make);
						
						Map<String, String> request_make = new HashMap<String, String>();
						request_make.put("BranchCode", "120");
						request_make.put("InsuranceId", "100050");
						request_make.put("BodyId", bodyType);
						

						String request_1 = printReq.toJson(request_map);
						String makeRequest = printReq.toJson(request_map);
						
						CompletableFuture<List<Map<String, String>>> motorUsageDropdown = thread.getMotorUsage(request_1, token);
						CompletableFuture<List<Map<String, String>>> bodyTypeDropDown = thread.getMotorType(request_1, token);
						CompletableFuture<List<Map<String, String>>> makeDropdown = thread.getMake(makeRequest,token);
						CompletableFuture<List<Map<String, String>>> modelDropdown = thread.getModel(request_1, token);
						//CompletableFuture<List<Map<String, String>>> motorCategoryDropdown = thread.getMotorCategory(request_1, token);
						CompletableFuture<List<Map<String, String>>> manYearDropdown = thread.getManufactureYear();
						CompletableFuture<List<Map<String, String>>> colorDropdown = thread.getVehicleColor(request_1, token);
						
						CompletableFuture.allOf(motorUsageDropdown,bodyTypeDropDown,makeDropdown,
								modelDropdown,manYearDropdown,colorDropdown).join();
						
						Map<String, Object> error_messages = new HashMap<String, Object>();
						error_messages.put("error_messages", inputValidation);
						error_messages.put("vehicle_usage", motorUsageDropdown.get().isEmpty() ? SAMPLE_DATA : motorUsageDropdown.get());
						error_messages.put("body_type", bodyTypeDropDown.get().isEmpty() ? SAMPLE_DATA : bodyTypeDropDown.get());
						error_messages.put("vehicle_make", makeDropdown.get().isEmpty() ? SAMPLE_DATA : makeDropdown.get());
						error_messages.put("vehicle_model", modelDropdown.get().isEmpty() ? SAMPLE_DATA : modelDropdown.get());
						//error_messages.put("motor_category", motorCategoryDropdown.get().isEmpty() ? SAMPLE_DATA : motorCategoryDropdown.get());
						error_messages.put("manufacture_year", manYearDropdown.get().isEmpty() ? SAMPLE_DATA : manYearDropdown.get());
						error_messages.put("vehicle_color", colorDropdown.get().isEmpty() ? SAMPLE_DATA : colorDropdown.get());
						
						error_messages.put("title", title);
						error_messages.put("customer_name", customerName);
						error_messages.put("gender", gender);
						error_messages.put("occupation", occupation);
						error_messages.put("mobile_number", mobileNo);
						error_messages.put("email_id", emailId);
						error_messages.put("id_type", idType);
						error_messages.put("id_number", idNumber);
						error_messages.put("region", region);
						error_messages.put("district", district);
						error_messages.put("address", address);
						
						returnResponse.put("action", "data_exchange");
						returnResponse.put("data", error_messages);
						
						response = printReq.toJson(returnResponse);

						return response;				
					}else {
						Map<String, String> request_map = new HashMap<String, String>();
						request_map.put("BranchCode", "120");
						request_map.put("InsuranceId", "100050");
						request_map.put("ProductId", "5");
						request_map.put("LoginId", "Wh_Nam_Broker");
						
						Map<String, String> request_insuredPeriod = new HashMap<String, String>();
						request_insuredPeriod.put("BranchCode", "120");
						request_insuredPeriod.put("InsuranceId", "100050");
						request_insuredPeriod.put("ProductId", "5");
						
						Map<String,String> noClaimReq = new HashMap<String, String>();
						noClaimReq.put("InsuranceId", "100050");
						noClaimReq.put("ItemType", "No_Claim_Bonus");
						
						String request_1 = printReq.toJson(request_map);
						String insuredPeriodRequest = printReq.toJson(request_map);
						String reqNoCliam = printReq.toJson(noClaimReq);
						
						CompletableFuture<List<Map<String,String>>> insuranceClassDropdown = thread.getInsuranceClass(request_1,token);
						CompletableFuture<List<Map<String,String>>> insuredPeriodDropdown = thread.getInsuredPeriod(insuredPeriodRequest,token);
						CompletableFuture<List<Map<String,String>>> noClaimBonusDropdown = thread.getNoClaimBonus(reqNoCliam,token);
						
						CompletableFuture.allOf(insuranceClassDropdown,insuredPeriodDropdown,noClaimBonusDropdown).join();
						
						//return to next screen
						Map<String, Object> map_policy = new HashMap<String, Object>();
						map_policy.put("title", title);
						map_policy.put("customer_name", customerName);
						map_policy.put("gender", gender);
						map_policy.put("occupation", occupation);
						map_policy.put("mobile_number", mobileNo);
						map_policy.put("email_id", emailId);
						map_policy.put("id_type", idType);
						map_policy.put("id_number", idNumber);
						map_policy.put("region", region);
						map_policy.put("district", distict);
						map_policy.put("address", address);
						map_policy.put("Chassisnumber", chassisNo);
						map_policy.put("Color", color);
						map_policy.put("EngineNumber", engineNo);
						map_policy.put("Grossweight", grossWeight);
						map_policy.put("ManufactureYear", manufactureYear);
						//map_policy.put("MotorCategory", motorCategory);
						map_policy.put("Motorusage", motorUsage);
						map_policy.put("Registrationnumber", regNo);
						map_policy.put("ResEngineCapacity", engineCapcaity);
						map_policy.put("SeatingCapacity", seatingCapacity);
						map_policy.put("Vehcilemodel", model);
						map_policy.put("VehicleType", bodyType);
						map_policy.put("Vehiclemake", make);
						map_policy.put("insurance_class",insuranceClassDropdown.get().isEmpty() ? list : insuranceClassDropdown.get());
						map_policy.put("insured_period",insuredPeriodDropdown.get().isEmpty() ? list : insuredPeriodDropdown.get());
						map_policy.put("comp_noclaim_bonus",noClaimBonusDropdown.get().isEmpty() ? list : noClaimBonusDropdown.get());
						map_policy.put("tpft_noclaim_bonus",noClaimBonusDropdown.get().isEmpty() ? list : noClaimBonusDropdown.get());
						map_policy.put("value", "default");
						Map<String, String> errorMap = new HashMap<>();
						map_policy.put("error_messages", errorMap);
						
						returnResponse.put("data", map_policy);
						returnResponse.put("screen", "POLICY_DETAILS");
						response = printReq.toJson(returnResponse);

						log.info("response" + response);

						return response;					
					}
				}
				else if("MAKE".equalsIgnoreCase(componentAction)) {
					String bodyType = data.get("body_type") == null ? "" : data.get("body_type").toString().trim();
					
					List<Map<String, String>> data_list = new ArrayList<Map<String, String>>();
					
					if (StringUtils.isNotBlank(bodyType)) {
						String api = this.motorMake;
						
						Map<String, Object> region_req = new HashMap<String, Object>();
						region_req.put("BodyId", bodyType);
						region_req.put("InsuranceId", "100050");
						region_req.put("BranchCode", "120");
						
						apiRequest = printReq.toJson(region_req);

						apiResponse = thread.callPhoenixApi(api, apiRequest, token);
						
						Map<String, Object> region_obj = mapper.readValue(apiResponse, Map.class);
						List<Map<String, Object>> result = (List<Map<String, Object>>) region_obj.get("Result");
						
						data_list = result.stream().map(p -> {
							Map<String, String> map = new HashMap<>();
							map.put("id", p.get("CodeDesc") == null ? "" : p.get("Code").toString()+"~"+p.get("CodeDesc").toString());
							map.put("title", p.get("CodeDesc") == null ? "" : p.get("CodeDesc").toString());
							return map;
						}).collect(Collectors.toList());
					}else {
						data_list = SAMPLE_DATA;
					}
					Map<String, Object> make_list = new HashMap<String, Object>();
					make_list.put("vehicle_make", data_list);
					returnResponse.put("data", make_list);
					response = printReq.toJson(returnResponse);
					return response;
				}
				else if("MODEL".equalsIgnoreCase(componentAction)) {
					String bodyType = data.get("body_type") == null ? "" : data.get("body_type").toString().trim();
					String vehMake = data.get("vehicle_make") == null ? "" : data.get("vehicle_make").toString().trim();
					List<Map<String, String>> data_list = new ArrayList<Map<String, String>>();
					
					if (!"00000".equals(vehMake) && StringUtils.isNotBlank(vehMake)) {
						String api = this.motorModel;
						
						Map<String, Object> region_req = new HashMap<String, Object>();
						region_req.put("BodyId", bodyType);
						region_req.put("InsuranceId", "100050");
						region_req.put("BranchCode", "120");
						region_req.put("MakeId", vehMake);
						
						apiRequest = printReq.toJson(region_req);

						apiResponse = thread.callPhoenixApi(api, apiRequest, token);

						Map<String, Object> region_obj = mapper.readValue(apiResponse, Map.class);
						List<Map<String, Object>> result = (List<Map<String, Object>>) region_obj.get("Result");
						
						data_list = result.stream().map(p -> {
							Map<String, String> map = new HashMap<>();
							map.put("id", p.get("CodeDesc") == null ? "" : p.get("Code").toString()+"~"+p.get("CodeDesc").toString());
							map.put("title", p.get("CodeDesc") == null ? "" : p.get("CodeDesc").toString());
							return map;
						}).collect(Collectors.toList());
					}else {
						data_list = SAMPLE_DATA;
					}
					Map<String, Object> make_list = new HashMap<String, Object>();
					make_list.put("veh_model", data_list);
					returnResponse.put("data", make_list);
					response = printReq.toJson(returnResponse);
					return response;
				}
				else if("POLICY_PAGE".equalsIgnoreCase(componentAction)) {
					//Customer Screen Datas
					String title = data.get("title") == null ? "" : data.get("title").toString().trim();
					String customerName = data.get("customer_name") == null ? "" : data.get("customer_name").toString().trim();
					String gender = data.get("gender") == null ? "" : data.get("gender").toString().trim();
					String occupation = data.get("occupation") == null ? "" : data.get("occupation").toString().trim();
					String mobileNo = data.get("mobile_number") == null ? "" : data.get("mobile_number").toString().trim();
					String emailId = data.get("email_id") == null ? "" : data.get("email_id").toString().trim();
					String idType = data.get("id_type") == null ? "" : data.get("id_type").toString().trim();
					String idNumber = data.get("id_number") == null ? "" : data.get("id_number").toString().trim();
					String region = data.get("region") == null ? "" : data.get("region").toString().trim();
					String distict = data.get("district") == null ? "" : data.get("district").toString().trim();
					String address = data.get("address") == null ? "" : data.get("address").toString().trim();
					
					//Vehicle Screen Datas
					String motorUsage = data.get("Motorusage") == null ? "" : data.get("Motorusage").toString().trim();
					String bodyType = data.get("VehicleType") == null ? "" : data.get("VehicleType").toString().trim();
					String make = data.get("Vehiclemake") == null ? "" : data.get("Vehiclemake").toString().trim();
					String model = data.get("Vehcilemodel") == null ? "" : data.get("Vehcilemodel").toString().trim();
					//String motorCategory = data.get("MotorCategory") == null ? "" : data.get("MotorCategory").toString().trim();
					String regNo = data.get("Registrationnumber") == null ? "" : data.get("Registrationnumber").toString().trim();
					String chassisNo = data.get("Chassisnumber") == null ? "" : data.get("Chassisnumber").toString().trim();
					String engineNo = data.get("EngineNumber") == null ? "" : data.get("EngineNumber").toString().trim();
					String engineCapcaity = data.get("ResEngineCapacity") == null ? "" : data.get("ResEngineCapacity").toString().trim();
					String seatingCapacity = data.get("SeatingCapacity") == null ? "" : data.get("SeatingCapacity").toString().trim();
					String manufactureYear = data.get("ManufactureYear") == null ? "" : data.get("ManufactureYear").toString().trim();
					String color = data.get("Color") == null ? "" : data.get("Color").toString().trim();
					String grossWeight = data.get("Grossweight") == null ? "" : data.get("Grossweight").toString().trim();
					
					//policy Screen Datas
					String insuranceClass = data.get("insurance_class") == null ? "" : data.get("insurance_class").toString().trim();
					String policyDate = data.get("policy_start_date") == null ? "" : data.get("policy_start_date").toString().trim();
					String insuredPeriod = data.get("insured_period") == null ? "" : data.get("insured_period").toString().trim();
					String comp_vehicle_si = data.get("comp_vehicle_si") == null ? "" : data.get("comp_vehicle_si").toString().trim();
					String comp_noclaim_bonus = data.get("comp_noclaim_bonus") == null ? "" : data.get("comp_noclaim_bonus").toString().trim();
					String comp_accessories_sumInured = data.get("comp_accessories_sumInured") == null ? "" : 
						data.get("comp_accessories_sumInured").toString().trim();
					String comp_windShield_sumInured = data.get("comp_windShield_sumInured") == null ? "" :
						data.get("comp_windShield_sumInured").toString().trim();
					String comp_extended_tppd_sumInsured = data.get("comp_extended_tppd_sumInsured") == null ? "" :
						data.get("comp_extended_tppd_sumInsured").toString().trim();
					String tpft_vehicle_si = data.get("tpft_vehicle_si") == null ? "" : data.get("tpft_vehicle_si").toString().trim();
					String tpft_noclaim_bonus = data.get("tpft_noclaim_bonus") == null ? "" :
						data.get("tpft_noclaim_bonus").toString().trim();
					String tpft_accessories_sumInured = data.get("tpft_accessories_sumInured") == null ? "" :
						data.get("tpft_accessories_sumInured").toString().trim();
					String tpft_windShield_sumInured = data.get("tpft_windShield_sumInured") == null ? "" :
						data.get("tpft_windShield_sumInured").toString().trim();
					String tpft_extended_tppd_sumInsured = data.get("tpft_extended_tppd_sumInsured") == null ? "" :
						data.get("tpft_extended_tppd_sumInsured").toString().trim();
						
					//String default_vehicle_si = data.get("default_vehicle_si") == null ? "" : data.get("default_vehicle_si").toString().trim();
					
					//Validation Block
					if("1".equals(insuranceClass)) {
						if(!comp_vehicle_si.matches("[0-9.]+")) {
							inputValidation.put("comp_vehicle_si", "Digits only allows");
						}
						if(!comp_accessories_sumInured.matches("[0-9.]+")) {
							inputValidation.put("comp_accessories_sumInured", "Digits only allows");
						}
						if(!comp_windShield_sumInured.matches("[0-9.]+")) {
							inputValidation.put("comp_windShield_sumInured", "Digits only allows");
						}
						if(!comp_extended_tppd_sumInsured.matches("[0-9.]+")) {
							inputValidation.put("comp_extended_tppd_sumInsured", "Digits only allows");
						}
					}else if("2".equals(insuranceClass)) {
						if(!tpft_vehicle_si.matches("[0-9.]+")) {
							inputValidation.put("tpft_vehicle_si", "Digits only allows");
						}
						if(!tpft_accessories_sumInured.matches("[0-9.]+")) {
							inputValidation.put("tpft_accessories_sumInured", "Digits only allows");
						}
						if(!tpft_windShield_sumInured.matches("[0-9.]+")) {
							inputValidation.put("tpft_windShield_sumInured", "Digits only allows");
						}
						if(!tpft_extended_tppd_sumInsured.matches("[0-9.]+")) {
							inputValidation.put("tpft_extended_tppd_sumInsured", "Digits only allows");
						}
					}
					
					if(inputValidation.size() > 0) {
						Map<String, Object> error_messages = new HashMap<String, Object>();
						error_messages.put("error_messages", inputValidation);

						returnResponse.put("action", "data_exchange");
						returnResponse.put("data", error_messages);

						response = printReq.toJson(returnResponse);
						return response;
					}
					else {
						Map<String, Object> extension_message_response = new HashMap<String, Object>();
						Map<String, Object> params = new HashMap<String, Object>();
						Map<String, Object> param_map = new HashMap<String, Object>();
						
						params.put("title", title);
						params.put("customer_name", customerName);
						params.put("gender", gender);
						params.put("occupation", occupation);
						params.put("mobile_number", mobileNo);
						params.put("email_id", emailId);
						params.put("id_type", idType);
						params.put("id_number", idNumber);
						params.put("address", address);
						params.put("distict", distict);
						params.put("region", region);
						params.put("Registrationnumber", regNo);	
						params.put("BodyType",bodyType);
						params.put("VehicleUsage", motorUsage);
						params.put("Chassisnumber", chassisNo);
						params.put("Color", color);
						params.put("EngineNumber", engineNo);
						params.put("Grossweight", grossWeight);
						params.put("ManufactureYear", manufactureYear);
						//params.put("MotorCategory", motorCategory);
						params.put("SeatingCapacity", seatingCapacity);
						params.put("Vehcilemodel", model);
						params.put("Vehiclemake", make);
						params.put("VehicleType", bodyType);
						params.put("ResEngineCapacity", engineCapcaity);
						params.put("Motorusage", motorUsage);
						params.put("comp_vehicle_si", comp_vehicle_si);
						params.put("tpft_vehicle_si", tpft_vehicle_si);
						params.put("insured_period", insuredPeriod);
						params.put("policy_start_date", policyDate);
						params.put("insurance_class", insuranceClass);
						params.put("comp_noclaim_bonus", comp_noclaim_bonus);
						params.put("comp_accessories_sumInured", comp_accessories_sumInured);
						params.put("comp_windShield_sumInured", comp_windShield_sumInured);
						params.put("comp_extended_tppd_sumInsured", comp_extended_tppd_sumInsured);
						params.put("tpft_noclaim_bonus", tpft_noclaim_bonus);
						params.put("tpft_accessories_sumInured", tpft_accessories_sumInured);
						params.put("tpft_windShield_sumInured", tpft_windShield_sumInured);
						params.put("tpft_extended_tppd_sumInsured", tpft_extended_tppd_sumInsured);
						
						param_map.put("params", params);
						extension_message_response.put("extension_message_response", param_map);

						returnResponse.put("screen", "SUCCESS");
						returnResponse.put("data", extension_message_response);

						response = printReq.toJson(returnResponse);

						return response;
						
					}
				}	
		}catch(Exception e) {
			e.printStackTrace();
			log.error(e);
		}
		return response;
	
	}

	@Override
	public Map<String, Object> boatswanaFlowScreenData() {

		
		try {
			
			String token = thread.getZambiaToken();
			
			Map<String,Object> requestMap = new HashMap<String,Object>();
			requestMap.put("BranchCode", "");
			requestMap.put("InsuranceId", "");
			
			Map<String,Object> countryId = new HashMap<String, Object>();
			countryId.put("CountryId", "");
			
			Map<String,Object> occReq = new HashMap<String,Object>();
			occReq.put("BranchCode", "");
			occReq.put("InsuranceId", "");
			occReq.put("ProductId", "5");
			occReq.put("TitleType", "I");
			
			Map<String,Object> idtype = new HashMap<String,Object>();
			idtype.put("BranchCode", "");
			idtype.put("InsuranceId", "");
			idtype.put("PolicyTypeId", "1");
			
			String request1 = printReq.toJson(requestMap);
			String req2 = printReq.toJson(countryId);
			String req3 = printReq.toJson(occReq);
			String req4 = printReq.toJson(idtype);
			
			log.info("Vehicle api Start time is : " + new Date());
			
			CompletableFuture<List<Map<String,String>>> cusTitle = thread.getCustomerTitle(request1, token);
			CompletableFuture<List<Map<String,String>>> cusRegion = thread.getCustomerRegion(req2, token);
			CompletableFuture<List<Map<String,String>>> cusOccupation = thread.getCustomerOccupation(req3, token);
			CompletableFuture<List<Map<String,String>>> cusIdType = thread.getCustomerIdType(req4, token);
			CompletableFuture<List<Map<String,String>>> cusGender = thread.getGender(request1, token);
			
			CompletableFuture.allOf(cusTitle,cusRegion,cusOccupation,cusIdType,cusGender).join();
			
			Map<String, String> error_message = new HashMap<String, String>();
			error_message.put("", "");
			
			Map<String,Object> data = new HashMap<String,Object>();
			data.put("title", cusTitle.get());
			data.put("region", cusRegion.get());
			data.put("occupation", cusOccupation.get());
			data.put("id_type", cusIdType.get());
			data.put("gender", cusGender.get());
			data.put("error_messages", error_message);
			
			log.info("Vehicle api End time is : " + mapper.writeValueAsString(data));
			
			Map<String, Object> flow_action_payload = new HashMap<String, Object>();
			flow_action_payload.put("screen", "CUSTOMER_DETAILS");
			flow_action_payload.put("data", data);

			return flow_action_payload;
			
		}catch(Exception e) {
			e.printStackTrace();
			log.error(e);
		}
		return null;
	
	}

}
