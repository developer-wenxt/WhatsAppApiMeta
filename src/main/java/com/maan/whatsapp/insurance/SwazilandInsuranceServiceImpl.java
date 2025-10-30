package com.maan.whatsapp.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.ConcurrentDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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
import com.maan.whatsapp.entity.whatsapp.PhoenixUserDataDetails;
import com.maan.whatsapp.repository.whatsapp.PhoenixUserDataDetailsRepo;
import com.maan.whatsapp.response.error.Error;

@Service
@PropertySource("classpath:WebServiceUrl.properties")
public class SwazilandInsuranceServiceImpl implements SwazilandInsuranceService {
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private Gson objectPrint;
	
	@Autowired
	@Lazy
	private PhoenixAsyncProcessThread thread;
	
	Logger log = LogManager.getLogger(getClass());
	
	@Value("${wh.phoenix.savevehicleinfo.api}")
	private String saveVehicleInfoApi;
	
	@Value("${wh.phoenix.customersave.api}")
	private String saveCustomerApi;
	
	@Value("${wh.phoenix.showvehicleinfo.api}")
	private String showVehicleInfoApi;
	
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

	@Value("${wh.phoenix.getmasterids.api}")
	private String masterIdsApi;
	
	@Value("${askeva.template.api}")
	private String askeveApi;
	
	@Autowired
	private PhoenixUserDataDetailsRepo userDataRepo;

