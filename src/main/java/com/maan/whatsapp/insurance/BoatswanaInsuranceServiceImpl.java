package com.maan.whatsapp.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.maan.whatsapp.config.exception.WhatsAppValidationException;
import com.maan.whatsapp.response.error.Error;

@Service
public class BoatswanaInsuranceServiceImpl implements BoatswanaInsuranceService{
	
	@Autowired
	private ObjectMapper mapper;
	
	@Value("${wh.phoenix.getmasterids.api}")
	private String masterIdsApi;
	
	@Value("${wh.phoenix.customersave.api}")
	private String saveCustomerApi;
	
	@Value("${wh.phoenix.getcustomer.api}")
	private String getCustomerApi;
	
	@Value("${wh.phoenix.savemotordetails.api}")
	private String motorSaveApi;
	
	@Value("${wh.phoenix.calc.api}")
	private String calcApi;
	
	@Value("${wh.phoenix.logincreation.api}")
	private String loginCreationApi;
	
	@Value("${wh.phoenix.buypolicy.api}")
	private String buyPolicy;
	
	@Value("${wh.phoenix.makepayment.api}")
	private String makePayment;
	
	@Value("${wh.phoenix.insertpayment.api}")
	private String insertPayment;

	@Value("${phoenix.motor.payment.link}")
	private String phoenixMotorPaymentlink;
	
	@Value("${wh.phoenix.getallmotordetails}")
	private String getAllMotorDetailsApi;
	
	@Autowired
	private Gson objectPrint;
	
	Logger log = LogManager.getLogger(getClass());
	
	@Autowired
	@Lazy
	private PhoenixAsyncProcessThread thread;