	@Override
	public Object swazilandQuote(Object req)
			throws JsonProcessingException, JsonMappingException, WhatsAppValidationException {
		String response = "";
		String exception = "";
		List<Error> errorList = new ArrayList<>(2);
		Map<String,Object> botResponceData = new HashMap<String,Object>();
		
	//	String flowDatas = new String(Base64.getDecoder().decode(req.toString()));
		
	//	Map<String,Object> requestDatas = mapper.readValue(flowDatas, Map.class);
		
		Map<String,Object> data = mapper.convertValue(req, Map.class);
		log.info("Flow Req :"+mapper.writeValueAsString(data));
		
		//Customer page Datas
				String title = data.get("Title") == null ? "" : data.get("Title").toString();
				String customerName = data.get("Customer Name") == null ? "" : data.get("Customer Name").toString();
				String gender = data.get("Gender") == null ? "" : data.get("Gender").toString();
				String occupation = data.get("Occupation") == null ? "" : data.get("Occupation").toString();
				String mobileNo = data.get("Mobile Number") == null ? "" : data.get("Mobile Number").toString();
				String email = data.get("Email") == null ? "" : data.get("Email").toString();
				String idType = data.get("Id Type") == null ? "" : data.get("Id Type").toString();
				String idNumber = data.get("Id Number") == null ? "" : data.get("Id Number").toString();
				String region = data.get("Region") == null ? "" : data.get("Region").toString();
			//	String distict = requestDatas.get("district") == null ? "" : requestDatas.get("district").toString();
				String address = data.get("Address") == null ? "" : data.get("Address").toString();
				
				//Vehicle Page Datas
				String motorUsage = data.get("Motor Usage") == null ? "" : data.get("Motor Usage").toString();
				String bodyType = data.get("Body Type") == null ? "" : data.get("Body Type").toString();
				String make = data.get("Make") == null ? "" : data.get("Make").toString();
				String model = data.get("Model") == null ? "" : data.get("Model").toString();
				String regNo = data.get("Registration No") == null ? "" : data.get("Registration No").toString();
				String chassisNo = data.get("Chassis No") == null ? "" : data.get("Chassis No").toString();
				String engineNo = data.get("Engine No") == null ? "" : data.get("Engine No").toString();
				String enginecapacity = data.get("Engine Capacity") == null ? "" : data.get("Engine Capacity").toString();
				String seatingCapacity = data.get("Seating Capacity") == null ? "" : data.get("Seating Capacity").toString();
				String manYear = data.get("Manufacture Year") == null ? "" : data.get("Manufacture Year").toString();
				String color = data.get("Color") == null ? "" : data.get("Color").toString();
				String grossWeight = data.get("Tonnage") == null ? "" : data.get("Tonnage").toString();
				
				//Policy Page datas 
				String insuranceClass = data.get("Insurance Class") == null ? "" : data.get("Insurance Class").toString();
				//String inusredPeriod = data.get("Insured Period") == null ? "" : data.get("Insured Period").toString();
				String policyDate = data.get("Policy Start Date") == null ? "" : data.get("Policy Start Date").toString();
				String  sum_insured= data.get("Sum Insured") == null ? "" : data.get("Sum Insured").toString();
				String  extended_tppd_si= data.get("Extended TPPD SumInsured") == null ? "" : data.get("Extended TPPD SumInsured").toString();
				
				//DOB calc
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
				DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				LocalDate policyStartDate = LocalDate.parse(policyDate, inputFormatter);
				policyDate = policyStartDate.format(formatter);
				LocalDate curDate = LocalDate.parse(policyDate,formatter);
				LocalDate minusDate = curDate.minusYears(18);
				String cusDob = minusDate.format(formatter);
				
			/*	if("COMP".equalsIgnoreCase(insuranceClass)) {
					insuranceClass = "1";
				}else if("TPFT".equalsIgnoreCase(insuranceClass)) {
					insuranceClass = "2";
				}else if("TPO".equalsIgnoreCase(insuranceClass)) {
					insuranceClass = "3";
				}*/
				
				//Ids collect from desc 
				
				String titlteId="",genderId="",occupationId="",idTypeId="",regionId="",
						distictId="",colorId = "",motorUsageId="",bodyTypeId="",makeId="",modelId="",
						insuranceClassId="";
				
				//masterIds 
				Map<String,Object> titleMap = new HashMap<>();
				titleMap.put("Desc", title);
				titleMap.put("MasterType", "CUSTOMER_TITLE");
				titleMap.put("InsuranceId", "100049");
				
				try {
					String titileReq = mapper.writeValueAsString(titleMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi, titileReq);
					
					log.info("Master Api Response: "+apiResponse);
					Map<String,Object> titleList = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = titleList.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(titleList.get("Result")), Map.class);
					
					if(masterApiResult.isEmpty() || masterApiResult == null) {
						String errorMessage = titleList.get("ErrorMessage") == null ? "" : titleList.get("ErrorMessage").toString();
						response = errorMessage;
						return response;
					}else {
						titlteId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				Map<String,Object> genderMap = new HashMap<>();
				genderMap.put("Desc", gender);
				genderMap.put("MasterType", "CUSTOMER_GENDER");
				genderMap.put("InsuranceId", "100049");
				
				try {
					
					String genderReq = mapper.writeValueAsString(genderMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi, genderReq);
					log.info("Master Api Response: "+apiResponse);
					Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
					
					if(masterApiResult.isEmpty() || masterApiResult == null) {
						String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
						response = errorMessage;
						return response;
					}else {
						genderId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
					}
					
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				Map<String,Object> occupationMap = new HashMap<>();
				occupationMap.put("Desc", occupation);
				occupationMap.put("MasterType", "CUSTOMER_OCCUPATION");
				occupationMap.put("InsuranceId", "100049");
				
				try {
					String occupationReq = mapper.writeValueAsString(occupationMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi, occupationReq);
					log.info("Master Api Response: "+apiResponse);
					Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
					
					if(masterApiResult.isEmpty() || masterApiResult == null) {
						String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
						response = errorMessage;
						return response;
					}else {
						occupationId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				Map<String,Object> idTypeMap = new HashMap<>();
				idTypeMap.put("Desc", idType);
				idTypeMap.put("MasterType", "CUSTOMER_IDTYPE");
				idTypeMap.put("InsuranceId", "100049");
				
				try {
					String idTypeReq = mapper.writeValueAsString(idTypeMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi, idTypeReq);
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
				
				Map<String,Object> regionMap = new HashMap<>();
				regionMap.put("Desc", region);
				regionMap.put("MasterType", "CUSTOMER_REGION");
				regionMap.put("CountryCode", "SZL");
				
				try {
					String regionReq = mapper.writeValueAsString(regionMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi, regionReq);
					log.info("Master Api Response: "+apiResponse);

					Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
					
					if(masterApiResult.isEmpty() || masterApiResult == null) {
						String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
						response = errorMessage;
						return response;
					}else {
						regionId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				Map<String,Object> districtMap = new HashMap<>();
				districtMap.put("Desc", region);
				districtMap.put("MasterType", "CUSTOMER_DISTRICT");
				districtMap.put("CountryCode", "SZL");
				
				try {
					String districtReq = mapper.writeValueAsString(districtMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi, districtReq);
					log.info("Master Api Response: "+apiResponse);

					Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
					
					if(masterApiResult.isEmpty() || masterApiResult == null) {
						String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
						response = errorMessage;
						return response;
					}else {
						distictId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				Map<String,Object> insuranceClassMap = new HashMap<>();
				insuranceClassMap.put("Desc", insuranceClass);
				insuranceClassMap.put("MasterType", "INSURANCE_CLASS");
				insuranceClassMap.put("InsuranceId", "100049");
				
				try {
					String insuranceClassReq = mapper.writeValueAsString(insuranceClassMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi, insuranceClassReq);
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
				motorusageMap.put("InsuranceId", "100049");
				
				try {
					String motorUsageReq = mapper.writeValueAsString(motorusageMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi,motorUsageReq);
					
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
				bodyTypeMap.put("InsuranceId", "100049");
				
				try {
					String bodyTypeReq = mapper.writeValueAsString(bodyTypeMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi,bodyTypeReq);
					
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
				makeMap.put("InsuranceId", "100049");
				
				try {
					String makeReq = mapper.writeValueAsString(makeMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi,makeReq);
					
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
				modelMap.put("InsuranceId", "100049");
				
				try {
					String modelReq = mapper.writeValueAsString(modelMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi,modelReq);
					
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
				
				Map<String,Object> colorMap = new HashMap<>();
				colorMap.put("Desc", color);
				colorMap.put("MasterType", "VEHICLE_COLOR");
				colorMap.put("InsuranceId", "100049");
				
				try {
					String colorReq = mapper.writeValueAsString(colorMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callSwazilandComApi(masterApi,colorReq);
					
					log.info("Master Api Response: "+apiResponse);
					Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = cust.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
					
				   
					if(masterApiResult == null) {
						String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}else {
						colorId = masterApiResult.get("Response")==null?"":masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				//==============================SAVE VEHICLE INFO BLOCK START=============================================
				log.info("SAVE VEHICLE INFO START: "+new Date());
				
				Map<String,Object> vehicleInfo = new HashMap<String,Object>();
				vehicleInfo.put("Insuranceid", "100049");
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
					String vehResponse = thread.callSwazilandComApi(saveVehicleInfoApi,vehReq);
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
				
						
				//==============================SAVE VEHICLE INFO BLOCK END=============================================
				
				//==============================CUSTOMER CREATION BLOCK START=============================================
				log.info("CUSTOMER CREATION BLOCK START: "+new Date());
				Map<String,Object> customerCreation = new HashMap<String,Object>();
				customerCreation.put("Activities", "");
				customerCreation.put("Address1", address);
				customerCreation.put("Address2", "");
				customerCreation.put("AppointmentDate", "");
				customerCreation.put("BranchCode", "119");
				customerCreation.put("BrokerBranchCode", "1");
				customerCreation.put("BusinessType", null);
				customerCreation.put("CityCode", distictId);
				customerCreation.put("CityName", region);//check district or region
				customerCreation.put("ClientName", customerName);
				customerCreation.put("Clientstatus", "Y");
				customerCreation.put("Country", "SZL");
				customerCreation.put("CountryName", "Swaziland");
				customerCreation.put("CreatedBy", "SZL_Whatsapp");//create login for whatsapp bot
				customerCreation.put("CustomerAsInsurer", "N");
				customerCreation.put("CustomerReferenceNo", "");
				customerCreation.put("DobOrRegDate", cusDob);
				customerCreation.put("Email1", email);
				customerCreation.put("Email2", null);
				customerCreation.put("Email3", null);
				customerCreation.put("ExpiryDate", null);
				customerCreation.put("Fax", null);
				customerCreation.put("Gender", genderId);
				customerCreation.put("IdNumber", idNumber);
				customerCreation.put("IdType", idTypeId);
				customerCreation.put("InsuranceId", "100049");
				customerCreation.put("IsTaxExempted", "N");
				customerCreation.put("Language", "1");
				customerCreation.put("LastName", "");
				customerCreation.put("MaritalStatus", "Single");//check
				customerCreation.put("MiddleName", "");
				customerCreation.put("MobileCode1", "268");
				customerCreation.put("MobileCodeDesc1", "1");
				customerCreation.put("MobileNo1", mobileNo);
				customerCreation.put("MobileNo2", "");
				customerCreation.put("MobileNo3", null);
				customerCreation.put("Nationality", "");
				customerCreation.put("Occupation", occupationId);
				customerCreation.put("OtherOccupation", "");
				customerCreation.put("PhoneNoCode", "");
				customerCreation.put("PinCode", "");
				customerCreation.put("Placeofbirth", address);
				customerCreation.put("PolicyHolderType", "1");
				customerCreation.put("PolicyHolderTypeid", idTypeId);
				customerCreation.put("PreferredNotification", "sms");
				customerCreation.put("ProductId", "5");
				customerCreation.put("RegionCode", regionId);
				customerCreation.put("RiskAssessmentDate", null);
				customerCreation.put("SaveOrSubmit", "Save");
				customerCreation.put("SocioProfessionalCategory", null);
				customerCreation.put("StateCode", regionId);
				customerCreation.put("StateName", null);
				customerCreation.put("Status", "Y");
				customerCreation.put("Street", address);
				customerCreation.put("TaxExemptedId", null);
				customerCreation.put("TelephoneNo1", null);
				customerCreation.put("TelephoneNo2", null);
				customerCreation.put("TelephoneNo3", null);
				customerCreation.put("Title", titlteId);
				customerCreation.put("Type", null);
				customerCreation.put("VipFlag", null);
				customerCreation.put("VrTinNo", null);
				customerCreation.put("WhatsappCode", "268");
				customerCreation.put("WhatsappDesc", "1");
				customerCreation.put("WhatsappNo", mobileNo);
				customerCreation.put("Zone", "1");
				
				String custRefNo = "";
				
				try {
					String cusReq = mapper.writeValueAsString(customerCreation);
					String cusSaveApi = saveCustomerApi;
					log.info("Customer Save Calling: "+cusSaveApi);
					log.info("Customer Save Request: "+cusReq);
					
					String apiResponse = thread.callSwazilandComApi(cusSaveApi,cusReq);
					
					log.info("Customer Save Response: "+apiResponse);
					Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> custResult = cust.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
					
					if(custResult == null) {
						String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}else {
						custRefNo = custResult.get("SuccessId") == null ? "" : custResult.get("SuccessId").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				log.info("CUSTOMER CREATION BLOCK END: "+new Date());
				
				//==============================CUSTOMER CREATION BLOCK END=============================================
				
				//==============================SHOW VEHICLE INFO BLOCK START=============================================
				log.info("SHOW VEHICLE INFO BLOCK START: "+new Date());
				Map<String,Object> vehInfo = new HashMap<String,Object>();
				vehInfo.put("BranchCode", "119");
				vehInfo.put("BrokerBranchCode", "1");
				vehInfo.put("CreatedBy", "SZL_Whatsapp");
				vehInfo.put("InsuranceId", "100049");
				vehInfo.put("ProductId", "5");
				vehInfo.put("ReqChassisNumber", "");
				vehInfo.put("ReqRegNumber", regNo);
				vehInfo.put("SavedFrom", "API");
				
				Map<String,Object> showVehResult = null;
				try {
					String showInfo = mapper.writeValueAsString(vehInfo);
					String showVehApi = showVehicleInfoApi;
					
					log.info("Show Vehicle Api Calling: "+showVehApi);
					log.info("Show Vehicle Request: "+showInfo);
					
					String apiResponse = thread.callSwazilandComApi(showVehApi, showInfo);
					
					log.info("Show Vehicle Response: "+apiResponse);
					
					Map<String,Object> showVeh = mapper.readValue(apiResponse, Map.class);
					showVehResult = showVeh.get("Result") == null ? null : (Map<String, Object>) showVeh.get("Result");
						
					if(showVehResult == null) {
						String errorMessgae = showVeh.get("ErrorMessage") == null ? "" : showVeh.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}
							
				}catch(Exception e) {
					e.printStackTrace();
				}
				log.info("SHOW VEHICLE INFO BLOCK END: "+new Date());
				//==============================SHOW VEHICLE INFO BLOCK END=============================================
				
				//==============================MOTOR SAVE BLOCK START=============================================
				log.info("MOTOR SAVE BLOCK START: "+new Date());
				LocalDate endDate = curDate.plusDays(364);
				String policyEndDate = endDate.format(formatter);
				
				Map<String,Object> motorSave = new HashMap<String,Object>();
				motorSave.put("AboutVehicle", null);
				motorSave.put("AcExecutiveId", null);
				motorSave.put("AcccessoriesSumInsured", null);
				motorSave.put("AccessoriesInformation", null);
				motorSave.put("AdditionalCircumstances", "");
				motorSave.put("AgencyCode", "14035");//local-13495,UAT-14035
				motorSave.put("AggregatedValue", null);
				motorSave.put("ApplicationId", "1");
				motorSave.put("AxelDistance", 1);
				motorSave.put("BankingDelegation", "");
				motorSave.put("BdmCode", "2000010");//uAt-2000010,local-5555555
				motorSave.put("BorrowerType", null);
				motorSave.put("BranchCode", "119");
				motorSave.put("BrokerBranchCode", "1");
				motorSave.put("BrokerCode", "14035");//local-13495,UAT-14035
				motorSave.put("Chassisnumber", chassisNo);
				motorSave.put("CityLimit", null);
				motorSave.put("ClaimType", "0");
				//motorSave.put("ClaimTypeDesc", null);
				motorSave.put("CollateralCompanyAddress", "");
				motorSave.put("CollateralCompanyName", "");
				motorSave.put("CollateralName", null);
				motorSave.put("CollateralYn", "N");
				motorSave.put("Color", colorId);
				motorSave.put("ColorDesc", color);
				motorSave.put("CommissionType", null);
				motorSave.put("CoverNoteNo", null);
				motorSave.put("CreatedBy", "SZL_Whatsapp");//broker id
				motorSave.put("CubicCapacity", enginecapacity);//doubt
				motorSave.put("Currency", "ZAR");
				motorSave.put("CustomerCode", "2000010");//uAt-2000010,local-5555555
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
				motorSave.put("EngineCapacity", enginecapacity);
				motorSave.put("EngineNumber", engineNo);
				motorSave.put("ExcessLimit", null);
				motorSave.put("ExchangeRate", "1.0");
				motorSave.put("FirstLossPayee", null);
				motorSave.put("FleetOwnerYn", "N");
				motorSave.put("FuelType", null);
				motorSave.put("FuelTypeDesc", "");
				motorSave.put("Gpstrackinginstalled", "N");
				motorSave.put("Grossweight", grossWeight);
				motorSave.put("HavePromoCode", "N");
				motorSave.put("HoldInsurancePolicy", "N");
				motorSave.put("HorsePower", "0");
				motorSave.put("Idnumber", idNumber);
				motorSave.put("Inflation", "");
				motorSave.put("InflationSumInsured", "");
				motorSave.put("InsuranceClass", "0");
				motorSave.put("InsuranceClassDesc", null);
				motorSave.put("InsuranceId", "100049");
				motorSave.put("Insurancetype", insuranceClassId);//103 check
				motorSave.put("InsurancetypeDesc", insuranceClass);
				motorSave.put("InsurerSettlement", "");
				motorSave.put("InterestedCompanyDetails", "");
				motorSave.put("IsFinanceEndt", null);
				motorSave.put("LoanAmount", 0);
				motorSave.put("LoanEndDate", null);
				motorSave.put("LoanStartDate", null);
			//	motorSave.put("LocationId", "1");
				motorSave.put("LoginId", "SZL_Whatsapp");//login
				motorSave.put("ManufactureYear", manYear);
				motorSave.put("MarketValue", null);
				motorSave.put("Mileage", null);
				motorSave.put("MobileCode", "268");
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
			//	motorSave.put("NoOfComprehensives", null);
				motorSave.put("NoOfFemale", null);
				motorSave.put("NoOfMale", null);
				motorSave.put("NoOfPassengers", null);
				motorSave.put("NoOfVehicles", "1");
				motorSave.put("NumberOfAxels", null);
				motorSave.put("NumberOfCards", null);
				motorSave.put("NumberOfCylinders", null);
				motorSave.put("Occupation", occupationId);
				motorSave.put("OrginalPolicyNo", null);
				motorSave.put("OwnerCategory", "");
				motorSave.put("PaCoverId", "0");
				//motorSave.put("PlateType", null);
				motorSave.put("PolicyEndDate", policyEndDate);
				motorSave.put("PolicyRenewalYn", "N");
				motorSave.put("PolicyStartDate", policyDate);
				motorSave.put("PolicyType", "1");
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
				motorSave.put("SavedFrom", "WEB");
				motorSave.put("SearchFromApi", false);
				motorSave.put("SeatingCapacity", seatingCapacity);
				motorSave.put("SectionId", Arrays.asList(insuranceClassId));
			//	motorSave.put("SourceType", "Broker");
				motorSave.put("SourceTypeId", "Broker");
				motorSave.put("SpotFogLamp", null);
				motorSave.put("Status", "Y");
				motorSave.put("Stickerno", null);
				motorSave.put("SubUserType", "Broker");
				motorSave.put("SumInsured", sum_insured);
				motorSave.put("Tareweight", null);
				motorSave.put("TiraCoverNoteNo", null);
				motorSave.put("TppdFreeLimit", null);
				motorSave.put("TppdIncreaeLimit", extended_tppd_si);
				motorSave.put("TrailerDetails", null);
				motorSave.put("TransportHydro", null);
				motorSave.put("UsageId", "");
				motorSave.put("UserType", "Broker");
				motorSave.put("Vehcilemodel", model);
				motorSave.put("VehcilemodelId", modelId);
				motorSave.put("VehicleId", 1);
				motorSave.put("VehicleType", bodyType);
				motorSave.put("VehicleTypeId", bodyTypeId);
				motorSave.put("VehicleTypeIvr", "90");
				motorSave.put("VehicleValueType", "");
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
				exchangeRateScenario.put("OldCurrency", "ZAR");
				exchangeRateScenario.put("OldExchangeRate", "1.0");
				exchangeRateScenario.put("OldSumInsured", 0);
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
					
					String apiResponse = thread.callSwazilandComApi(saveMotorApi,motorSaveReq);
					
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
				calc.put("AgencyCode", "14035");//local-13506,UAT-14035
				calc.put("BranchCode", "119");
				calc.put("CdRefNo", motorSaveResult.get("CdRefNo") == null ? "" : motorSaveResult.get("CdRefNo"));
				calc.put("CoverModification", "N");
				calc.put("CreatedBy", "SZL_Whatsapp");
				calc.put("DdRefNo", motorSaveResult.get("DdRefNo") == null ? "" : motorSaveResult.get("DdRefNo"));
				calc.put("EffectiveDate", policyDate);
				calc.put("InsuranceId", "100049");
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
					String apiResponse = thread.callSwazilandComApi(calculatorApi, calReq);
					
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
				
				List<Map<String,Object>> tax = (List<Map<String, Object>>) coverList.get(0).get("Taxes");
				
				BigDecimal pre = coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
						.map(m -> new BigDecimal(m.get("PremiumExcluedTaxLC").toString())).reduce(BigDecimal.ZERO, (x,y) -> x.add(y));
				
				List<Map<String,Object>> taxList = coverList.parallelStream().filter(p -> ("B".equalsIgnoreCase(p.get("CoverageType").toString()) 
						|| "D".equalsIgnoreCase(p.get("isSelected").toString())) && p.get("Taxes") != null)
						.map(m -> (List<Map<String,Object>>) m.get("Taxes")).flatMap(f -> f.stream()).collect(Collectors.toList());
				
				vatTax = taxList.stream().map(t -> t.get("TaxAmount") == null ? 0L : Double.valueOf(t.get("TaxAmount").toString()).longValue())
						.reduce(0L, (a,b) -> a + b);
				
				vatPercentage = tax.get(0).get("TaxRate")==null?0L:Double.valueOf(tax.get(0).get("TaxRate").toString());
				
				premium = pre.longValue();
				
				Long totalPremium =pre.longValue()+vatTax.longValue();
						
				log.info("CALC BLOCK END: "+new Date());
				
				//==============================CALC BLOCK END=============================================================
				
				//==============================USER CREATION BLOCK START=============================================
				
				log.info("USER CREATION BLOCK START: "+new Date());
				
				Map<String,Object> userCreationMap = new HashMap<String,Object>();
				userCreationMap.put("CompanyId", "100049");
				userCreationMap.put("CustomerId", custRefNo);
				userCreationMap.put("ProductId", "5");
				userCreationMap.put("ReferenceNo", reqRefNo);
				userCreationMap.put("UserMobileNo", mobileNo);
				userCreationMap.put("UserMobileCode", "268");
				userCreationMap.put("AgencyCode", "14035"); //local-13506,UAT-14035
				
				Map<String,Object> userResult = null;
				Map<String,Object> userRes = null;
				try {
					String userCreationReq = mapper.writeValueAsString(userCreationMap);
					String userCreationApi = loginCreationApi;
					
					log.info("USER CREATION API: "+userCreationApi);
					
					String apiResponse = thread.callSwazilandComApi(userCreationApi, userCreationReq);
					
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
				buypolicyMap.put("CreatedBy", "SZL_Whatsapp");
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
					
					String apiResponse = thread.callSwazilandComApi(buyPolicyApi, buypolicyReq);
					
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
				
				log.info("MAKE PAYMENT BLOCK START : "+new Date());
				Map<String,Object> makePaymentMap = new HashMap<String,Object>();
				makePaymentMap.put("CreatedBy", "SZL_Whatsapp");
				makePaymentMap.put("EmiYn", "N");
				makePaymentMap.put("InstallmentMonth", null);
				makePaymentMap.put("InstallmentPeriod", null);
				makePaymentMap.put("InsuranceId", "100049");
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
					String apiResponse = thread.callSwazilandComApi(makePaymentApi, makePayemantReq);
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
					String apiResponse = thread.callSwazilandComApi(insertPaymentApi, insertPaymentReq);
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
			    
			  //==============================PAYMENT LINK BLOCK END=============================================
			    
			  //==============================WHATSAPP RESPONSE BLOCK START=============================================
			    log.info("WHATSAPP RESPONSE BLOCK START : "+new Date());
			    
			    Map<String,Object> getMotorReq = new HashMap<String, Object>();
				getMotorReq.put("RequestReferenceNo", reqRefNo);
				
				String api_request =mapper.writeValueAsString(getMotorReq);
				String motorDetailsApi = getAllMotorDetailsApi;
				String apiResponse = thread.callSwazilandComApi(motorDetailsApi, api_request);
				
				Map<String,Object> getMotorRes =mapper.readValue(apiResponse, Map.class);
				
				List<Map<String,Object>> motorRes =getMotorRes.get("Result")==null?null:
					mapper.readValue(mapper.writeValueAsString(getMotorRes.get("Result")), List.class);
				
				log.info("ALL MOTOR DETAILS :" +motorRes);
				
				Map<String,Object> mot = motorRes.get(0);
				
				botResponceData.put("registration", mot.get("Registrationnumber")==null?"N/A":mot.get("Registrationnumber"));
				botResponceData.put("usage", mot.get("MotorUsageDesc")==null?"N/A":mot.get("MotorUsageDesc"));
				botResponceData.put("vehtype", mot.get("VehicleTypeDesc")==null?"N/A":mot.get("VehicleTypeDesc"));
				botResponceData.put("color",mot.get("ColorDesc")==null?"N/A":mot.get("ColorDesc"));
				//botResponceData.put("insurance_class",insuredClass);
				botResponceData.put("premium", premium);
				botResponceData.put("url", paymentUrl);
				botResponceData.put("vatamt", vatTax);
				botResponceData.put("suminsured", mot.get("SumInsured")==null?"N/A":mot.get("SumInsured"));
				botResponceData.put("chassis", mot.get("Chassisnumber")==null?"N/A":mot.get("Chassisnumber"));
				botResponceData.put("vat", String.valueOf(vatPercentage.longValue()));
				botResponceData.put("totalpremium", totalPremium);
				botResponceData.put("inceptiondate", policyDate);
				botResponceData.put("expirydate",policyEndDate);
				botResponceData.put("referenceno", reqRefNo);
				botResponceData.put("veh_model_desc", mot.get("VehcilemodelDesc")==null?"N/A":mot.get("VehcilemodelDesc"));
				botResponceData.put("veh_make_desc", mot.get("VehiclemakeDesc")==null?"N/A":mot.get("VehiclemakeDesc"));
				botResponceData.put("customer_name", customerName);
				
				
				return botResponceData;
			}


	@Override
	public Object swazilandQuoteGenerate(Object req)
			throws JsonProcessingException, JsonMappingException, WhatsAppValidationException {
		
		Map<String,Object> data = mapper.convertValue(req, Map.class);
		
		String customerName = data.get("cus_name") == null ? "":data.get("cus_name").toString();
		String mobileNo = data.get("mob_no") == null ? "":data.get("mob_no").toString();
		String idNumber = data.get("id_num") == null ? "":data.get("id_num").toString();
		//String mobileNum = data.get("") == null ? "":data.get("").toString();
		String address = data.get("address") == null ? "":data.get("address").toString();
		String regNo = data.get("reg_no") == null ? "":data.get("reg_no").toString();
		String engineNo = data.get("engine_num") == null ? "":data.get("engine_num").toString();
		String chassisNo = data.get("chassis_no") == null ? "":data.get("chassis_no").toString();
		String motorUsage = data.get("motor_usage") == null ? "":data.get("motor_usage").toString();
		String color = data.get("color") == null ? "":data.get("color").toString();
		String manYear = data.get("manufacture_year") == null ? "":data.get("manufacture_year").toString();
		String grossWeight = data.get("gross_wt") == null ? "":data.get("gross_wt").toString();
		String seatingCapacity = data.get("seating_capacity") == null ? "":data.get("seating_capacity").toString();
		String model = data.get("model") == null ? "":data.get("model").toString();
		String make = data.get("make") == null ? "":data.get("make").toString();
		String bodyType = data.get("body_type") == null ? "":data.get("body_type").toString();
		String sum_insured = data.get("sum_insured") == null ? "":data.get("sum_insured").toString();
		//String enginecapacity = data.get("tonnage") == null ? "":data.get("tonnage").toString();
		
		String response = "";
		String exception = "";
		List<Error> errorList = new ArrayList<>(2);
		Map<String,Object> botResponceData = new HashMap<String,Object>();
		String motorUsageId="";
		String bodyTypeId="";
		String makeId="";
		String modelId="";
		String colorId="";
		//masterIds
		Map<String,Object> motorusageMap = new HashMap<>();
		motorusageMap.put("Desc", motorUsage);
		motorusageMap.put("MasterType", "MOTOR_USAGE");
		motorusageMap.put("InsuranceId", "100049");
		
		try {
			String motorUsageReq = mapper.writeValueAsString(motorusageMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callSwazilandComApi(masterApi,motorUsageReq);
			
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
		bodyTypeMap.put("InsuranceId", "100049");
		
		try {
			String bodyTypeReq = mapper.writeValueAsString(bodyTypeMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callSwazilandComApi(masterApi,bodyTypeReq);
			
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
		makeMap.put("InsuranceId", "100049");
		
		try {
			String makeReq = mapper.writeValueAsString(makeMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callSwazilandComApi(masterApi,makeReq);
			
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
		modelMap.put("InsuranceId", "100049");
		
		try {
			String modelReq = mapper.writeValueAsString(modelMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callSwazilandComApi(masterApi,modelReq);
			
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
		
		Map<String,Object> colorMap = new HashMap<>();
		colorMap.put("Desc", color);
		colorMap.put("MasterType", "VEHICLE_COLOR");
		colorMap.put("InsuranceId", "100049");
		
		try {
			String colorReq = mapper.writeValueAsString(colorMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callSwazilandComApi(masterApi,colorReq);
			
			log.info("Master Api Response: "+apiResponse);
			Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
			
			Map<String,Object> masterApiResult = cust.get("Result") == null ? null :
				mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
			
		   
			if(masterApiResult == null) {
				String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}else {
				colorId = masterApiResult.get("Response")==null?"12":masterApiResult.get("Response").toString();
			}
		}catch(Exception e) {
			e.printStackTrace();
			log.info(e);
		}
		
		
		
		
		//==============================SAVE VEHICLE INFO BLOCK START=============================================
		log.info("SAVE VEHICLE INFO START: "+new Date());
		
		Map<String,Object> vehicleInfo = new HashMap<String,Object>();
		vehicleInfo.put("Insuranceid", "100049");
		vehicleInfo.put("BranchCode", "119");
		vehicleInfo.put("AxelDistance", 1);
		vehicleInfo.put("Chassisnumber", chassisNo);
		vehicleInfo.put("Color", colorId);//
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
		vehicleInfo.put("ResEngineCapacity", "1200");
		vehicleInfo.put("ResOwnerName", customerName);
		vehicleInfo.put("ResStatusCode", "Y");
		vehicleInfo.put("ResStatusDesc", "None");
		vehicleInfo.put("SeatingCapacity", seatingCapacity);
		vehicleInfo.put("HorsePower", "0");
		//vehicleInfo.put("Tareweight", null);
		vehicleInfo.put("Vehcilemodel", model);
		vehicleInfo.put("VehcilemodelId", modelId);//modelId
		vehicleInfo.put("VehicleType", bodyType);
		vehicleInfo.put("Vehiclemake", make);
		vehicleInfo.put("VehiclemakeId", makeId);//makeId
		//vehicleInfo.put("DisplacementInCM3", null);
		//vehicleInfo.put("NumberOfCylinders", 0);
		//vehicleInfo.put("PlateType", null);
		
		try {
			String saveVehicleInfo = saveVehicleInfoApi;
			String vehReq = mapper.writeValueAsString(vehicleInfo);
			log.info("Save Vehicle Info Calling: "+saveVehicleInfo);
			log.info("Save Vehicle Request: "+vehReq);
		//	String token = thread.getSwazilandToken();
			String vehResponse = thread.callSwazilandComApi(saveVehicleInfoApi,vehReq);
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
		
				
		//==============================SAVE VEHICLE INFO BLOCK END=============================================
		
		//==============================CUSTOMER CREATION BLOCK START=============================================
		
		//Cus Age
		LocalDate today = LocalDate.now();
		LocalDate cusDob = today.minusYears(20);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		String dob ="";
		try {
			dob = cusDob.format(formatter);
		}catch(Exception e) {
			e.printStackTrace();
		}
		log.info("CUSTOMER CREATION BLOCK START: "+new Date());
		Map<String,Object> customerCreation = new HashMap<String,Object>();
		customerCreation.put("Activities", "");
		customerCreation.put("Address1", address);
		customerCreation.put("Address2", "");
		customerCreation.put("AppointmentDate", "");
		customerCreation.put("BranchCode", "119");
		customerCreation.put("BrokerBranchCode", "1");
		customerCreation.put("BusinessType", null);
		customerCreation.put("CityCode", "3");
		customerCreation.put("CityName", "Lubombo");//check district or region
		customerCreation.put("ClientName", customerName);
		customerCreation.put("Clientstatus", "Y");
		customerCreation.put("Country","SZL" );
		customerCreation.put("CountryName", "Swaziland");
		customerCreation.put("CreatedBy", "SZL_Whatsapp");//create login for whatsapp bot
		customerCreation.put("CustomerAsInsurer", "N");
		customerCreation.put("CustomerReferenceNo", "");
		customerCreation.put("DobOrRegDate", dob);
		customerCreation.put("Email1", "");
		customerCreation.put("Email2", null);
		customerCreation.put("Email3", null);
		customerCreation.put("ExpiryDate", null);
		customerCreation.put("Fax", null);
		customerCreation.put("Gender", "M");
		customerCreation.put("IdNumber", idNumber);
		customerCreation.put("IdType", "3");
		customerCreation.put("InsuranceId", "100049");
		customerCreation.put("IsTaxExempted", "N");
		customerCreation.put("Language", "1");
		customerCreation.put("LastName", "");
		customerCreation.put("MaritalStatus", "Single");//check
		customerCreation.put("MiddleName", "");
		customerCreation.put("MobileCode1", "268");
		customerCreation.put("MobileCodeDesc1", "1");
		customerCreation.put("MobileNo1", mobileNo);
		customerCreation.put("MobileNo2", "");
		customerCreation.put("MobileNo3", null);
		customerCreation.put("Nationality", "");
		customerCreation.put("Occupation", "99999");
		customerCreation.put("OtherOccupation", "");
		customerCreation.put("PhoneNoCode", "");
		customerCreation.put("PinCode", "");
		customerCreation.put("Placeofbirth", address);
		customerCreation.put("PolicyHolderType", "1");
		customerCreation.put("PolicyHolderTypeid", "3");
		customerCreation.put("PreferredNotification", "sms");
		customerCreation.put("ProductId", "5");
		customerCreation.put("RegionCode", "3");
		customerCreation.put("RiskAssessmentDate", null);
		customerCreation.put("SaveOrSubmit", "Save");
		customerCreation.put("SocioProfessionalCategory", null);
		customerCreation.put("StateCode", "3");
		customerCreation.put("StateName", null);
		customerCreation.put("Status", "Y");
		customerCreation.put("Street", address);
		customerCreation.put("TaxExemptedId", null);
		customerCreation.put("TelephoneNo1", "");
		customerCreation.put("TelephoneNo2", null);
		customerCreation.put("TelephoneNo3", null);
		customerCreation.put("Title", "1");
		customerCreation.put("Type", null);
		customerCreation.put("VipFlag", null);
		customerCreation.put("VrTinNo", null);
		customerCreation.put("WhatsappCode", "");
		customerCreation.put("WhatsappDesc", "1");
		customerCreation.put("WhatsappNo", mobileNo);
		customerCreation.put("Zone", "1");
		
		String custRefNo = "";
		
		try {
			String cusReq = mapper.writeValueAsString(customerCreation);
			String cusSaveApi = saveCustomerApi;
			log.info("Customer Save Calling: "+cusSaveApi);
			log.info("Customer Save CallingRequest: "+cusReq);
			
			String apiResponse = thread.callSwazilandComApi(cusSaveApi,cusReq);
			
			log.info("Customer Save Response: "+apiResponse);
			Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
			
			Map<String,Object> custResult = cust.get("Result") == null ? null :
				mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
			
		    
			
			if(custResult == null) {
				String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}else {
				custRefNo = custResult.get("SuccessId") == null ? "" : custResult.get("SuccessId").toString();
			}
		}catch(Exception e) {
			e.printStackTrace();
			log.info(e);
		}
		
		log.info("CUSTOMER CREATION BLOCK END: "+new Date());
		
		//==============================CUSTOMER CREATION BLOCK END=============================================
		//==============================SHOW VEHICLE INFO BLOCK START=============================================
		log.info("SHOW VEHICLE INFO BLOCK START: "+new Date());
		Map<String,Object> vehInfo = new HashMap<String,Object>();
		vehInfo.put("BranchCode", "119");
		vehInfo.put("BrokerBranchCode", "1");
		vehInfo.put("CreatedBy", "SZL_Whatsapp");
		vehInfo.put("InsuranceId", "100049");
		vehInfo.put("ProductId", "5");
		vehInfo.put("ReqChassisNumber", "");
		vehInfo.put("ReqRegNumber", regNo);
		vehInfo.put("SavedFrom", "API");
		
		Map<String,Object> showVehResult = null;
		try {
			String showInfo = mapper.writeValueAsString(vehInfo);
			String showVehApi = showVehicleInfoApi;
			
			log.info("Show Vehicle Api Calling: "+showVehApi);
			log.info("Show Vehicle Request: "+showInfo);
			
			String apiResponse = thread.callSwazilandComApi(showVehApi, showInfo);
			
			log.info("Show Vehicle Response: "+apiResponse);
			
			Map<String,Object> showVeh = mapper.readValue(apiResponse, Map.class);
			showVehResult = showVeh.get("Result") == null ? null : (Map<String, Object>) showVeh.get("Result");
				
			if(showVehResult == null) {
				String errorMessgae = showVeh.get("ErrorMessage") == null ? "" : showVeh.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}
					
		}catch(Exception e) {
			e.printStackTrace();
		}
		log.info("SHOW VEHICLE INFO BLOCK END: "+new Date());
		//==============================SHOW VEHICLE INFO BLOCK END=============================================
		
		//==============================MOTOR SAVE BLOCK START=============================================
		log.info("MOTOR SAVE BLOCK START: "+new Date());
		LocalDate endDate = today.plusDays(364);
		String policyEndDate = endDate.format(formatter);
		String policyDate = today.format(formatter);
		
		Map<String,Object> motorSave = new HashMap<String,Object>();
		motorSave.put("AboutVehicle", null);
		motorSave.put("AcExecutiveId", null);
		motorSave.put("AcccessoriesSumInsured", null);
		motorSave.put("AccessoriesInformation", "");
		motorSave.put("AdditionalCircumstances", "");
		motorSave.put("AgencyCode", "14035");//local-13495,UAT-14035
		motorSave.put("AggregatedValue", null);
		motorSave.put("ApplicationId", "1");
		motorSave.put("AxelDistance", 1);
		motorSave.put("BankingDelegation", "");
		motorSave.put("BdmCode", "2000010");//uAt-2000010,local-5555555
		motorSave.put("BorrowerType", null);
		motorSave.put("BranchCode", "119");
		motorSave.put("BrokerBranchCode", "1");
		motorSave.put("BrokerCode", "14035");//local-13495,UAT-14035
		motorSave.put("Chassisnumber", chassisNo);
		motorSave.put("CityLimit", null);
		motorSave.put("ClaimType", "0");
		motorSave.put("ClaimTypeDesc", null);
		motorSave.put("CollateralCompanyAddress", "");
		motorSave.put("CollateralCompanyName", "");
		motorSave.put("CollateralName", null);
		motorSave.put("CollateralYn", "N");
		motorSave.put("Color", "12");//colorId
		motorSave.put("ColorDesc", color);
		motorSave.put("CommissionType", null);
		motorSave.put("CoverNoteNo", null);
		motorSave.put("CreatedBy", "SZL_Whatsapp");//broker id
		motorSave.put("CubicCapacity", grossWeight);
		motorSave.put("Currency", "ZAR");
		motorSave.put("CustomerCode", "2000010");//uAt-2000010,local-5555555
		motorSave.put("CustomerName", customerName);
		motorSave.put("CustomerReferenceNo", custRefNo);
		motorSave.put("DateOfCirculation", null);
		motorSave.put("Deductibles", null);
		motorSave.put("DefenceValue", "");
	//	motorSave.put("DisplacementInCM3", null);
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
		motorSave.put("EngineCapacity", grossWeight);
		motorSave.put("EngineNumber", engineNo);
		motorSave.put("ExcessLimit", null);
		motorSave.put("ExchangeRate", "1.0");
		motorSave.put("FirstLossPayee", null);
		motorSave.put("FleetOwnerYn", "N");
		motorSave.put("FuelType", null);
		motorSave.put("FuelTypeDesc", "");
		motorSave.put("Gpstrackinginstalled", "N");
		motorSave.put("Grossweight", grossWeight);
		motorSave.put("HavePromoCode", "N");
		motorSave.put("HoldInsurancePolicy", "N");
		motorSave.put("HorsePower", "0");
		motorSave.put("Idnumber", idNumber);
		motorSave.put("Inflation", "");
		motorSave.put("InflationSumInsured", "");
		motorSave.put("InsuranceClass", "0");
		motorSave.put("InsuranceClassDesc", null);
		motorSave.put("InsuranceId", "100049");
		motorSave.put("Insurancetype", "103");//103 check
		motorSave.put("InsurancetypeDesc", "Comprehensive");
		motorSave.put("InsurerSettlement", "");
		motorSave.put("InterestedCompanyDetails", "");
		motorSave.put("IsFinanceEndt", null);
		motorSave.put("LoanAmount", 0);
		motorSave.put("LoanEndDate", null);
		motorSave.put("LoanStartDate", null);
	//	motorSave.put("LocationId", "1");
		motorSave.put("LoginId", "SZL_Whatsapp");//login
		motorSave.put("ManufactureYear", manYear);
		motorSave.put("MarketValue", null);
		motorSave.put("Mileage", null);
		motorSave.put("MobileCode", "268");
		motorSave.put("MobileNumber", mobileNo);
		motorSave.put("ModelNumber", null);
		motorSave.put("MotorCategory", null);
		motorSave.put("Motorusage", motorUsage);
		motorSave.put("MotorusageId", motorUsageId);//motorUsageId
		motorSave.put("MunicipalityTraffic", null);
		motorSave.put("NcdYn", "N");
		motorSave.put("Ncb", "0");
		motorSave.put("NewValue", null);
		motorSave.put("NoOfClaimYears", null);
		motorSave.put("NoOfClaims", null);
	//	motorSave.put("NoOfComprehensives", null);
		motorSave.put("NoOfFemale", null);
		motorSave.put("NoOfMale", null);
		motorSave.put("NoOfPassengers", null);
		motorSave.put("NoOfVehicles", "1");
		motorSave.put("NumberOfAxels", null);
		motorSave.put("NumberOfCards", null);
		motorSave.put("NumberOfCylinders", null);
		motorSave.put("Occupation", "99999");
		motorSave.put("OrginalPolicyNo", null);
		motorSave.put("OwnerCategory", null);
		motorSave.put("PaCoverId", "0");
		motorSave.put("PlateType", null);
		motorSave.put("PolicyEndDate", policyEndDate);
		motorSave.put("PolicyRenewalYn", "N");
		motorSave.put("PolicyStartDate", policyDate);
		motorSave.put("PolicyType", "1");
		motorSave.put("PurchaseDate", null);
		motorSave.put("PreviousInsuranceYN", "N");
		motorSave.put("PreviousLossRatio", "");
		motorSave.put("ProductId", "5");
		motorSave.put("PromoCode", null);
		motorSave.put("PurchaseDate", null);
		motorSave.put("QuoteExpiryDays", 90);
		motorSave.put("RadioOrCasseteplayer", null);
		motorSave.put("RegistrationDate", null);
		motorSave.put("RegistrationYear", dob);//check
		motorSave.put("Registrationnumber", regNo);
		motorSave.put("RequestReferenceNo", "");
		motorSave.put("RoofRack", null);
	//	motorSave.put("SaveOrSubmit", "Save");
		motorSave.put("SavedFrom", "WEB");
		motorSave.put("SearchFromApi", false);
		motorSave.put("SeatingCapacity", seatingCapacity);
		motorSave.put("SectionId", Arrays.asList("103"));
	//	motorSave.put("SourceType", "Broker");
		motorSave.put("SourceTypeId", "Broker");
		motorSave.put("SpotFogLamp", null);
		motorSave.put("Status", "Y");
		motorSave.put("Stickerno", null);
		motorSave.put("SubUserType", "Broker");
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
		motorSave.put("VehcilemodelId", modelId);//modelId
		motorSave.put("VehicleId", "1");
		motorSave.put("VehicleType", bodyType);
		motorSave.put("VehicleTypeId", bodyTypeId);//bodyTypeId
		motorSave.put("VehicleTypeIvr", "90");
		motorSave.put("VehicleValueType", "");
		motorSave.put("Vehiclemake", make);
		motorSave.put("VehiclemakeId", makeId);//makeId
		motorSave.put("WindScreenSumInsured", null);
		motorSave.put("Windscreencoverrequired", null);
		motorSave.put("Zone", "1");
		motorSave.put("ZoneCirculation", null);
		motorSave.put("accident", null);
		motorSave.put("periodOfInsurance", "365");//inusredPeriod
		LinkedHashMap<String, Object> exchangeRateScenario = new  LinkedHashMap<>();
		exchangeRateScenario.put("OldAcccessoriesSumInsured", null);
		exchangeRateScenario.put("OldCurrency", "ZAR");
		exchangeRateScenario.put("OldExchangeRate", "1.0");
		exchangeRateScenario.put("OldSumInsured", 0);
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
			
			String apiResponse = thread.callSwazilandComApi(saveMotorApi,motorSaveReq);
			
			log.info("Save Motor Response: "+apiResponse);
			
			Map<String,Object> saveMot = mapper.readValue(apiResponse, Map.class);
			saveMotResult = saveMot.get("Result") == null ? null : (List<Map<String, Object>>) saveMot.get("Result");	
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
		calc.put("AgencyCode", "14035");//local-13506,UAT-14035
		calc.put("BranchCode", "119");
		calc.put("CdRefNo", motorSaveResult.get("CdRefNo") == null ? "" : motorSaveResult.get("CdRefNo"));
		calc.put("CoverModification", "N");
		calc.put("CreatedBy", "SZL_Whatsapp");
		calc.put("DdRefNo", motorSaveResult.get("DdRefNo") == null ? "" : motorSaveResult.get("DdRefNo"));
		calc.put("EffectiveDate", policyDate);
		calc.put("InsuranceId", "100049");
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
			String apiResponse = thread.callSwazilandComApi(calculatorApi, calReq);
			
			log.info("Calc Response: "+apiResponse);
			
			 calcRes = mapper.readValue(apiResponse, Map.class);
			 coverList = calcRes.get("CoverList") == null ? null :
				 mapper.readValue(mapper.writeValueAsString(calcRes.get("CoverList")), List.class);
			 
			//calcResult = calcRes.get("Result") == null ? null : (Map<String, Object>) calcRes.get("Result");
			
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
		
		List<Map<String,Object>> tax = (List<Map<String, Object>>) coverList.get(0).get("Taxes");
		
		BigDecimal pre = coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
				.map(m -> new BigDecimal(m.get("PremiumExcluedTaxLC").toString())).reduce(BigDecimal.ZERO, (x,y) -> x.add(y));
		
		List<Map<String,Object>> taxList = coverList.parallelStream().filter(p -> ("B".equalsIgnoreCase(p.get("CoverageType").toString()) 
				|| "D".equalsIgnoreCase(p.get("isSelected").toString())) && p.get("Taxes") != null)
				.map(m -> (List<Map<String,Object>>) m.get("Taxes")).flatMap(f -> f.stream()).collect(Collectors.toList());
		
		vatTax = taxList.stream().map(t -> t.get("TaxAmount") == null ? 0L : Double.valueOf(t.get("TaxAmount").toString()).longValue())
				.reduce(0L, (a,b) -> a + b);
		
		vatPercentage = tax.get(0).get("TaxRate")==null?0L:Double.valueOf(tax.get(0).get("TaxRate").toString());
		
		premium = pre.longValue();
		
		Long totalPremium =pre.longValue()+vatTax.longValue();
				
		log.info("CALC BLOCK END: "+new Date());
		
		//==============================CALC BLOCK END=============================================================
		
		//==============================USER CREATION BLOCK START=============================================
		
		log.info("USER CREATION BLOCK START: "+new Date());
		
		Map<String,Object> userCreationMap = new HashMap<String,Object>();
		userCreationMap.put("CompanyId", "100049");
		userCreationMap.put("CustomerId", custRefNo);
		userCreationMap.put("ProductId", "5");
		userCreationMap.put("ReferenceNo", reqRefNo);
		userCreationMap.put("UserMobileNo", mobileNo);
		userCreationMap.put("UserMobileCode", "268");
		userCreationMap.put("AgencyCode", "14035"); //local-13506,UAT-14035
		
		Map<String,Object> userResult = null;
		Map<String,Object> userRes = null;
		try {
			String userCreationReq = mapper.writeValueAsString(userCreationMap);
			String userCreationApi = loginCreationApi;
			
			log.info("USER CREATION API: "+userCreationApi);
			
			String apiResponse = thread.callSwazilandComApi(userCreationApi, userCreationReq);
			
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
		buypolicyMap.put("CreatedBy", "SZL_Whatsapp");
		buypolicyMap.put("ProductId", "5");
		buypolicyMap.put("ManualReferralYn", "N");
		buypolicyMap.put("Vehicles", vehiMapList);
		
		Map<String,Object> buyPolicyResult = null;
		Map<String,Object> buyPolicyRes = null;
		try {
			String buypolicyReq =objectPrint.toJson(buypolicyMap);
			String buyPolicyApi = buyPolicy;
			
			log.info("BUY POLICY API: "+buyPolicyApi);
			log.info("BUY POLICY Request: "+buypolicyReq);
			
			String apiResponse = thread.callSwazilandComApi(buyPolicyApi, buypolicyReq);
			
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
		
		log.info("MAKE PAYMENT BLOCK START : "+new Date());
		Map<String,Object> makePaymentMap = new HashMap<String,Object>();
		makePaymentMap.put("CreatedBy", "SZL_Whatsapp");
		makePaymentMap.put("EmiYn", "N");
		makePaymentMap.put("InstallmentMonth", null);
		makePaymentMap.put("InstallmentPeriod", null);
		makePaymentMap.put("InsuranceId", "100049");
		makePaymentMap.put("Premium", totalPremium);
		makePaymentMap.put("QuoteNo", buyPolicyResult.get("QuoteNo"));
		makePaymentMap.put("Remarks", "None");
		makePaymentMap.put("SubUserType", "broker");
		makePaymentMap.put("UserType", "Broker");
		
		Map<String,Object> makePaymentResult =null;
		Map<String,Object> makePaymentRes =null;
		try {
			String makePayemantReq = objectPrint.toJson(makePaymentMap);
			
			String makePaymentApi = makePayment;
			log.info("MAKE PAYMENT API: "+makePaymentApi);
			
			log.info("MAKE PAYMENT REQUEST: "+makePayemantReq);
			String apiResponse = thread.callSwazilandComApi(makePaymentApi, makePayemantReq);
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
		insertPaymentMap.put("SubUserType", "broker");
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
			String apiResponse = thread.callSwazilandComApi(insertPaymentApi, insertPaymentReq);
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
	    
	  //==============================PAYMENT LINK BLOCK END=============================================
	    
	  //==============================WHATSAPP RESPONSE BLOCK START=============================================
	    log.info("WHATSAPP RESPONSE BLOCK START : "+new Date());
	    
	    Map<String,Object> getMotorReq = new HashMap<String, Object>();
		getMotorReq.put("RequestReferenceNo", reqRefNo);
		
		String api_request =mapper.writeValueAsString(getMotorReq);
		String motorDetailsApi = getAllMotorDetailsApi;
		String apiResponse = thread.callSwazilandComApi(motorDetailsApi, api_request);
		
		Map<String,Object> getMotorRes =mapper.readValue(apiResponse, Map.class);
		
		List<Map<String,Object>> motorRes =getMotorRes.get("Result")==null?null:
			mapper.readValue(mapper.writeValueAsString(getMotorRes.get("Result")), List.class);
		
		log.info("ALL MOTOR DETAILS :" +motorRes);
		
		Map<String,Object> mot = motorRes.get(0);
		
		botResponceData.put("registration", mot.get("Registrationnumber")==null?"N/A":mot.get("Registrationnumber"));
		botResponceData.put("usage", mot.get("MotorUsageDesc")==null?"N/A":mot.get("MotorUsageDesc"));
		botResponceData.put("vehtype", mot.get("VehicleTypeDesc")==null?"N/A":mot.get("VehicleTypeDesc"));
		botResponceData.put("color",mot.get("ColorDesc")==null?"N/A":mot.get("ColorDesc"));
		//botResponceData.put("insurance_class",insuredClass);
		botResponceData.put("premium", premium);
		botResponceData.put("url", paymentUrl);
		botResponceData.put("vatamt", vatTax);
		botResponceData.put("suminsured", mot.get("SumInsured")==null?"N/A":mot.get("SumInsured"));
		botResponceData.put("chassis", mot.get("Chassisnumber")==null?"N/A":mot.get("Chassisnumber"));
		botResponceData.put("vat", String.valueOf(vatPercentage.longValue()));
		botResponceData.put("totalpremium", totalPremium);
		botResponceData.put("inceptiondate", policyDate);
		botResponceData.put("expirydate",policyEndDate);
		botResponceData.put("referenceno", reqRefNo);
		botResponceData.put("veh_model_desc", mot.get("VehcilemodelDesc")==null?"N/A":mot.get("VehcilemodelDesc"));
		botResponceData.put("veh_make_desc", mot.get("VehiclemakeDesc")==null?"N/A":mot.get("VehiclemakeDesc"));
		botResponceData.put("customer_name", customerName);
		
		log.info("WHATSAPP RESPONSE BLOCK END : "+new Date());
		return botResponceData;

			
	}
	/*List<String> questionTexts = Arrays.asList(
			"Please Enter the Vehicle Registration Number",
			"Please Enter the Vehicle Engine Number",
			"Please Enter the Vehicle Chassis Number",
			"Please Enter the Type of Motor Usage",
			"Please Enter the Body Type of Vehicle",
			"Please Enter the Make",
			"Please Enter the Vehicle Model",
			"Please Enter Seating Capacity of the Vehicle (Eg:5)",
			"Please Enter the Vehicle Manufacture Year (Eg:2025)",
			"Please Enter the Vehicle Gross Weight in Kgs (Eg:500)",
			"Please Enter the Vehicle Color",
			"Please Enter the Policy Holder Name",
			"Please Enter the Policy Holder Id Number",
			"Please Enter the Policy Holder Address",
			"Please Enter the Sum Insured (in Numerical numbers)",
			"Please Enter the Payment Mobile Number (without country code and Zero)",
			"Thank you! You've completed the questionnaire. please type OK"
      );
	
	List<String> apiKeys = Arrays.asList(
		"reg_no","engine_num","chassis_no","motor_usage","body_type","make","model","seating_capacity",
		"manufacture_year","gross_wt","color","cus_name","id_num","address","sum_insured","mob_no","thank_you"
			);
	
	//private final Map<String, UserSession> sessionMap = new HashMap<>();
*/

	@Override
	public String swazilandFlowRequest(Map<String, Object> req) {
		log.info("WebHook Req :" +req);
		
		String quoteResponse = "";
		
		PhoenixUserDataDetails userDetail = new PhoenixUserDataDetails();
		List<Map<String,Object>> entry = (List<Map<String, Object>>) req.get("entry");
		List<Map<String,Object>> changes = (List<Map<String, Object>>) entry.get(0).get("changes");
		Map<String,Object> value = (Map<String, Object>) changes.get(0).get("value");
		List<Map<String,Object>> contacts = (List<Map<String, Object>>) value.get("contacts");
		Long waId = Long.valueOf((String) contacts.get(0).get("wa_id")) ;
		List<Map<String,Object>> messages = (List<Map<String, Object>>) value.get("messages");
		String waMsgId = messages.get(0).get("id").toString();
		String type = messages.get(0).get("type").toString();
		Map<String,Object> text = null;
		String bodyDesc="";
		Map<String,Object> interactive = null;
		Map<String,Object> list_reply = null;
		Map<String,Object> nfm_reply = null;
		if(type.equalsIgnoreCase("text")) {
		     text = (Map<String, Object>) messages.get(0).get("text");
			 bodyDesc = text.get("body").toString();
		}else if(type.equalsIgnoreCase("interactive")) {
			interactive = (Map<String, Object>) messages.get(0).get("interactive");
			nfm_reply = interactive.get("nfm_reply") == null ? null : (Map<String, Object>) interactive.get("nfm_reply");
			//list_reply = (Map<String, Object>) interactive.get("list_reply");
			bodyDesc = nfm_reply.get("name").toString();
		}
		Map<String,Object> flowReq =null;
		String flow ="";
		if(bodyDesc.equalsIgnoreCase("flow")) {
			 flow = nfm_reply.get("response_json").toString();
			
			try {
				flowReq = mapper.readValue(flow, Map.class);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

		//interactive.getOrDefault(list_reply, value)
		if(bodyDesc.equalsIgnoreCase("hi")) {
			userDetail.setParentMessageId("MSG001");
			userDetail.setUserMessageId("MSG001");
			userDetail.setEntryDate(new Date());
			userDetail.setUserReply(bodyDesc);
			userDetail.setStatus("Y");
			userDetail.setWaid(waId);
			userDetail.setWamessageid(waMsgId);
			userDetail.setCompanyId("100049");
			
			// sessionMap.remove(waId.toString());
			
		//	userDataRepo.saveAndFlush(userDetail);
		}else if(bodyDesc.equalsIgnoreCase("Motor Insurance")){
			userDetail.setParentMessageId("MSG001");
			userDetail.setUserMessageId("MSG002");
			userDetail.setEntryDate(new Date());
			userDetail.setUserReply(bodyDesc);
			userDetail.setStatus("Y");
			userDetail.setWaid(waId);
			userDetail.setWamessageid(waMsgId);
			userDetail.setCompanyId("100049");
			// sessionMap.remove(waId.toString());
			
		//	userDataRepo.saveAndFlush(userDetail);
		}else if(bodyDesc.equalsIgnoreCase("Buy New Insurance")){
			userDetail.setParentMessageId("MSG002");
			userDetail.setUserMessageId("MSG003");
			userDetail.setEntryDate(new Date());
			userDetail.setUserReply(bodyDesc);
			userDetail.setStatus("Y");
			userDetail.setWaid(waId);
			userDetail.setWamessageid(waMsgId);
			userDetail.setCompanyId("100049");
			// sessionMap.remove(waId.toString());
			
			//userDataRepo.saveAndFlush(userDetail);
		}else if(bodyDesc.equalsIgnoreCase("flow")){
			userDetail.setParentMessageId("MSG003");
			userDetail.setUserMessageId("MSG004");
			userDetail.setEntryDate(new Date());
			userDetail.setUserReply(bodyDesc);
			userDetail.setStatus("Y");
			userDetail.setWaid(waId);
			userDetail.setWamessageid(waMsgId);	
			userDetail.setCompanyId("100049");
			userDetail.setFlowRequest(flow);
			
			try {
				quoteResponse =  callYourAPI(flowReq);
			} catch (JsonProcessingException | WhatsAppValidationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.info(e);
				
			}
		}				
		userDataRepo.saveAndFlush(userDetail);
		
		if(StringUtils.isNotBlank(quoteResponse)) {
			responseMethodCall(quoteResponse,waId.toString());
		}
		
		return null;
	}
	/*public String handleIncomingMessage(String waIds, String textBody) throws JsonMappingException, JsonProcessingException, WhatsAppValidationException {
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

        // Send next question or finalize
        if (index + 1 < questionTexts.size()) {
            sendWhatsappMessage(waIds, questionTexts.get(index + 1));
        } else {
            apiResult = callYourAPI(session.getResponses());
            sessionMap.remove(waIds);
            sendWhatsappMessage(waIds, "✅ Thank you! All your details have been received.");
            
            if(StringUtils.isNoneBlank(apiResult)) {
            	responseMethodCall(apiResult,waIds);
            }
        }
		return apiResult;
	} */

	
	
	private void responseMethodCall(String apiResult, String waIds) {
		
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
		
		Map<String,Object> colorMap = new HashMap<>();
		colorMap.put("type", "text");
		colorMap.put("text", vehicleColor);
		
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
		
		Map<String,Object> expDateMap = new HashMap<>();
		expDateMap.put("type", "text");
		expDateMap.put("text", expiryDate);
		
		Map<String,Object> refNoMap = new HashMap<>();
		refNoMap.put("type", "text");
		refNoMap.put("text", referenceNo);
		
		Map<String,Object> urlMap = new HashMap<>();
		urlMap.put("type", "text");
		urlMap.put("text", payment);
		
		//Map<String,Object> makeMap = new HashMap<>();
		//makeMap.put("type", "text");
		//makeMap.put("text", make);
		
		//Map<String,Object> modelMap = new HashMap<>();
		//modelMap.put("type", "text");
		//modelMap.put("text", model);
		
		componentsMap.put("parameters", Arrays.asList(nameMap,regNoMap,chassisNoMap,vehUsageMap,vehTypeMap,colorMap,sumInsuredMap,
				premiumMap,vatMap,vatAmtMap,totPremiumMap,incDateMap,expDateMap,refNoMap,urlMap));
		
		Map<String,Object> tempMap = new HashMap<>();
		tempMap.put("language", langMap);
		tempMap.put("name", "motor_quotation_res_swazi");
		tempMap.put("components", Arrays.asList(componentsMap));
		
		respMap.put("template", tempMap);
		
		String respMapreq = objectPrint.toJson(respMap);
		
		String apiUrl = askeveApi;
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


	//private void sendWhatsappMessage(String waId, String message) {
     //   System.out.println("Sending to " + waId + ": " + message);
     //   log.info("Sending to " + waId + ": " + message);
        // Here, you can call the Meta WhatsApp API to actually send the message.
   // }
  
  private  String callYourAPI(Map<String, Object> data) throws JsonMappingException, JsonProcessingException, WhatsAppValidationException {
        //String apiUrl = "http://localhost:6060/WhatsAppApiMeta/insurance/generate/swaziland/quote";
        
        log.info("Create Quote Api Req " + data);
        
       Map<String,Object> responsce= (Map<String, Object>) swazilandQuote(data);
       
       String resp = objectPrint.toJson(responsce);

     /*   RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(data, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            System.out.println("API Response: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error calling API: " + e.getMessage());
        } */
        
        return resp;
    }


@Override
public Object googleFlowTest(Object req)
		throws JsonProcessingException, JsonMappingException, WhatsAppValidationException {

	String response = "";
	String exception = "";
	List<Error> errorList = new ArrayList<>(2);
	Map<String, Object> botResponceData = new HashMap<String, Object>();

	Map<String, Object> data = mapper.convertValue(req, Map.class);

	// Customer Request Mapping
	String customerName = data.get("Customer Name") == null ? "" : data.get("Customer Name").toString();
	String title = data.get("Title") == null ? "" : data.get("Title").toString();
	String gender = data.get("Gender") == null ? "" : data.get("Gender").toString();
	String occupation = data.get("Occupation") == null ? "" : data.get("Occupation").toString();
	String mobileNo = data.get("Mobile Number") == null ? "" : data.get("Mobile Number").toString();
	String email = data.get("Email") == null ? "" : data.get("Email").toString();
	String idType = data.get("Id Type") == null ? "" : data.get("Id Type").toString();
	String idNumber = data.get("Id Number") == null ? "" : data.get("Id Number").toString();
	String region = data.get("Region") == null ? "" : data.get("Region").toString();
	String address = data.get("Address") == null ? "" : data.get("Address").toString();

	// Motor Request Mapping
	String motorUsage = data.get("Motor Usage") == null ? "" : data.get("Motor Usage").toString();
	String bodyType = data.get("Body Type") == null ? "" : data.get("Body Type").toString();
	String make = data.get("Make") == null ? "" : data.get("Make").toString();
	String model = data.get("Model") == null ? "" : data.get("Model").toString();
	String regNo = data.get("Registration Number") == null ? "" : data.get("Registration Number").toString();
	String engineNo = data.get("Engine Number") == null ? "" : data.get("Engine Number").toString();
	String chassisNo = data.get("Chassis Number") == null ? "" : data.get("Chassis Number").toString();
	String enginecapacity = data.get("Engine Capacity") == null ? "" : data.get("Engine Capacity").toString();
	String seatingCapacity = data.get("Seating Capacity") == null ? "" : data.get("Seating Capacity").toString();
	String manYear = data.get("Manufacture Year") == null ? "" : data.get("Manufacture Year").toString();
	String color = data.get("Color") == null ? "" : data.get("Color").toString();
	String grossWeight = data.get("Tonnage") == null ? "" : data.get("Tonnage").toString();

	// PolicyDetails Request Mapping
	String insuranceClass = data.get("Insurance Class") == null ? "" : data.get("Insurance Class").toString();
	String policyDate = data.get("Policy Start Date") == null ? "" : data.get("Policy Start Date").toString();
	String sum_insured = data.get("Sum Insured") == null ? "" : data.get("Sum Insured").toString();
	String extended_tppd_si = data.get("Extended Tppd Si") == null ? "" : data.get("Extended Tppd Si").toString();

	// DOB calc
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	LocalDate policyStartDate = LocalDate.parse(policyDate, inputFormatter);
	policyDate = policyStartDate.format(formatter);
	LocalDate curDate = LocalDate.parse(policyDate, formatter);
	LocalDate minusDate = curDate.minusYears(18);
	String cusDob = minusDate.format(formatter);

	String titlteId = "", genderId = "", occupationId = "", idTypeId = "", regionId = "", distictId = "", colorId = "",
			motorUsageId = "", bodyTypeId = "", makeId = "", modelId = "", insuranceClassId = "";

	// masterIds
	Map<String, Object> titleMap = new HashMap<>();
	titleMap.put("Desc", title);
	titleMap.put("MasterType", "CUSTOMER_TITLE");
	titleMap.put("InsuranceId", "100049");

	try {
		String titileReq = mapper.writeValueAsString(titleMap);
		String masterApi = masterIdsApi;
		log.info("Master Ids Api Calling: " + masterApi);

		String apiResponse = thread.callSwazilandComApi(masterApi, titileReq);

		log.info("Master Api Response: " + apiResponse);
		Map<String, Object> titleList = mapper.readValue(apiResponse, Map.class);

		Map<String, Object> masterApiResult = titleList.get("Result") == null ? null
				: mapper.readValue(mapper.writeValueAsString(titleList.get("Result")), Map.class);

		if (masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = titleList.get("ErrorMessage") == null ? "" : titleList.get("ErrorMessage").toString();
			response = errorMessage;
			return response;
		} else {
			titlteId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}
	} catch (Exception e) {
		e.printStackTrace();
		log.info(e.getMessage());
	}

	// Gender Id
	Map<String, Object> genderMap = new HashMap<>();
	genderMap.put("Desc", gender);
	genderMap.put("MasterType", "CUSTOMER_GENDER");
	genderMap.put("InsuranceId", "100049");

	try {
		String genderReq = mapper.writeValueAsString(genderMap);
		String masterApi = masterIdsApi;
		log.info("Master Ids Api Calling: " + masterApi);

		String apiResponse = thread.callSwazilandComApi(masterApi, genderReq);

		log.info("Master Api Response: " + apiResponse);
		Map<String, Object> respMap = mapper.readValue(apiResponse, Map.class);

		Map<String, Object> masterApiResult = respMap.get("Result") == null ? null
				: mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);

		if (masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
			response = errorMessage;
			return response;
		} else {
			genderId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}

	} catch (Exception e) {
		e.printStackTrace();
		log.info(e.getMessage());
	}

	// occupationId
	Map<String, Object> occupationMap = new HashMap<>();
	occupationMap.put("Desc", occupation);
	occupationMap.put("MasterType", "CUSTOMER_OCCUPATION");
	occupationMap.put("InsuranceId", "100049");
	
	try {
		String occupationReq = mapper.writeValueAsString(occupationMap);
		String masterApi = masterIdsApi;
		log.info("Master Ids Api Calling: " + masterApi);

		String apiResponse = thread.callSwazilandComApi(masterApi, occupationReq);
		
		log.info("Master Api Response: " + apiResponse);
		Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
		
		Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
			mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
		
		if(masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
			response = errorMessage;
			return response;
		}else {
			occupationId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}
		
	}catch(Exception e) {
		e.printStackTrace();
		log.info(e.getMessage());
	}
	
	//IdTYpe
	Map<String,Object> idTypeMap = new HashMap<>();
	idTypeMap.put("Desc", idType);
	idTypeMap.put("MasterType", "CUSTOMER_IDTYPE");
	idTypeMap.put("InsuranceId", "100049");
	
	try {
		String idTypeReq = mapper.writeValueAsString(idTypeMap);
		String masterApi = masterIdsApi;
		log.info("Master Ids Api Calling: " + masterApi);
		
		String apiResponse = thread.callSwazilandComApi(masterApi, idTypeReq);
		
		log.info("Master Api Response: " + apiResponse);
		Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
		
		Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
			mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
		
		if(masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("Result").toString();
			response = errorMessage;
			return response;
		}else {
			idTypeId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}
		
	}catch(Exception e) {
		e.printStackTrace();
		log.info(e);
	}
	
	//RegionId
	Map<String,Object> regionMap = new HashMap<>();
	regionMap.put("Desc", region);
	regionMap.put("MasterType", "CUSTOMER_REGION");
	regionMap.put("CountryCode", "SZL");
	
	try {
		String regionReq = mapper.writeValueAsString(regionMap);
		String masterApi = masterIdsApi;
		log.info("Master Ids Api Calling: "+masterApi);
		
		String apiResponse = thread.callSwazilandComApi(masterApi, regionReq);
		
		log.info("Master Api Response: " + apiResponse);
		Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
		
		Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
			mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
		
		if(masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
			response = errorMessage;
			return response;
		}else {
			regionId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}
	}catch(Exception e) {
		e.printStackTrace();
		log.info(e.getMessage());
	}
	
	//DistrictId
	Map<String,Object> districtMap = new HashMap<>();
	districtMap.put("Desc", region);
	districtMap.put("MasterType", "CUSTOMER_DISTRICT");
	districtMap.put("CountryCode", "SZL");
	
	try {
		String districtReq = mapper.writeValueAsString(districtMap);
		String masterApi = masterIdsApi;
        log.info("Master Ids Api Calling: "+masterApi);
		
		String apiResponse = thread.callSwazilandComApi(masterApi, districtReq);
		
		log.info("Master Api Response: " + apiResponse);
		Map<String, Object> respMap = mapper.readValue(apiResponse, Map.class);
		
		Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
			mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
		
		if(masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
			response = errorMessage;
			return response;
		}else {
			distictId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}
		
	}catch(Exception e) {
		e.printStackTrace();
		log.info(e.getMessage());
	}
	
	//InsuranceClassId
	Map<String,Object> insuranceClassMap = new HashMap<>();
	insuranceClassMap.put("Desc", insuranceClass);
	insuranceClassMap.put("MasterType", "INSURANCE_CLASS");
	insuranceClassMap.put("InsuranceId", "100049");
	
	try {
		String insuranceClassReq = mapper.writeValueAsString(insuranceClassMap);
		String masterApi = masterIdsApi;
        log.info("Master Ids Api Calling: "+masterApi);
		
		String apiResponse = thread.callSwazilandComApi(masterApi, insuranceClassReq);
		
		log.info("Master Api Response: " + apiResponse);
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
	}catch(Exception e){
		e.printStackTrace();
		log.info(e.getMessage());
	}
	
	//MotorUsageId
	Map<String,Object> motorusageMap = new HashMap<>();
	motorusageMap.put("Desc", motorUsage);
	motorusageMap.put("MasterType", "MOTOR_USAGE");
	motorusageMap.put("InsuranceId", "100049");
	
	try {
		String motorUsageReq = mapper.writeValueAsString(motorusageMap);
		String masterApi = masterIdsApi;
        log.info("Master Ids Api Calling: "+masterApi);
		
		String apiResponse = thread.callSwazilandComApi(masterApi, motorUsageReq);
		
		log.info("Master Api Response: " + apiResponse);
		Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
		
		Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
			mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
		
		if(masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
		}else {
			motorUsageId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}
		
	}catch(Exception e) {
		e.printStackTrace();
		log.info(e.getMessage());
	}
	
	//bodyTYpeId
	Map<String,Object> bodyTypeMap = new HashMap<>();
	bodyTypeMap.put("Desc", bodyType);
	bodyTypeMap.put("MasterType", "BODY_TYPE");
	bodyTypeMap.put("InsuranceId", "100049");
	
	try {
		String bodyTypeReq = mapper.writeValueAsString(bodyTypeMap);
		String masterApi = masterIdsApi;
        log.info("Master Ids Api Calling: "+masterApi);
		
		String apiResponse = thread.callSwazilandComApi(masterApi, bodyTypeReq);
		
		log.info("Master Api Response: " + apiResponse);
		
		Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
		
		Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
			mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
		
		if(masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
			response = errorMessage;
			return response;
		}else {
			bodyTypeId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}
	}catch(Exception e) {
		e.printStackTrace();
		log.info(e.getMessage());
	}
	
	//MakeId 
	Map<String,Object> makeMap = new HashMap<>();
	makeMap.put("Desc", make);
	makeMap.put("MasterType", "VEHICLE_MAKE");
	makeMap.put("InsuranceId", "100049");
	
	try {
		String makeReq = mapper.writeValueAsString(makeMap);
		String masterApi = masterIdsApi;
        log.info("Master Ids Api Calling: "+masterApi);
		
		String apiResponse = thread.callSwazilandComApi(masterApi, makeReq);
		
		log.info("Master Api Response: " + apiResponse);
		
		Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
		
		Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
			mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
		
		if(masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
			response = errorMessage;
			return response;
		}else {
			makeId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}
	}catch(Exception e) {
		e.printStackTrace();
		log.info(e.getMessage());
	}
	
	//ModelId
	Map<String,Object> modelMap = new HashMap<>();
	modelMap.put("Desc", model);
	modelMap.put("MasterType", "VEHICLE_MODEL");
	modelMap.put("InsuranceId", "100049");
	
	try {
		String modelReq = mapper.writeValueAsString(modelMap);
		String masterApi = masterIdsApi;
        log.info("Master Ids Api Calling: "+masterApi);
		
		String apiResponse = thread.callSwazilandComApi(masterApi, modelReq);
		
		log.info("Master Api Response: " + apiResponse);
		
		Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
		
		Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
			mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
		
		if(masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
			response = errorMessage;
			return response;
		}else {
			modelId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}
	}catch(Exception e) {
		e.printStackTrace();
		log.info(e.getMessage());
	}
	
	//colorId
	Map<String,Object> colorMap = new HashMap<>();
	colorMap.put("Desc", color);
	colorMap.put("MasterType", "VEHICLE_COLOR");
	colorMap.put("InsuranceId", "100049");
	
	try {
		String colorReq = mapper.writeValueAsString(colorMap);
		String masterApi = masterIdsApi;
        log.info("Master Ids Api Calling: "+masterApi);
		
		String apiResponse = thread.callSwazilandComApi(masterApi, colorReq);
		
		log.info("Master Api Response: " + apiResponse);
		
		Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
		
		Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
			mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
		
		if(masterApiResult.isEmpty() || masterApiResult == null) {
			String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
			response = errorMessage;
			return response;
		}else {
			colorId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
		}
		
	}catch(Exception e) {
		e.printStackTrace();
		log.info(e.getMessage());
	}
	
	//==============================SAVE VEHICLE INFO BLOCK START=============================================
	log.info("SAVE VEHICLE INFO START: "+new Date());
	
	Map<String,Object> vehicleInfo = new HashMap<String,Object>();
	vehicleInfo.put("Insuranceid", "100049");
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
		String vehResponse = thread.callSwazilandComApi(saveVehicleInfoApi,vehReq);
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
	
			
	//==============================SAVE VEHICLE INFO BLOCK END=============================================
	
	//==============================CUSTOMER CREATION BLOCK START=============================================
	log.info("CUSTOMER CREATION BLOCK START: "+new Date());
	Map<String,Object> customerCreation = new HashMap<String,Object>();
	customerCreation.put("Activities", "");
	customerCreation.put("Address1", address);
	customerCreation.put("Address2", "");
	customerCreation.put("AppointmentDate", "");
	customerCreation.put("BranchCode", "119");
	customerCreation.put("BrokerBranchCode", "1");
	customerCreation.put("BusinessType", null);
	customerCreation.put("CityCode", distictId);
	customerCreation.put("CityName", region);//check district or region
	customerCreation.put("ClientName", customerName);
	customerCreation.put("Clientstatus", "Y");
	customerCreation.put("Country", "SZL");
	customerCreation.put("CountryName", "Swaziland");
	customerCreation.put("CreatedBy", "SZL_Whatsapp");//create login for whatsapp bot
	customerCreation.put("CustomerAsInsurer", "N");
	customerCreation.put("CustomerReferenceNo", "");
	customerCreation.put("DobOrRegDate", cusDob);
	customerCreation.put("Email1", email);
	customerCreation.put("Email2", null);
	customerCreation.put("Email3", null);
	customerCreation.put("ExpiryDate", null);
	customerCreation.put("Fax", null);
	customerCreation.put("Gender", genderId);
	customerCreation.put("IdNumber", idNumber);
	customerCreation.put("IdType", idTypeId);
	customerCreation.put("InsuranceId", "100049");
	customerCreation.put("IsTaxExempted", "N");
	customerCreation.put("Language", "1");
	customerCreation.put("LastName", "");
	customerCreation.put("MaritalStatus", "Single");//check
	customerCreation.put("MiddleName", "");
	customerCreation.put("MobileCode1", "268");
	customerCreation.put("MobileCodeDesc1", "1");
	customerCreation.put("MobileNo1", mobileNo);
	customerCreation.put("MobileNo2", "");
	customerCreation.put("MobileNo3", null);
	customerCreation.put("Nationality", "");
	customerCreation.put("Occupation", occupationId);
	customerCreation.put("OtherOccupation", "");
	customerCreation.put("PhoneNoCode", "");
	customerCreation.put("PinCode", "");
	customerCreation.put("Placeofbirth", address);
	customerCreation.put("PolicyHolderType", "1");
	customerCreation.put("PolicyHolderTypeid", idTypeId);
	customerCreation.put("PreferredNotification", "sms");
	customerCreation.put("ProductId", "5");
	customerCreation.put("RegionCode", regionId);
	customerCreation.put("RiskAssessmentDate", null);
	customerCreation.put("SaveOrSubmit", "Save");
	customerCreation.put("SocioProfessionalCategory", null);
	customerCreation.put("StateCode", regionId);
	customerCreation.put("StateName", null);
	customerCreation.put("Status", "Y");
	customerCreation.put("Street", address);
	customerCreation.put("TaxExemptedId", null);
	customerCreation.put("TelephoneNo1", null);
	customerCreation.put("TelephoneNo2", null);
	customerCreation.put("TelephoneNo3", null);
	customerCreation.put("Title", titlteId);
	customerCreation.put("Type", null);
	customerCreation.put("VipFlag", null);
	customerCreation.put("VrTinNo", null);
	customerCreation.put("WhatsappCode", "268");
	customerCreation.put("WhatsappDesc", "1");
	customerCreation.put("WhatsappNo", mobileNo);
	customerCreation.put("Zone", "1");
	
	String custRefNo = "";
	
	try {
		String cusReq = mapper.writeValueAsString(customerCreation);
		String cusSaveApi = saveCustomerApi;
		log.info("Customer Save Calling: "+cusSaveApi);
		log.info("Customer Save Request: "+cusReq);
		
		String apiResponse = thread.callSwazilandComApi(cusSaveApi,cusReq);
		
		log.info("Customer Save Response: "+apiResponse);
		Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
		
		Map<String,Object> custResult = cust.get("Result") == null ? null :
			mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
		
		if(custResult == null) {
			String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
			response = errorMessgae;
			return response;
		}else {
			custRefNo = custResult.get("SuccessId") == null ? "" : custResult.get("SuccessId").toString();
		}
	}catch(Exception e) {
		e.printStackTrace();
		log.info(e);
	}
	
	log.info("CUSTOMER CREATION BLOCK END: "+new Date());
	
	//==============================CUSTOMER CREATION BLOCK END=============================================
	
	//==============================SHOW VEHICLE INFO BLOCK START=============================================
	log.info("SHOW VEHICLE INFO BLOCK START: "+new Date());
	Map<String,Object> vehInfo = new HashMap<String,Object>();
	vehInfo.put("BranchCode", "119");
	vehInfo.put("BrokerBranchCode", "1");
	vehInfo.put("CreatedBy", "SZL_Whatsapp");
	vehInfo.put("InsuranceId", "100049");
	vehInfo.put("ProductId", "5");
	vehInfo.put("ReqChassisNumber", "");
	vehInfo.put("ReqRegNumber", regNo);
	vehInfo.put("SavedFrom", "API");
	
	Map<String,Object> showVehResult = null;
	try {
		String showInfo = mapper.writeValueAsString(vehInfo);
		String showVehApi = showVehicleInfoApi;
		
		log.info("Show Vehicle Api Calling: "+showVehApi);
		log.info("Show Vehicle Request: "+showInfo);
		
		String apiResponse = thread.callSwazilandComApi(showVehApi, showInfo);
		
		log.info("Show Vehicle Response: "+apiResponse);
		
		Map<String,Object> showVeh = mapper.readValue(apiResponse, Map.class);
		showVehResult = showVeh.get("Result") == null ? null : (Map<String, Object>) showVeh.get("Result");
			
		if(showVehResult == null) {
			String errorMessgae = showVeh.get("ErrorMessage") == null ? "" : showVeh.get("ErrorMessage").toString();
			response = errorMessgae;
			return response;
		}
				
	}catch(Exception e) {
		e.printStackTrace();
	}
	log.info("SHOW VEHICLE INFO BLOCK END: "+new Date());
	//==============================SHOW VEHICLE INFO BLOCK END=============================================
	
	//==============================MOTOR SAVE BLOCK START=============================================
	log.info("MOTOR SAVE BLOCK START: "+new Date());
	LocalDate endDate = curDate.plusDays(364);
	String policyEndDate = endDate.format(formatter);
	
	Map<String,Object> motorSave = new HashMap<String,Object>();
	motorSave.put("AboutVehicle", null);
	motorSave.put("AcExecutiveId", null);
	motorSave.put("AcccessoriesSumInsured", null);
	motorSave.put("AccessoriesInformation", null);
	motorSave.put("AdditionalCircumstances", "");
	motorSave.put("AgencyCode", "14035");//local-13495,UAT-14035
	motorSave.put("AggregatedValue", null);
	motorSave.put("ApplicationId", "1");
	motorSave.put("AxelDistance", 1);
	motorSave.put("BankingDelegation", "");
	motorSave.put("BdmCode", "2000010");//uAt-2000010,local-5555555
	motorSave.put("BorrowerType", null);
	motorSave.put("BranchCode", "119");
	motorSave.put("BrokerBranchCode", "1");
	motorSave.put("BrokerCode", "14035");//local-13495,UAT-14035
	motorSave.put("Chassisnumber", chassisNo);
	motorSave.put("CityLimit", null);
	motorSave.put("ClaimType", "0");
	//motorSave.put("ClaimTypeDesc", null);
	motorSave.put("CollateralCompanyAddress", "");
	motorSave.put("CollateralCompanyName", "");
	motorSave.put("CollateralName", null);
	motorSave.put("CollateralYn", "N");
	motorSave.put("Color", colorId);
	motorSave.put("ColorDesc", color);
	motorSave.put("CommissionType", null);
	motorSave.put("CoverNoteNo", null);
	motorSave.put("CreatedBy", "SZL_Whatsapp");//broker id
	motorSave.put("CubicCapacity", enginecapacity);//doubt
	motorSave.put("Currency", "ZAR");
	motorSave.put("CustomerCode", "2000010");//uAt-2000010,local-5555555
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
	motorSave.put("EngineCapacity", enginecapacity);
	motorSave.put("EngineNumber", engineNo);
	motorSave.put("ExcessLimit", null);
	motorSave.put("ExchangeRate", "1.0");
	motorSave.put("FirstLossPayee", null);
	motorSave.put("FleetOwnerYn", "N");
	motorSave.put("FuelType", null);
	motorSave.put("FuelTypeDesc", "");
	motorSave.put("Gpstrackinginstalled", "N");
	motorSave.put("Grossweight", grossWeight);
	motorSave.put("HavePromoCode", "N");
	motorSave.put("HoldInsurancePolicy", "N");
	motorSave.put("HorsePower", "0");
	motorSave.put("Idnumber", idNumber);
	motorSave.put("Inflation", "");
	motorSave.put("InflationSumInsured", "");
	motorSave.put("InsuranceClass", "0");
	motorSave.put("InsuranceClassDesc", null);
	motorSave.put("InsuranceId", "100049");
	motorSave.put("Insurancetype", insuranceClassId);//103 check
	motorSave.put("InsurancetypeDesc", insuranceClass);
	motorSave.put("InsurerSettlement", "");
	motorSave.put("InterestedCompanyDetails", "");
	motorSave.put("IsFinanceEndt", null);
	motorSave.put("LoanAmount", 0);
	motorSave.put("LoanEndDate", null);
	motorSave.put("LoanStartDate", null);
//	motorSave.put("LocationId", "1");
	motorSave.put("LoginId", "SZL_Whatsapp");//login
	motorSave.put("ManufactureYear", manYear);
	motorSave.put("MarketValue", null);
	motorSave.put("Mileage", null);
	motorSave.put("MobileCode", "268");
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
//	motorSave.put("NoOfComprehensives", null);
	motorSave.put("NoOfFemale", null);
	motorSave.put("NoOfMale", null);
	motorSave.put("NoOfPassengers", null);
	motorSave.put("NoOfVehicles", "1");
	motorSave.put("NumberOfAxels", null);
	motorSave.put("NumberOfCards", null);
	motorSave.put("NumberOfCylinders", null);
	motorSave.put("Occupation", occupationId);
	motorSave.put("OrginalPolicyNo", null);
	motorSave.put("OwnerCategory", "");
	motorSave.put("PaCoverId", "0");
	//motorSave.put("PlateType", null);
	motorSave.put("PolicyEndDate", policyEndDate);
	motorSave.put("PolicyRenewalYn", "N");
	motorSave.put("PolicyStartDate", policyDate);
	motorSave.put("PolicyType", "1");
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
	motorSave.put("SavedFrom", "WEB");
	motorSave.put("SearchFromApi", false);
	motorSave.put("SeatingCapacity", seatingCapacity);
	motorSave.put("SectionId", Arrays.asList(insuranceClassId));
//	motorSave.put("SourceType", "Broker");
	motorSave.put("SourceTypeId", "Broker");
	motorSave.put("SpotFogLamp", null);
	motorSave.put("Status", "Y");
	motorSave.put("Stickerno", null);
	motorSave.put("SubUserType", "Broker");
	motorSave.put("SumInsured", sum_insured);
	motorSave.put("Tareweight", null);
	motorSave.put("TiraCoverNoteNo", null);
	motorSave.put("TppdFreeLimit", null);
	motorSave.put("TppdIncreaeLimit", extended_tppd_si);
	motorSave.put("TrailerDetails", null);
	motorSave.put("TransportHydro", null);
	motorSave.put("UsageId", "");
	motorSave.put("UserType", "Broker");
	motorSave.put("Vehcilemodel", model);
	motorSave.put("VehcilemodelId", modelId);
	motorSave.put("VehicleId", 1);
	motorSave.put("VehicleType", bodyType);
	motorSave.put("VehicleTypeId", bodyTypeId);
	motorSave.put("VehicleTypeIvr", "90");
	motorSave.put("VehicleValueType", "");
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
	exchangeRateScenario.put("OldCurrency", "ZAR");
	exchangeRateScenario.put("OldExchangeRate", "1.0");
	exchangeRateScenario.put("OldSumInsured", 0);
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
		
		String apiResponse = thread.callSwazilandComApi(saveMotorApi,motorSaveReq);
		
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
	calc.put("AgencyCode", "14035");//local-13506,UAT-14035
	calc.put("BranchCode", "119");
	calc.put("CdRefNo", motorSaveResult.get("CdRefNo") == null ? "" : motorSaveResult.get("CdRefNo"));
	calc.put("CoverModification", "N");
	calc.put("CreatedBy", "SZL_Whatsapp");
	calc.put("DdRefNo", motorSaveResult.get("DdRefNo") == null ? "" : motorSaveResult.get("DdRefNo"));
	calc.put("EffectiveDate", policyDate);
	calc.put("InsuranceId", "100049");
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
		String apiResponse = thread.callSwazilandComApi(calculatorApi, calReq);
		
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
	
	List<Map<String,Object>> tax = (List<Map<String, Object>>) coverList.get(0).get("Taxes");
	
	BigDecimal pre = coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
			.map(m -> new BigDecimal(m.get("PremiumExcluedTaxLC").toString())).reduce(BigDecimal.ZERO, (x,y) -> x.add(y));
	
	List<Map<String,Object>> taxList = coverList.parallelStream().filter(p -> ("B".equalsIgnoreCase(p.get("CoverageType").toString()) 
			|| "D".equalsIgnoreCase(p.get("isSelected").toString())) && p.get("Taxes") != null)
			.map(m -> (List<Map<String,Object>>) m.get("Taxes")).flatMap(f -> f.stream()).collect(Collectors.toList());
	
	vatTax = taxList.stream().map(t -> t.get("TaxAmount") == null ? 0L : Double.valueOf(t.get("TaxAmount").toString()).longValue())
			.reduce(0L, (a,b) -> a + b);
	
	vatPercentage = tax.get(0).get("TaxRate")==null?0L:Double.valueOf(tax.get(0).get("TaxRate").toString());
	
	premium = pre.longValue();
	
	Long totalPremium =pre.longValue()+vatTax.longValue();
			
	log.info("CALC BLOCK END: "+new Date());
	
	//==============================CALC BLOCK END=============================================================
	
	//==============================USER CREATION BLOCK START=============================================
	
	log.info("USER CREATION BLOCK START: "+new Date());
	
	Map<String,Object> userCreationMap = new HashMap<String,Object>();
	userCreationMap.put("CompanyId", "100049");
	userCreationMap.put("CustomerId", custRefNo);
	userCreationMap.put("ProductId", "5");
	userCreationMap.put("ReferenceNo", reqRefNo);
	userCreationMap.put("UserMobileNo", mobileNo);
	userCreationMap.put("UserMobileCode", "268");
	userCreationMap.put("AgencyCode", "14035"); //local-13506,UAT-14035
	
	Map<String,Object> userResult = null;
	Map<String,Object> userRes = null;
	try {
		String userCreationReq = mapper.writeValueAsString(userCreationMap);
		String userCreationApi = loginCreationApi;
		
		log.info("USER CREATION API: "+userCreationApi);
		
		String apiResponse = thread.callSwazilandComApi(userCreationApi, userCreationReq);
		
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
	buypolicyMap.put("CreatedBy", "SZL_Whatsapp");
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
		
		String apiResponse = thread.callSwazilandComApi(buyPolicyApi, buypolicyReq);
		
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
	
	log.info("MAKE PAYMENT BLOCK START : "+new Date());
	Map<String,Object> makePaymentMap = new HashMap<String,Object>();
	makePaymentMap.put("CreatedBy", "SZL_Whatsapp");
	makePaymentMap.put("EmiYn", "N");
	makePaymentMap.put("InstallmentMonth", null);
	makePaymentMap.put("InstallmentPeriod", null);
	makePaymentMap.put("InsuranceId", "100049");
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
		String apiResponse = thread.callSwazilandComApi(makePaymentApi, makePayemantReq);
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
		String apiResponse = thread.callSwazilandComApi(insertPaymentApi, insertPaymentReq);
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
    
  //==============================PAYMENT LINK BLOCK END=============================================
    
  //==============================WHATSAPP RESPONSE BLOCK START=============================================
    log.info("WHATSAPP RESPONSE BLOCK START : "+new Date());
    
    Map<String,Object> getMotorReq = new HashMap<String, Object>();
	getMotorReq.put("RequestReferenceNo", reqRefNo);
	
	String api_request =mapper.writeValueAsString(getMotorReq);
	String motorDetailsApi = getAllMotorDetailsApi;
	String apiResponse = thread.callSwazilandComApi(motorDetailsApi, api_request);
	
	Map<String,Object> getMotorRes =mapper.readValue(apiResponse, Map.class);
	
	List<Map<String,Object>> motorRes =getMotorRes.get("Result")==null?null:
		mapper.readValue(mapper.writeValueAsString(getMotorRes.get("Result")), List.class);
	
	log.info("ALL MOTOR DETAILS :" +motorRes);
	
	Map<String,Object> mot = motorRes.get(0);
	
	botResponceData.put("registration", mot.get("Registrationnumber")==null?"N/A":mot.get("Registrationnumber"));
	botResponceData.put("usage", mot.get("MotorUsageDesc")==null?"N/A":mot.get("MotorUsageDesc"));
	botResponceData.put("vehtype", mot.get("VehicleTypeDesc")==null?"N/A":mot.get("VehicleTypeDesc"));
	botResponceData.put("color",mot.get("ColorDesc")==null?"N/A":mot.get("ColorDesc"));
	//botResponceData.put("insurance_class",insuredClass);
	botResponceData.put("premium", premium);
	botResponceData.put("url", paymentUrl);
	botResponceData.put("vatamt", vatTax);
	botResponceData.put("suminsured", mot.get("SumInsured")==null?"N/A":mot.get("SumInsured"));
	botResponceData.put("chassis", mot.get("Chassisnumber")==null?"N/A":mot.get("Chassisnumber"));
	botResponceData.put("vat", String.valueOf(vatPercentage.longValue()));
	botResponceData.put("totalpremium", totalPremium);
	botResponceData.put("inceptiondate", policyDate);
	botResponceData.put("expirydate",policyEndDate);
	botResponceData.put("referenceno", reqRefNo);
	botResponceData.put("veh_model_desc", mot.get("VehcilemodelDesc")==null?"N/A":mot.get("VehcilemodelDesc"));
	botResponceData.put("veh_make_desc", mot.get("VehiclemakeDesc")==null?"N/A":mot.get("VehiclemakeDesc"));
	botResponceData.put("customer_name", customerName);
	
	
	return botResponceData;

}

    // Optional: Restart session manually
  /*  public void restartSession(String waId) {
        sessionMap.remove(waId);
        sendWhatsappMessage(waId, "🔄 Your form has been restarted.\n" + questionTexts.get(0));
        sessionMap.put(waId, new UserSession());
    }*/

	}