	@Override
	public Object quoteGenerationBoatswana(Object req)
			throws JsonProcessingException, JsonMappingException, WhatsAppValidationException {
		
		String response = "";
		String exception = "";
		List<Error> errorList = new ArrayList<>(2);
		Map<String, Object> botResponceData = new HashMap<String, Object>();

		Map<String, Object> data = mapper.convertValue(req, Map.class);
		
		// Customer Request Mapping
				
				String customerName = data.get("name") == null ? "" : data.get("name").toString();
			//	String title = data.get("Title") == null ? "" : data.get("Title").toString();
			//	String gender = data.get("Gender") == null ? "" : data.get("Gender").toString();
			//	String occupation = data.get("Occupation") == null ? "" : data.get("Occupation").toString();
				String mobileNo = data.get("phone_number") == null ? "" : data.get("phone_number").toString();
				String email = data.get("email") == null ? "" : data.get("email").toString();
				String idType = data.get("identity_type") == null ? "" : data.get("identity_type").toString();
				String idNumber = data.get("id_number") == null ? "" : data.get("id_number").toString();
			//	String region = data.get("Region") == null ? "" : data.get("Region").toString();
			//	String address = data.get("Address") == null ? "" : data.get("Address").toString();

				// Motor Request Mapping
				String motorUsage = data.get("vehicle_usage") == null ? "" : data.get("vehicle_usage").toString();
				String bodyType = data.get("vehicle_bodytype") == null ? "" : data.get("vehicle_bodytype").toString();
				String make = data.get("vehicle_make") == null ? "" : data.get("vehicle_make").toString();
				String model = data.get("vehicle_model") == null ? "" : data.get("vehicle_model").toString();
				String regNo = data.get("registration_number") == null ? "" : data.get("registration_number").toString();
			//	String engineNo = data.get("Engine Number") == null ? "" : data.get("Engine Number").toString();
			//	String chassisNo = data.get("Chassis Number") == null ? "" : data.get("Chassis Number").toString();
			//	String enginecapacity = data.get("cubic_capacity") == null ? "" : data.get("cubic_capacity").toString();
			//	String seatingCapacity = data.get("seating_capacity") == null ? "" : data.get("seating_capacity").toString();
			//	String manYear = data.get("Manufacture Year") == null ? "" : data.get("Manufacture Year").toString();
			//	String color = data.get("Color") == null ? "" : data.get("Color").toString();
			//	String cubicCapacity = data.get("cubic_capacity") == null ? "" : data.get("cubic_capacity").toString();
			//	String fuel = data.get("fuel_type") == null ? "" : data.get("fuel_type").toString();

				// PolicyDetails Request Mapping
				String insuranceClass = data.get("insurance_type") == null ? "" : data.get("insurance_type").toString();
				String policyDate = data.get("policy_start_date") == null ? "" : data.get("policy_start_date").toString();
				String sum_insured = data.get("vehicle_sum") == null ? "" : data.get("vehicle_sum").toString();
			//	String extended_tppd_si = data.get("Extended Tppd Si") == null ? "" : data.get("Extended Tppd Si").toString();
				
				String insuranceClassId ="",motorUsageId="",bodyTypeId="",makeId="",modelId="",idTypeId="";
				
				Map<String,Object> idTypeMap = new HashMap<>();
				idTypeMap.put("Desc", idType);
				idTypeMap.put("MasterType", "CUSTOMER_IDTYPE");
				idTypeMap.put("InsuranceId", "100049");
				
				try {
					String idTypeReq = mapper.writeValueAsString(idTypeMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callBotswanaComApi(masterApi, idTypeReq);
					log.info("Master Api Response: "+apiResponse);
					Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
					
					if(masterApiResult.isEmpty() || masterApiResult == null) {
						String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
						response = errorMessage;
						return response;
					}else {
						idTypeId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
					}
					
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				Map<String,Object> insuranceClassMap = new HashMap<>();
				insuranceClassMap.put("Desc", insuranceClass);
				insuranceClassMap.put("MasterType", "INSURANCE_CLASS");
				insuranceClassMap.put("InsuranceId", "100047");
				
				try {
					String insuranceClassReq = mapper.writeValueAsString(insuranceClassMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callBotswanaComApi(masterApi, insuranceClassReq);
					log.info("Master Api Response: "+apiResponse);

					Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
					
					if(masterApiResult.isEmpty() || masterApiResult == null) {
						String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
						response = errorMessage;
						return response;
					}else {
						insuranceClassId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				Map<String,Object> motorusageMap = new HashMap<>();
				motorusageMap.put("Desc", motorUsage);
				motorusageMap.put("MasterType", "MOTOR_USAGE");
				motorusageMap.put("InsuranceId", "100047");
				
				try {
					String motorUsageReq = mapper.writeValueAsString(motorusageMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callBotswanaComApi(masterApi,motorUsageReq);
					
					log.info("Master Api Response: "+apiResponse);
					Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = cust.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
					
				   
					if(masterApiResult == null) {
						String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}else {
						motorUsageId = masterApiResult.get("Response")==null?"":masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				Map<String,Object> bodyTypeMap = new HashMap<>();
				bodyTypeMap.put("Desc", bodyType);
				bodyTypeMap.put("MasterType", "BODY_TYPE");
				bodyTypeMap.put("InsuranceId", "100047");
				
				try {
					String bodyTypeReq = mapper.writeValueAsString(bodyTypeMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callBotswanaComApi(masterApi,bodyTypeReq);
					
					log.info("Master Api Response: "+apiResponse);
					Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = cust.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
					
				   
					if(masterApiResult == null) {
						String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}else {
						bodyTypeId = masterApiResult.get("Response")==null?"":masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				Map<String,Object> makeMap = new HashMap<>();
				makeMap.put("Desc", make);
				makeMap.put("MasterType", "VEHICLE_MAKE");
				makeMap.put("InsuranceId", "100047");
				
				try {
					String makeReq = mapper.writeValueAsString(makeMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callBotswanaComApi(masterApi,makeReq);
					
					log.info("Master Api Response: "+apiResponse);
					Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = cust.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
					
				   
					if(masterApiResult == null) {
						String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}else {
						makeId = masterApiResult.get("Response")==null?"":masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				Map<String,Object> modelMap = new HashMap<>();
				modelMap.put("Desc", model);
				modelMap.put("MasterType", "VEHICLE_MODEL");
				modelMap.put("InsuranceId", "100047");
				
				try {
					String modelReq = mapper.writeValueAsString(modelMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callBotswanaComApi(masterApi,modelReq);
					
					log.info("Master Api Response: "+apiResponse);
					Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = cust.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
					
				   
					if(masterApiResult == null) {
						String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}else {
						modelId = masterApiResult.get("Response")==null?"99999":masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				//==============================SAVE VEHICLE INFO BLOCK START=============================================
			/*	log.info("SAVE VEHICLE INFO START: "+new Date());
				
				Map<String,Object> vehicleInfo = new HashMap<String,Object>();
				vehicleInfo.put("Insuranceid", "100047");
				vehicleInfo.put("BranchCode", "119");
				vehicleInfo.put("AxelDistance", 1);
				vehicleInfo.put("Chassisnumber", chassisNo);
				vehicleInfo.put("Color", colorId);
				vehicleInfo.put("CreatedBy", "SZL_Whatsapp");
				vehicleInfo.put("DisplacementInCM3", "0");
				vehicleInfo.put("EngineNumber", engineNo);
				//vehicleInfo.put("FuelType", null);
				vehicleInfo.put("Grossweight", grossWeight);
				vehicleInfo.put("ManufactureYear", manYear);
				//vehicleInfo.put("MotorCategory", null);
				vehicleInfo.put("Motorusage", motorUsage);
				//vehicleInfo.put("NumberOfAxels", null);
				vehicleInfo.put("OwnerCategory", "1");
				vehicleInfo.put("Registrationnumber", regNo);
				vehicleInfo.put("ResEngineCapacity", enginecapacity);
				vehicleInfo.put("ResOwnerName", customerName);
				vehicleInfo.put("ResStatusCode", "Y");
				vehicleInfo.put("ResStatusDesc", "None");
				vehicleInfo.put("SeatingCapacity", seatingCapacity);
				vehicleInfo.put("HorsePower", "0");
				//vehicleInfo.put("Tareweight", null);
				vehicleInfo.put("Vehcilemodel", model);
				vehicleInfo.put("VehcilemodelId", modelId);
				vehicleInfo.put("VehicleType", bodyType);
				vehicleInfo.put("Vehiclemake", make);
				vehicleInfo.put("VehiclemakeId", makeId);
				//vehicleInfo.put("DisplacementInCM3", null);
				//vehicleInfo.put("NumberOfCylinders", 0);
				//vehicleInfo.put("PlateType", null);
				
				try {
					String saveVehicleInfo = saveVehicleInfoApi;
					log.info("Save Vehicle Info Calling: "+saveVehicleInfo);
					String vehReq = mapper.writeValueAsString(vehicleInfo);
					log.info("Save Vehicle Info Calling: "+saveVehicleInfo);
					log.info("Save Vehicle Request: "+vehReq);
				//	String token = thread.getSwazilandToken();
					String vehResponse = thread.callBotswanaComApi(saveVehicleInfoApi,vehReq);
					Map<String,Object> mapRes = mapper.readValue(vehResponse, Map.class);
					log.info("Save Vehicle Info Response: "+mapRes);
					
					Map<String,Object> vehInfoResult = mapRes.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(mapRes.get("Result")), Map.class);
					
					if(vehInfoResult==null) {
						String errorMessgae = mapRes.get("ErrorMessage") == null ? "" : mapRes.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}
				}
				catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				log.info("SAVE VEHICLE INFO END: "+new Date());
				
				*/		
				//==============================SAVE VEHICLE INFO BLOCK END=============================================
				
				//==============================CUSTOMER CREATION BLOCK START=============================================
				
				
				
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
				LocalDate startDate = LocalDate.parse(policyDate, formatter);
				LocalDate endDate = startDate.plusDays(364);
				String policyEndDate = endDate.format(formatter);
				
				LocalDate curDate = LocalDate.parse(policyDate,formatter);
				LocalDate minusDate = curDate.minusYears(18);
				String cusDob = minusDate.format(formatter);
				
				log.info("CUSTOMER CREATION BLOCK START: "+new Date());
				Map<String,Object> customerCreation = new HashMap<String,Object>();
				customerCreation.put("Activities", "1");
				customerCreation.put("Address1", null);
				customerCreation.put("Address2", "");
				customerCreation.put("AppointmentDate", "");
				customerCreation.put("BranchCode", "121");
				customerCreation.put("BrokerBranchCode", "1");
				customerCreation.put("BusinessType", 1);
				customerCreation.put("CityCode", null);
				customerCreation.put("CityName", null);//check district or region
				customerCreation.put("ClientName", customerName);
				customerCreation.put("Clientstatus", "Y");
				customerCreation.put("Country", "BWA");
				customerCreation.put("CountryName", "Botswana");
				customerCreation.put("CreatedBy", "guest_Botswana");//create login for whatsapp bot
				customerCreation.put("CustomerAsInsurer", "N");
				customerCreation.put("CustomerReferenceNo", "");
				customerCreation.put("DobOrRegDate", cusDob); //cusDob
				customerCreation.put("Email1", email);
				customerCreation.put("Email2", null);
				customerCreation.put("Email3", null);
				customerCreation.put("ExpiryDate", null);
				customerCreation.put("Fax", null);
				customerCreation.put("Gender", "M");
				customerCreation.put("IdNumber", idNumber);//idNumber
				customerCreation.put("IdType", idTypeId);//idTypeId
				customerCreation.put("InsuranceId", "100047");
				customerCreation.put("IsTaxExempted", "N");
				customerCreation.put("Language", "1");
				customerCreation.put("LastName", " ");
				customerCreation.put("MaritalStatus", null);//check
				customerCreation.put("MiddleName", "");
				customerCreation.put("MobileCode1", "267");
				customerCreation.put("MobileCodeDesc1", "1");
				customerCreation.put("MobileNo1", mobileNo);
				customerCreation.put("MobileNo2", mobileNo);
				customerCreation.put("MobileNo3", null);
				customerCreation.put("Nationality", null);
				customerCreation.put("Occupation", "");
				customerCreation.put("OtherOccupation", "");
				customerCreation.put("PhoneNoCode", null);
				customerCreation.put("PinCode", null);
				customerCreation.put("Placeofbirth", "chennai");
				customerCreation.put("PolicyHolderType", "1");
				customerCreation.put("PolicyHolderTypeid", "8");
				customerCreation.put("PreferredNotification", null);
				customerCreation.put("ProductId", "5");
				customerCreation.put("RegionCode", null);
				customerCreation.put("RiskAssessmentDate", null);
				customerCreation.put("SaveOrSubmit", "Save");
				customerCreation.put("SocioProfessionalCategory", null);
				customerCreation.put("StateCode", null);
				customerCreation.put("StateName", null);
				customerCreation.put("Status", "Y");
				customerCreation.put("Street", null);
				customerCreation.put("TaxExemptedId", null);
				customerCreation.put("TelephoneNo1", "");
				customerCreation.put("TelephoneNo2", null);
				customerCreation.put("TelephoneNo3", null);
				customerCreation.put("Title", "1");
				customerCreation.put("Type", null);
				customerCreation.put("VipFlag", null);
				customerCreation.put("VrTinNo", null);
				customerCreation.put("WhatsappCode", "267");
				customerCreation.put("WhatsappDesc", "1");
				customerCreation.put("WhatsappNo", mobileNo);
				customerCreation.put("Zone", "1");
				
				String custRefNo = "";
				
				try {
					String cusReq = mapper.writeValueAsString(customerCreation);
					String cusSaveApi = saveCustomerApi;
					log.info("Customer Save Calling: "+cusSaveApi);
					log.info("Customer Save Request: "+cusReq);
					
					String apiResponse = thread.callBotswanaComApi(cusSaveApi,cusReq);
					
					log.info("Customer Save Response: "+apiResponse);
					Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> custResult = cust.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
					
					if(custResult == null) {
						String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
						
						String body = errorMessgae.substring(1, errorMessgae.length() - 1); // remove '[' and ']'
					    body = body.substring(1, body.length() - 1); // remove '{' and '}'

					    // Split only on commas that separate fields (NOT inside message)
					    String[] parts = body.split(", (?=[A-Za-z]+=)");

					    Map<String, String> map = new HashMap<>();

					    for (String part : parts) {
					        String[] kv = part.split("=", 2); // split only on the FIRST '='
					        String key = kv[0].trim();
					        String value = kv[1].trim();
					        if ("null".equals(value)) value = null;
					        map.put(key, value);
					    }

					    // Now you can extract directly
					    String code = map.get("Code");
					    String message = map.get("Message");
						if(("MOBILE_EXISTS".equalsIgnoreCase(code)) || ("EMAIL_EXISTS".equalsIgnoreCase(code)) ||
								("ID_NUMBER_EXISTS".equalsIgnoreCase(code))) {
						//	String message = error.get(0).get("Message") == null ? null : error.get(0).get("Message").toString();
							
							Pattern pattern = Pattern.compile("PIB-CUST-[0-9]+");
							Matcher matcher = pattern.matcher(message);

							String custCode = "";
							if (matcher.find()) {
								custCode = matcher.group();
							}
							    if(StringUtils.isNotBlank(custCode)) {
							    	custRefNo = custCode;
							    }
							  //  if(StringUtils.isNotBlank(custRefNo)) {
							    /*	log.info("CUSTOMER UPDATE BLOCK START: "+new Date());
									Map<String,Object> customerUpdate = new HashMap<String,Object>();
									customerUpdate.put("Activities", "1");
									customerUpdate.put("Address1", null);
									customerUpdate.put("Address2", "");
									customerUpdate.put("AppointmentDate", "");
									customerUpdate.put("BranchCode", "121");
									customerUpdate.put("BrokerBranchCode", "1");
									customerUpdate.put("BusinessType", 1);
									customerUpdate.put("CityCode", null);
									customerUpdate.put("CityName", null);//check district or region
									customerUpdate.put("ClientName", customerName);
									customerUpdate.put("Clientstatus", "Y");
									customerUpdate.put("Country", "BWA");
									customerUpdate.put("CountryName", "Botswana");
									customerUpdate.put("CreatedBy", "guest_Botswana");//create login for whatsapp bot
									customerUpdate.put("CustomerAsInsurer", "N");
									customerUpdate.put("CustomerReferenceNo", custRefNo);
									customerUpdate.put("DobOrRegDate", cusDob); //cusDob
									customerUpdate.put("Email1", email);
									customerUpdate.put("Email2", null);
									customerUpdate.put("Email3", null);
									customerUpdate.put("ExpiryDate", null);
									customerUpdate.put("Fax", null);
									customerUpdate.put("Gender", "M");
									customerUpdate.put("IdNumber", idNumber);//idNumber
									customerUpdate.put("IdType", idTypeId);//idTypeId
									customerUpdate.put("InsuranceId", "100047");
									customerUpdate.put("IsTaxExempted", "N");
									customerUpdate.put("Language", "1");
									customerUpdate.put("LastName", " ");
									customerUpdate.put("MaritalStatus", null);//check
									customerUpdate.put("MiddleName", "");
									customerUpdate.put("MobileCode1", "267");
									customerUpdate.put("MobileCodeDesc1", "1");
									customerUpdate.put("MobileNo1", mobileNo);
									customerUpdate.put("MobileNo2", mobileNo);
									customerUpdate.put("MobileNo3", null);
									customerUpdate.put("Nationality", null);
									customerUpdate.put("Occupation", "");
									customerUpdate.put("OtherOccupation", "");
									customerUpdate.put("PhoneNoCode", null);
									customerUpdate.put("PinCode", null);
									customerUpdate.put("Placeofbirth", "chennai");
									customerUpdate.put("PolicyHolderType", "1");
									customerUpdate.put("PolicyHolderTypeid", "8");
									customerUpdate.put("PreferredNotification", null);
									customerUpdate.put("ProductId", "5");
									customerUpdate.put("RegionCode", null);
									customerUpdate.put("RiskAssessmentDate", null);
									customerUpdate.put("SaveOrSubmit", "Save");
									customerUpdate.put("SocioProfessionalCategory", null);
									customerUpdate.put("StateCode", null);
									customerUpdate.put("StateName", null);
									customerUpdate.put("Status", "Y");
									customerUpdate.put("Street", null);
									customerUpdate.put("TaxExemptedId", null);
									customerUpdate.put("TelephoneNo1", "");
									customerUpdate.put("TelephoneNo2", null);
									customerUpdate.put("TelephoneNo3", null);
									customerUpdate.put("Title", "1");
									customerUpdate.put("Type", null);
									customerUpdate.put("VipFlag", null);
									customerUpdate.put("VrTinNo", null);
									customerUpdate.put("WhatsappCode", "267");
									customerUpdate.put("WhatsappDesc", "1");
									customerUpdate.put("WhatsappNo", mobileNo);
									customerUpdate.put("Zone", "1");
									
									String custReq = mapper.writeValueAsString(customerUpdate);
									String cusUpdateApi = saveCustomerApi;
									log.info("Customer Update Calling: "+cusUpdateApi);
									log.info("Customer Update Request: "+custReq);
									
									String apiResponses = thread.callBotswanaComApi(cusSaveApi,cusReq);
									
									log.info("Customer Update Response: "+apiResponses);
									Map<String,Object> custupd = mapper.readValue(apiResponses, Map.class);
									
									Map<String,Object> custupdResult = custupd.get("Result") == null ? null :
										mapper.readValue(mapper.writeValueAsString(custupd.get("Result")), Map.class);
									
									if(custupdResult == null) {
										String errorMessgaes = custupd.get("ErrorMessage") == null ? "" : custupd.get("ErrorMessage").toString();
										response = errorMessgae;
										
										return response;		
							    }	*/
							    	
							    	log.info("GET CUSTOMER BLOCK START: "+new Date());
							    	Map<String,Object> getCustomerReq = new HashMap<>();
							    	getCustomerReq.put("CustomerReferenceNo", custRefNo);
							    	getCustomerReq.put("InsuranceId", "100047");
							    	
							    	String getCusReq = mapper.writeValueAsString(getCustomerReq);
									String CustomerApi = getCustomerApi;
									log.info("GET CUSTOMER Api Calling: "+getCustomerApi);
									log.info("GET CUSTOMER Request: "+getCusReq);
									
									 apiResponse = thread.callBotswanaComApi(CustomerApi,getCusReq);
									
									log.info("GET CUSTOMER Response: "+apiResponse);
									
									Map<String,Object> getcust = mapper.readValue(apiResponse, Map.class);
									Map<String,Object> getcustResult = getcust.get("Result") == null ? null : (Map<String, Object>) (getcust.get("Result"));
									
									//reqRefNo = saveMotResult.get("RequestReferenceNo") == null ? "" : saveMotResult.get("RequestReferenceNo").toString();
										
									if(getcustResult == null) {
										 errorMessgae = getcust.get("ErrorMessage") == null ? "" : getcust.get("ErrorMessage").toString();
										response = errorMessgae;
										return response;
									}else {
										String firstName = getcustResult.get("FirstName") == null ? "" : getcustResult.get("FirstName").toString();
										String lastName = getcustResult.get("LastName") == null ? "" : getcustResult.get("LastName").toString();
										customerName = firstName + lastName;
										email = getcustResult.get("Email1") == null ? "" : getcustResult.get("Email1").toString();
										cusDob = getcustResult.get("DobOrRegDate") == null ? "" : getcustResult.get("DobOrRegDate").toString();
										idTypeId = getcustResult.get("IdType") == null ? "" : getcustResult.get("IdType").toString();
										idType = getcustResult.get("IdTypeDesc") == null ? "" : getcustResult.get("IdTypeDesc").toString();
									}
						}else {
							response = errorMessgae;
							
							return response;
						}
						
					}else {
						custRefNo = custResult.get("SuccessId") == null ? "" : custResult.get("SuccessId").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
					return e.getMessage();
				}
				
				
				
				
				log.info("CUSTOMER CREATION BLOCK END: "+new Date());
				
				//==============================CUSTOMER CREATION BLOCK END=============================================
				
				//==============================MOTOR SAVE BLOCK START=============================================
				log.info("MOTOR SAVE BLOCK START: "+new Date());
				
				
				Map<String,Object> motorSave = new HashMap<String,Object>();
				motorSave.put("AboutVehicle", "");
				motorSave.put("AcExecutiveId", null);
				motorSave.put("AcccessoriesSumInsured", null);
				motorSave.put("AccessoriesInformation", null);
				motorSave.put("AdditionalCircumstances", "");
				motorSave.put("AgencyCode", "13576");//local-13495,UAT-14035
				motorSave.put("AggregatedValue", null);
				motorSave.put("ApplicationId", "1");
				motorSave.put("AxelDistance", 1);
				motorSave.put("BankingDelegation", "");
				motorSave.put("BdmCode", "99632332");//uAt-2000010,local-5555555
				motorSave.put("BorrowerType", null);
				motorSave.put("BranchCode", "121");
				motorSave.put("BrokerBranchCode", "1");
				motorSave.put("BrokerCode", "13576");//local-13495,UAT-14035
				motorSave.put("Chassisnumber", "99999");
				motorSave.put("CityLimit", null);
				motorSave.put("ClaimType", "");
				//motorSave.put("ClaimTypeDesc", null);
				motorSave.put("CollateralCompanyAddress", null);
				motorSave.put("CollateralCompanyName", null);
				motorSave.put("CollateralName", null);
				motorSave.put("CollateralYn", "N");
				motorSave.put("Color", null);
				motorSave.put("ColorDesc", null);
				motorSave.put("CommissionType", null);
				motorSave.put("CoverNoteNo", null);
				motorSave.put("CreatedBy", "guest_Botswana");//broker id
				motorSave.put("CubicCapacity", "1200");//doubt
				motorSave.put("Currency", "BWP");
				motorSave.put("CustomerCode", "99632332");//uAt-2000010,local-5555555
				motorSave.put("CustomerName", customerName);
				motorSave.put("CustomerReferenceNo", custRefNo);
				motorSave.put("DateOfCirculation", null);
				motorSave.put("Deductibles", null);
				motorSave.put("DefenceValue", "");
				//motorSave.put("DisplacementInCM3", null);
				motorSave.put("DrivenByDesc", "D");
				motorSave.put("DriverDetails", null);
				motorSave.put("EndorsementDate", null);
				motorSave.put("EndorsementEffectiveDate", null);
				motorSave.put("EndorsementRemarks", null);
				motorSave.put("EndorsementType", null);
				motorSave.put("EndorsementTypeDesc", null);
				motorSave.put("EndorsementYn", "N");
				motorSave.put("EndtCategoryDesc", null);
				motorSave.put("EndtCount", null);
				motorSave.put("EndtPrevPolicyNo", null);
				motorSave.put("EndtPrevQuoteNo", null);
				motorSave.put("EndtStatus", null);
				motorSave.put("EngineCapacity", "1200");
				motorSave.put("EngineNumber", null);
				motorSave.put("ExcessLimit", null);
				motorSave.put("ExchangeRate", "1.0");
				motorSave.put("FirstLossPayee", null);
				motorSave.put("FleetOwnerYn", "N");
				motorSave.put("FuelType", null);
				motorSave.put("FuelTypeDesc", "");
				motorSave.put("Gpstrackinginstalled", "N");
				//motorSave.put("Grossweight", grossWeight);
				motorSave.put("HavePromoCode", "N");
				motorSave.put("HoldInsurancePolicy", "N");
				motorSave.put("HorsePower", "0");
				motorSave.put("Idnumber", "");
				motorSave.put("Inflation", null);
				motorSave.put("InflationSumInsured", "");
				motorSave.put("InsuranceClass", insuranceClassId);
				motorSave.put("InsuranceClassDesc", insuranceClass);
				motorSave.put("InsuranceId", "100047");
				motorSave.put("Insurancetype", insuranceClassId);//103 check
				motorSave.put("InsurancetypeDesc", insuranceClass);
				motorSave.put("InsurerSettlement", "");
				motorSave.put("InterestedCompanyDetails", "");
				motorSave.put("IsFinanceEndt", null);
				motorSave.put("LoanAmount", 0);
				motorSave.put("LoanEndDate", null);
				motorSave.put("LoanStartDate", null);
				motorSave.put("LocationId", "1");
				motorSave.put("LoginId", "guest_Botswana");//login
				motorSave.put("ManufactureYear", "2023");
				motorSave.put("MarketValue", null);
				motorSave.put("Mileage", null);
				motorSave.put("MobileCode", "267");
				motorSave.put("MobileNumber", mobileNo);
				motorSave.put("ModelNumber", null);
				motorSave.put("MotorCategory", null);
				motorSave.put("Motorusage", motorUsage);
				motorSave.put("MotorusageId", motorUsageId);
				motorSave.put("MunicipalityTraffic", null);
				motorSave.put("NcdYn", "N");
				motorSave.put("Ncb", "0");
				motorSave.put("NewValue", null);
				motorSave.put("NoOfClaimYears", null);
				motorSave.put("NoOfClaims", null);
				motorSave.put("NoOfComprehensives", null);
				motorSave.put("NoOfFemale", "");
				motorSave.put("NoOfMale", "");
				motorSave.put("NoOfPassengers", "");
				motorSave.put("NoOfVehicles", "1");
				motorSave.put("NumberOfAxels", "1");
				motorSave.put("NumberOfCards", null);
				motorSave.put("NumberOfCylinders", null);
				motorSave.put("Occupation", "");
				motorSave.put("OrginalPolicyNo", null);
				motorSave.put("OwnerCategory", "1");
				motorSave.put("PaCoverId", "");
				//motorSave.put("PlateType", null);
				motorSave.put("PolicyEndDate", policyEndDate);
				motorSave.put("PolicyRenewalYn", "N");
				motorSave.put("PolicyStartDate", policyDate);
				motorSave.put("PolicyType", insuranceClassId);
				motorSave.put("PurchaseDate", null);
				motorSave.put("PreviousInsuranceYN", "N");
				motorSave.put("PreviousLossRatio", "");
				motorSave.put("ProductId", "5");
				motorSave.put("PromoCode", null);
				motorSave.put("QuoteExpiryDays", 90);
				motorSave.put("RadioOrCasseteplayer", null);
				motorSave.put("RegistrationDate", null);
				motorSave.put("RegistrationYear", cusDob);//check
				motorSave.put("Registrationnumber", regNo);
				motorSave.put("RequestReferenceNo", "");
				motorSave.put("RoofRack", null);
				//motorSave.put("SaveOrSubmit", "Save");
				motorSave.put("SavedFrom", "SQ");
				motorSave.put("SearchFromApi", false);
				motorSave.put("SeatingCapacity", "5");
				motorSave.put("SectionId", Arrays.asList(insuranceClassId));
				motorSave.put("SourceType", "b2c");
				motorSave.put("SourceTypeId", "b2c");
				motorSave.put("SpotFogLamp", null);
				motorSave.put("Status", "Y");
				motorSave.put("Stickerno", null);
				motorSave.put("SubUserType", "b2c");
				motorSave.put("SumInsured", sum_insured);
				motorSave.put("Tareweight", null);
				motorSave.put("TiraCoverNoteNo", null);
				motorSave.put("TppdFreeLimit", null);
				motorSave.put("TppdIncreaeLimit", null);
				motorSave.put("TrailerDetails", null);
				motorSave.put("TransportHydro", null);
				motorSave.put("UsageId", "");
				motorSave.put("UserType", "Broker");
				motorSave.put("Vehcilemodel", model);
				motorSave.put("VehcilemodelId", modelId);
				motorSave.put("VehicleId", 1);
				motorSave.put("VehicleType", bodyType);
				motorSave.put("VehicleTypeId", bodyTypeId);
				motorSave.put("VehicleTypeIvr", "");
				motorSave.put("VehicleValueType", null);
				motorSave.put("Vehiclemake", make);
				motorSave.put("VehiclemakeId", makeId);
				motorSave.put("WindScreenSumInsured", null);
				motorSave.put("Windscreencoverrequired", null);
				motorSave.put("Zone", "1");
				motorSave.put("ZoneCirculation", null);
				motorSave.put("accident", null);
				motorSave.put("periodOfInsurance", null);//inusredPeriod
				LinkedHashMap<String, Object> exchangeRateScenario = new  LinkedHashMap<>();
				exchangeRateScenario.put("OldAcccessoriesSumInsured", null);
				exchangeRateScenario.put("OldCurrency", null);
				exchangeRateScenario.put("OldExchangeRate", null);
				exchangeRateScenario.put("OldSumInsured", null);
				exchangeRateScenario.put("OldTppdIncreaeLimit", null);
				exchangeRateScenario.put("OldWindScreenSumInsured", null);
				
				LinkedHashMap<String, Object> excahnge = new  LinkedHashMap<>();
				excahnge.put("ExchangeRateScenario", exchangeRateScenario);
				motorSave.put("Scenarios", excahnge);
				
				List<Map<String,Object>> saveMotResult = null;
				Map<String,Object> saveMotorRes = new HashMap<String,Object>();
				String reqRefNo = "";
				Map<String,Object> motorSaveResult=null;
				try {
					String motorSaveReq = mapper.writeValueAsString(motorSave);
					String saveMotorApi = motorSaveApi;
					log.info("Save Motor Api Calling: "+saveMotorApi);
					log.info("Save Motor Request: "+motorSaveReq);
					
					String apiResponse = thread.callBotswanaComApi(saveMotorApi,motorSaveReq);
					
					log.info("Save Motor Response: "+apiResponse);
					
					Map<String,Object> saveMot = mapper.readValue(apiResponse, Map.class);
					saveMotResult = saveMot.get("Result") == null ? null : (List<Map<String, Object>>) saveMot.get("Result");
					
					//reqRefNo = saveMotResult.get("RequestReferenceNo") == null ? "" : saveMotResult.get("RequestReferenceNo").toString();
						
					if(saveMotResult == null) {
						String errorMessgae = saveMot.get("ErrorMessage") == null ? "" : saveMot.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}else {
						reqRefNo = saveMotResult.get(0).get("RequestReferenceNo") == null ? "" : saveMotResult.get(0).get("RequestReferenceNo").toString();
						
						motorSaveResult=saveMotResult.get(0);
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
				
				log.info("MOTOR SAVE BLOCK END: "+new Date());
				//==============================MOTOR SAVE BLOCK END=============================================
				
				//==============================CALC BLOCK START=============================================
				log.info("CALC BLOCK START: "+new Date());		
				
				Map<String,Object> calc = new HashMap<String,Object>();
				calc.put("AgencyCode", "13576");//local-13506,UAT-14035
				calc.put("BranchCode", "121");
				calc.put("CdRefNo", motorSaveResult.get("CdRefNo") == null ? "" : motorSaveResult.get("CdRefNo"));
				calc.put("CoverModification", "N");
				calc.put("CreatedBy", "guest_Botswana");
				calc.put("DdRefNo", motorSaveResult.get("DdRefNo") == null ? "" : motorSaveResult.get("DdRefNo"));
				calc.put("EffectiveDate", policyDate);
				calc.put("InsuranceId", "100047");
				calc.put("LocationId", "1");
				calc.put("MSRefNo", motorSaveResult.get("MSRefNo") == null ? "" : motorSaveResult.get("MSRefNo"));
				calc.put("PolicyEndDate", policyEndDate);
				calc.put("ProductId", "5");
				calc.put("RequestReferenceNo", reqRefNo);
				calc.put("SectionId", motorSaveResult.get("SectionId")==null?"":motorSaveResult.get("SectionId"));
				calc.put("VdRefNo", motorSaveResult.get("VdRefNo")==null?"":motorSaveResult.get("VdRefNo"));
				calc.put("VehicleId", "1");
				calc.put("productId", "5");
				
				List<Map<String,Object>> coverList =null;
				Map<String,Object> calcRes=null;
				Map<String,Object> calcResult = null;
				try {
					String calReq = mapper.writeValueAsString(calc);
					log.info("Calc Request: "+calReq);
					
					String calculatorApi = calcApi;
					String apiResponse = thread.callBotswanaComApi(calculatorApi, calReq);
					
					log.info("Calc Response: "+apiResponse);
					
				//	log.info("Save Motor Response: "+apiResponse);
					
					 calcRes = mapper.readValue(apiResponse, Map.class);
					 coverList = calcRes.get("CoverList") == null ? null :
						 mapper.readValue(mapper.writeValueAsString(calcRes.get("CoverList")), List.class);
					 
				//	calcResult = calcRes.get("Result") == null ? null : (Map<String, Object>) calcRes.get("Result");
					
					if(coverList == null) {
						String errorMessgae = calcRes.get("ErrorMessage") == null ? "" : calcRes.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				
				Long premium=0L;
				Long vatTax =0L;
				Double vatPercentage=0D;
				
				List<Map<String,Object>> basecover = coverList.stream().filter(p -> "5".equalsIgnoreCase(p.get("CoverId").toString())).collect(Collectors.toList());
				
				List<Map<String,Object>> tax = 
						(List<Map<String, Object>>) basecover.get(0).get("Taxes");
				
				
				/*BigDecimal pre = coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
						.map(m -> new BigDecimal(m.get("PremiumExcluedTaxLC").toString())).reduce(BigDecimal.ZERO, (x,y) -> x.add(y));
				
				List<Map<String,Object>> taxList = coverList.parallelStream().filter(p -> ("B".equalsIgnoreCase(p.get("CoverageType").toString()) 
						|| "D".equalsIgnoreCase(p.get("isSelected").toString())) && p.get("Taxes") != null)
						.map(m -> (List<Map<String,Object>>) m.get("Taxes")).flatMap(f -> f.stream()).collect(Collectors.toList());
				
				vatTax = taxList.stream().map(t -> t.get("TaxAmount") == null ? 0L : Double.valueOf(t.get("TaxAmount").toString()).longValue())
						.reduce(0L, (a,b) -> a + b);*/
				
				Map<String, Object> selectedCover =
					    coverList.stream()
					        .filter(p -> "B".equalsIgnoreCase(String.valueOf(p.get("CoverageType")))
					                  && "D".equalsIgnoreCase(String.valueOf(p.get("isSelected"))))
					        .max(Comparator.comparing(
					            p -> new BigDecimal(p.get("PremiumExcluedTaxLC").toString())))
					        .orElse(null);
				
				BigDecimal pre =
					    selectedCover == null
					        ? BigDecimal.ZERO
					        : new BigDecimal(selectedCover.get("PremiumExcluedTaxLC").toString());
				
				BigDecimal vatTaxs =
					    selectedCover == null || selectedCover.get("Taxes") == null
					        ? BigDecimal.ZERO
					        : ((List<Map<String, Object>>) selectedCover.get("Taxes"))
					            .stream()
					            .map(t -> new BigDecimal(
					                t.get("TaxAmount") == null ? "0" : t.get("TaxAmount").toString()))
					            .reduce(BigDecimal.ZERO, BigDecimal::add);
				
				vatPercentage = tax.get(0).get("TaxRate")==null?0L:Double.valueOf(tax.get(0).get("TaxRate").toString());
				
				premium = pre.longValue();
				
				BigDecimal totalPremium = pre.add(vatTaxs);
				System.out.println(totalPremium);
				
			//	Long totalPremium =pre.longValue()+vatTax.longValue();
						
				log.info("CALC BLOCK END: "+new Date());
				
				//==============================CALC BLOCK END=============================================================
				
//==============================USER CREATION BLOCK START=============================================
				
				log.info("USER CREATION BLOCK START: "+new Date());
				
				Map<String,Object> userCreationMap = new HashMap<String,Object>();
				userCreationMap.put("CompanyId", "100047");
				userCreationMap.put("CustomerId", custRefNo);
				userCreationMap.put("ProductId", "5");
				userCreationMap.put("ReferenceNo", reqRefNo);
				userCreationMap.put("UserMobileNo", mobileNo);
				userCreationMap.put("UserMobileCode", "267");
				userCreationMap.put("AgencyCode", "13576"); //local-13506,UAT-14035
				
				Map<String,Object> userResult = null;
				Map<String,Object> userRes = null;
				try {
					String userCreationReq = mapper.writeValueAsString(userCreationMap);
					String userCreationApi = loginCreationApi;
					
					log.info("USER CREATION API: "+userCreationApi);
					
					String apiResponse = thread.callBotswanaComApi(userCreationApi, userCreationReq);
					
					log.info("USER CREATION RESPONSE: "+apiResponse);
					
					 userRes = mapper.readValue(apiResponse, Map.class);
						
						userResult = userRes.get("Result") == null ? null : (Map<String, Object>) userRes.get("Result");
						
						//if(userResult == null) {
						//	String errorMessgae = calcRes.get("ErrorMessage") == null ? "" : calcRes.get("ErrorMessage").toString();
						//	response = errorMessgae;
						//	return response;
					//	}
						
				}catch(Exception e) {
					e.printStackTrace();
				}
				log.info("USER CREATION BLOCK END : "+new Date());
				
				//==============================USER CREATION BLOCK END=============================================
				
				//==============================BUY POLICY BLOCK START=============================================
				log.info("BUY POLICY BLOCK START : "+new Date());
				
				Map<String,Object> coversMap = new HashMap<>();
				coversMap.put("CoverId", "5");
				coversMap.put("SubCoverYn", "N");
				
				Function<Map<String,Object>,Map<String,Object>> function = fun -> {
					Map<String,Object> map = new HashMap<String,Object>();
					map.put("CoverId", fun.get("CoverId").toString());
					map.put("SubCoverYn", "N");		
					return map;
				};
				
				List<Map<String,Object>> buyCovers =coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
						.map(function).collect(Collectors.toList());
				
				Map<String,Object> vehicleMap =new HashMap<String,Object>();
				vehicleMap.put("SectionId", motorSaveResult.get("SectionId")==null?"":motorSaveResult.get("SectionId"));
				vehicleMap.put("Id", "1");
				vehicleMap.put("LocationId", "1");
				vehicleMap.put("Covers", buyCovers);
				List<Map<String,Object>> vehiMapList =new ArrayList<Map<String,Object>>();
				vehiMapList.add(vehicleMap);
				
				Map<String,Object> buypolicyMap =new HashMap<String,Object>();
				buypolicyMap.put("RequestReferenceNo", reqRefNo);
				buypolicyMap.put("CreatedBy", "guest_Botswana");
				buypolicyMap.put("ProductId", "5");
				buypolicyMap.put("ManualReferralYn", "N");
				buypolicyMap.put("EmiYn", "N");
				buypolicyMap.put("Vehicles", vehiMapList);
				
				Map<String,Object> buyPolicyResult = null;
				Map<String,Object> buyPolicyRes = null;
				try {
					String buypolicyReq =objectPrint.toJson(buypolicyMap);
					String buyPolicyApi = buyPolicy;
					
					log.info("BUY POLICY API: "+buyPolicyApi);
					log.info("BUY POLICY Request: "+buypolicyReq);
					
					String apiResponse = thread.callBotswanaComApi(buyPolicyApi, buypolicyReq);
					
					log.info("BUY POLICY RESPONSE: "+apiResponse);
					
					buyPolicyRes = mapper.readValue(apiResponse, Map.class);
					buyPolicyResult = buyPolicyRes.get("Result") == null ? null :
						(Map<String, Object>) buyPolicyRes.get("Result");
					
					if(buyPolicyResult == null) {
						String errorMessgae = buyPolicyRes.get("ErrorMessage") == null ? "" : buyPolicyRes.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}
					
				}catch(Exception e) {
					e.printStackTrace();
					exception = e.getMessage();
				}
				if(StringUtils.isNotBlank(exception)) {
					errorList.add(new Error(exception, "ErrorMsg", "101"));
				}
				
				if(errorList.size()>0) {
					throw new WhatsAppValidationException(errorList);

				}
				
				log.info("BUYPOLICY  BLOCK END : "+new Date());
				
				//==============================BUY POLICY BLOCK END=============================================
				
				//==============================MAKE PAYMENT BLOCK START=============================================
			/*	
				log.info("MAKE PAYMENT BLOCK START : "+new Date());
				Map<String,Object> makePaymentMap = new HashMap<String,Object>();
				makePaymentMap.put("CreatedBy", "guest_Botswana");
				makePaymentMap.put("EmiYn", "N");
				makePaymentMap.put("InstallmentMonth", null);
				makePaymentMap.put("InstallmentPeriod", null);
				makePaymentMap.put("InsuranceId", "100047");
				makePaymentMap.put("Premium", totalPremium);
				makePaymentMap.put("QuoteNo", buyPolicyResult.get("QuoteNo"));
				makePaymentMap.put("Remarks", "None");
				makePaymentMap.put("SubUserType", "Broker");
				makePaymentMap.put("UserType", "Broker");
				
				Map<String,Object> makePaymentResult =null;
				Map<String,Object> makePaymentRes =null;
				try {
					String makePayemantReq = objectPrint.toJson(makePaymentMap);
					
					String makePaymentApi = makePayment;
					log.info("MAKE PAYMENT API: "+makePaymentApi);
					
					log.info("MAKE PAYMENT REQUEST: "+makePayemantReq);
					String apiResponse = thread.callBotswanaComApi(makePaymentApi, makePayemantReq);
					log.info("MAKE PAYMENT RESPONSE: "+apiResponse);
					
					makePaymentRes = mapper.readValue(apiResponse, Map.class);
					makePaymentResult = makePaymentRes.get("Result") == null ? null :
						(Map<String, Object>) makePaymentRes.get("Result");
					
					if(makePaymentResult == null) {
						String errorMessgae = buyPolicyRes.get("ErrorMessage") == null ? "" : buyPolicyRes.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}
				}catch(Exception e) {
					e.printStackTrace();
					exception = e.getMessage();
				}
				
				if(StringUtils.isNotBlank(exception)) {
					errorList.add(new Error(exception, "ErrorMsg", "101"));
				}
				
				if(errorList.size()>0) {
					throw new WhatsAppValidationException(errorList);

				}
				
				log.info("MAKE PAYMENT BLOCK END : "+new Date());
				
				//==============================MAKE PAYMENT BLOCK END=============================================
				
				//==============================INSERT PAYMENT BLOCK START=============================================
				log.info("INSERT PAYMENT BLOCK START : "+new Date());
				
				Map<String,Object> insertPaymentMap = new HashMap<>();
				insertPaymentMap.put("AccountNumber", null);
				insertPaymentMap.put("BankName", null);
				insertPaymentMap.put("ChequeDate", "");
				insertPaymentMap.put("ChequeNo", null);
				insertPaymentMap.put("CreatedBy", "SZL_Whatsapp");
				insertPaymentMap.put("EmiYn", "N");
				insertPaymentMap.put("IbanNumber", null);
				insertPaymentMap.put("InsuranceId", "100049");
				insertPaymentMap.put("MICRNo", null);
				insertPaymentMap.put("MobileCode1", null);
				insertPaymentMap.put("MobileNo1", null);
				insertPaymentMap.put("PayeeName", customerName);
				insertPaymentMap.put("PaymentId", makePaymentResult.get("PaymentId"));
				insertPaymentMap.put("PaymentType", "4");
				insertPaymentMap.put("Payments", "");
				insertPaymentMap.put("Premium", totalPremium);
				insertPaymentMap.put("QuoteNo", buyPolicyResult.get("QuoteNo"));
				insertPaymentMap.put("Remarks", "None");
				insertPaymentMap.put("SubUserType", "Broker");
				insertPaymentMap.put("UserType", "Broker");
				insertPaymentMap.put("WhatsappCode", null);
				insertPaymentMap.put("WhatsappNo", null);
				
				Map<String,Object> insertPaymentRes = null;
				Map<String,Object> instPatmentResult = null;
				try {
					String insertPaymentReq = objectPrint.toJson(insertPaymentMap);
					String insertPaymentApi = insertPayment;
					log.info("INSERT PAYMENT API: "+insertPaymentApi);
					log.info("INSERT PAYMENT REQUEST: "+insertPaymentReq);
					String apiResponse = thread.callBotswanaComApi(insertPaymentApi, insertPaymentReq);
					log.info("INSERT PAYMENT RESPONSE: "+apiResponse);
					insertPaymentRes = mapper.readValue(apiResponse, Map.class);
					instPatmentResult = insertPaymentRes.get("Result") == null ? null :
						(Map<String, Object>) insertPaymentRes.get("Result");
					
					if(instPatmentResult == null) {
						String errorMessgae = buyPolicyRes.get("ErrorMessage") == null ? "" : buyPolicyRes.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}
				}catch(Exception e) {
					e.printStackTrace();
					exception = e.getMessage();
				}
				
				if(StringUtils.isNotBlank(exception)) {
					errorList.add(new Error(exception, "ErrorMsg", "101"));
				}
				
				if(errorList.size()>0) {
					throw new WhatsAppValidationException(errorList);

				}
				
				String marchantRefNo = instPatmentResult.get("MerchantReference") == null ? "" :
					instPatmentResult.get("MerchantReference").toString();
				
				String quoteNo = instPatmentResult.get("QuoteNo") == null ? "" :
					instPatmentResult.get("QuoteNo").toString();
				
				log.info("RequestRefNo : "+reqRefNo+" ||  MerchantReference : "+marchantRefNo+" || QuoteNo : "+quoteNo+" ");
				
				log.info("INSERT PAYMENT BLOCK END : "+new Date());
				
				//==============================INSERT PAYMENT BLOCK END=============================================
				
				//==============================PAYMENT LINK BLOCK START=============================================
				log.info("PAYMENT LINK BLOCK START : "+new Date());
				
			    Map<String,Object> paymentMap = new HashMap<>();
			    paymentMap.put("MerchantRefNo", marchantRefNo);
			    paymentMap.put("CompanyId", "100049");
			    paymentMap.put("WhatsappCode", "268");
			    paymentMap.put("WhtsappNo", mobileNo);
			    paymentMap.put("QuoteNo", quoteNo);
			    
			    String payJson =objectPrint.toJson(paymentMap);
			    String encodeReq =Base64.getEncoder().encodeToString(payJson.getBytes());
			    
			    String paymentUrl = phoenixMotorPaymentlink+encodeReq;
			    
			    log.info("PAYMENT LINK :" +paymentUrl);
			    
			    log.info("PAYMENT LINK BLOCK END : "+new Date());
			    */
			  //==============================PAYMENT LINK BLOCK END=============================================
			    
			  //==============================WHATSAPP RESPONSE BLOCK START=============================================
			    log.info("WHATSAPP RESPONSE BLOCK START : "+new Date());
			    
			    Map<String,Object> getMotorReq = new HashMap<String, Object>();
				getMotorReq.put("RequestReferenceNo", reqRefNo);
				
				String api_request =mapper.writeValueAsString(getMotorReq);
				String motorDetailsApi = getAllMotorDetailsApi;
				String apiResponse = thread.callBotswanaComApi(motorDetailsApi, api_request);
				
				Map<String,Object> getMotorRes =mapper.readValue(apiResponse, Map.class);
				
				List<Map<String,Object>> motorRes =getMotorRes.get("Result")==null?null:
					mapper.readValue(mapper.writeValueAsString(getMotorRes.get("Result")), List.class);
				
				log.info("ALL MOTOR DETAILS :" +motorRes);
				
				Map<String,Object> mot = motorRes.get(0);
				
				botResponceData.put("RegNo", mot.get("Registrationnumber")==null?"N/A":mot.get("Registrationnumber"));
				botResponceData.put("VehicleUsage", mot.get("MotorUsageDesc")==null?"N/A":mot.get("MotorUsageDesc"));
				botResponceData.put("BodyType", mot.get("VehicleTypeDesc")==null?"N/A":mot.get("VehicleTypeDesc"));
			//	botResponceData.put("color",mot.get("ColorDesc")==null?"N/A":mot.get("ColorDesc"));
				botResponceData.put("InsuranceClass",insuranceClass);
				botResponceData.put("Premium", pre);
			//	botResponceData.put("url", paymentUrl);
				botResponceData.put("VatAmt", vatTaxs);
				botResponceData.put("SumInsured", mot.get("SumInsured")==null?"N/A":mot.get("SumInsured"));
			//	botResponceData.put("chassis", mot.get("Chassisnumber")==null?"N/A":mot.get("Chassisnumber"));
				botResponceData.put("VatPercentage", String.valueOf(vatPercentage.longValue()));
				botResponceData.put("TotalPremium", totalPremium);
				botResponceData.put("InceptionDate", policyDate);
				botResponceData.put("ExpiryDate",policyEndDate);
				botResponceData.put("ReferenceNo", reqRefNo);
				botResponceData.put("Model", mot.get("VehcilemodelDesc")==null?"N/A":mot.get("VehcilemodelDesc"));
				botResponceData.put("Make", mot.get("VehiclemakeDesc")==null?"N/A":mot.get("VehiclemakeDesc"));
				botResponceData.put("CustomerName", customerName);
				botResponceData.put("QuoteNo", buyPolicyResult.get("QuoteNo"));
				
				
				
				return botResponceData;
				
	}

}
