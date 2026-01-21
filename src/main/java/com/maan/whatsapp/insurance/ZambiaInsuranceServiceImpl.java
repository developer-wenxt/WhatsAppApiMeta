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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.maan.whatsapp.response.error.Error;

@Service
@PropertySource("classpath:WebServiceUrl.properties")
public class ZambiaInsuranceServiceImpl implements ZambiaInsuranceService {

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
	
	@Value("${wh.phoenix.view.calc.api}")
	private String viewCalcApi;

	@Value("${askeva.template.api}")
	private String askeveApi;
	
	@Value("${askeva.template.api.zambia}")
	private String askeveApiforZambia;
	
	@Value("${wh.phoenix.getcustomer.api}")
	private String getCustomerApi;

	// @Override
	public Object generateZambiaQuote(Object req,String insClass,String chatNum)
			throws JsonProcessingException, JsonMappingException, WhatsAppValidationException {

		String response = "";
		String exception = "";
		List<Error> errorList = new ArrayList<>(2);
		Map<String,Object> botResponceData = new HashMap<String,Object>();
		Map<String,Object> data = mapper.convertValue(req, Map.class);
	//	String flowDatas = new String(Base64.getDecoder().decode(req.getQuote_form()));
		
	//	Map<String,Object> requestDatas = mapper.readValue(flowDatas, Map.class);
		
		String cus_name = data.get("cus_name") == null ? "" : data.get("cus_name").toString();
		String mob_no = data.get("mob_no") == null ? "" : data.get("mob_no").toString();
		String motor_usage = data.get("motor_usage") == null ? "" : data.get("motor_usage").toString();
		String reg_no = data.get("reg_no") == null ? "" : data.get("reg_no").toString();
		String id_type = data.get("id_type") == null ? "" : data.get("id_type").toString();
		String id_num = data.get("id_num") == null ? "" : data.get("id_num").toString();
		String ins_period = data.get("ins_period") == null ? "" : data.get("ins_period").toString();
		String insuranceClass = insClass;
		String sum_insured = data.get("sum_insured") == null ? null : data.get("sum_insured").toString();
		
		String idType = "";
		if(StringUtils.isNotBlank(id_type)) {
			if(id_type.equals("1")) {
				idType = "Driver License";
			}else if(id_type.equals("2")) {
				idType = "NUIT Number";
			}else if(id_type.equals("3")) {
				idType = "Passport Number";
			}else if(id_type.equals("4")) {
				idType = "National Id";
			}
		}
		
		String inusredPeriod = "";
		if(StringUtils.isNotBlank(ins_period)) {
			if(ins_period.equals("1")) {
				inusredPeriod = "30";
			}else if(ins_period.equals("2")) {
				inusredPeriod = "90";
			}else if(ins_period.equals("3")) {
				inusredPeriod = "180";
			}else if(ins_period.equals("4")) {
				inusredPeriod = "270";
			}else if(ins_period.equals("5")) {
				inusredPeriod = "365";
			}
		}
		String motorUsage = "";
		if(StringUtils.isNotBlank(motor_usage)) {
			if(motor_usage.equals("1")) {
				motorUsage = "Commercial";
			}else if(motor_usage.equals("2")) {
				motorUsage = "Motorcycles";
			}else if(motor_usage.equals("3")) {
				motorUsage = "Private";
			}else if(motor_usage.equals("4")) {
				motorUsage = "Public Transport Vehicle";
			}
		}
		
				//DOB calc
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
				LocalDate curDate = LocalDate.now();
				String policyDate = curDate.format(formatter);
			//	LocalDate curDate = LocalDate.parse(policyDate,formatter);
				LocalDate minusDate = curDate.minusYears(18);
				String cusDob = minusDate.format(formatter);
				
				//==============================SHOW VEHICLE INFO BLOCK START=============================================
				log.info("SHOW VEHICLE INFO BLOCK START: "+new Date());
				Map<String,Object> vehInfo = new HashMap<String,Object>();
				vehInfo.put("BranchCode", "126");
				vehInfo.put("BrokerBranchCode", "1");
				vehInfo.put("CreatedBy", "Zambia_whatsapp");
				vehInfo.put("InsuranceId", "100046");
				vehInfo.put("ProductId", "5");
				vehInfo.put("ReqChassisNumber", "");
				vehInfo.put("ReqRegNumber", reg_no);
				vehInfo.put("SavedFrom", "API");
				
				Map<String,Object> showVehResult = null;
				try {
					String showInfo = mapper.writeValueAsString(vehInfo);
					String showVehApi = showVehicleInfoApi;
					log.info("Show Vehicle Request: "+showInfo);
					log.info("Show Vehicle Api Calling: "+showVehApi);
					
					String apiResponse = thread.callZambiaComApi(showVehApi, showInfo);
					
					log.info("Show Vehicle Response: "+apiResponse);
					
					Map<String,Object> showVeh = mapper.readValue(apiResponse, Map.class);
					showVehResult = showVeh.get("Result") == null ? null : (Map<String, Object>) showVeh.get("Result");
						
					if(showVehResult == null) {
						String errorMessgae = showVeh.get("ErrorMessage") == null ? "" : showVeh.get("ErrorMessage").toString();
						response = errorMessgae;
						callVehicleNotinListTemplate(cus_name,reg_no,chatNum);
						return response;
					}
							
				}catch(Exception e) {
					e.printStackTrace();
				}
				log.info("SHOW VEHICLE INFO BLOCK END: "+new Date());
				//==============================SHOW VEHICLE INFO BLOCK END=============================================
				
				
				//ID's Calling
				Map<String,Object> idTypeMap = new HashMap<>();
				idTypeMap.put("Desc", idType);
				idTypeMap.put("MasterType", "CUSTOMER_IDTYPE");
				idTypeMap.put("InsuranceId", "100046");
				String idTypeId = "";
				try {
					String idTypeReq = mapper.writeValueAsString(idTypeMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callZambiaComApi(masterApi, idTypeReq);
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
				String insuranceClassId="";
				Map<String,Object> insuranceClassMap = new HashMap<>();
				insuranceClassMap.put("Desc", insuranceClass);
				insuranceClassMap.put("MasterType", "INSURANCE_CLASS");
				insuranceClassMap.put("InsuranceId", "100046");
				
				try {
					String insuranceClassReq = mapper.writeValueAsString(insuranceClassMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callZambiaComApi(masterApi, insuranceClassReq);
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
				String motorUsageId="";
				Map<String,Object> motorUsageMap = new HashMap<>();
				motorUsageMap.put("Desc", motorUsage);
				motorUsageMap.put("MasterType", "MOTOR_USAGE");
				motorUsageMap.put("InsuranceId", "100046");
				
				try {
					String motorUsageReq = mapper.writeValueAsString(motorUsageMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callZambiaComApi(masterApi, motorUsageReq);
					log.info("Master Api Response: "+apiResponse);

					Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
					
					if(masterApiResult.isEmpty() || masterApiResult == null) {
						String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
						response = errorMessage;
						return response;
					}else {
						motorUsageId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				String bodyTypeId="";
				String bodyType = showVehResult.get("VehicleType") == null ? "" : showVehResult.get("VehicleType").toString();
				if(StringUtils.isNotBlank(bodyType)) {
					
					Map<String,Object> bodyTypeMap = new HashMap<>();
					motorUsageMap.put("Desc", bodyType);
					motorUsageMap.put("MasterType", "BODY_TYPE");
					motorUsageMap.put("InsuranceId", "100046");
					
					try {
						String bodyTypeReq = mapper.writeValueAsString(motorUsageMap);
						String masterApi = masterIdsApi;
						log.info("Master Ids Api Calling: "+masterApi);
						
						String apiResponse = thread.callZambiaComApi(masterApi, bodyTypeReq);
						log.info("Master Api Response: "+apiResponse);

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
						log.info(e);
					}
				}
				
				//==============================CUSTOMER CREATION BLOCK START=============================================
				log.info("CUSTOMER CREATION BLOCK START: "+new Date());
				Map<String,Object> customerCreation = new HashMap<String,Object>();
				customerCreation.put("Activities", "");
				customerCreation.put("Address1", "");
				customerCreation.put("Address2", "");
				customerCreation.put("AppointmentDate", "");
				customerCreation.put("BranchCode", "126");
				customerCreation.put("BrokerBranchCode", "1");
				customerCreation.put("BusinessType", null);
				customerCreation.put("CityCode", "1");
				customerCreation.put("CityName", "Chibombo");//check district or region
				customerCreation.put("ClientName", cus_name);
				customerCreation.put("Clientstatus", "Y");
				customerCreation.put("Country", "ZMB");
				customerCreation.put("CountryName", "Zambia");
				customerCreation.put("CreatedBy", "Zambia_whatsapp");//create login for whatsapp bot
				customerCreation.put("CustomerAsInsurer", "N");
				customerCreation.put("CustomerReferenceNo", "");
				customerCreation.put("DobOrRegDate", cusDob);
				customerCreation.put("Email1", "");
				customerCreation.put("Email2", null);
				customerCreation.put("Email3", null);
				customerCreation.put("ExpiryDate", null);
				customerCreation.put("Fax", null);
				customerCreation.put("Gender", "M");
				customerCreation.put("IdNumber", id_num);
				customerCreation.put("IdType", idTypeId);
				customerCreation.put("InsuranceId", "100046");
				customerCreation.put("IsTaxExempted", "N");
				customerCreation.put("Language", "1");
			//	customerCreation.put("LastName", "");
			//	customerCreation.put("MaritalStatus", "Single");//check
			//	customerCreation.put("MiddleName", "");
				customerCreation.put("MobileCode1", "260");
				customerCreation.put("MobileCodeDesc1", "1");
				customerCreation.put("MobileNo1", mob_no);
			//	customerCreation.put("MobileNo2", "");
				customerCreation.put("MobileNo3", null);
				customerCreation.put("Nationality", "");
				customerCreation.put("Occupation", "9");
				customerCreation.put("OtherOccupation", "");
				customerCreation.put("PhoneNoCode", "260");
				customerCreation.put("PinCode", "");
				customerCreation.put("Placeofbirth", "chennai");
				customerCreation.put("PolicyHolderType", "1");
				customerCreation.put("PolicyHolderTypeid", idTypeId);
				customerCreation.put("PreferredNotification", "sms");
				customerCreation.put("ProductId", "5");
				customerCreation.put("RegionCode", "10000");
				customerCreation.put("RiskAssessmentDate", null);
				customerCreation.put("SaveOrSubmit", "Save");
				customerCreation.put("SocioProfessionalCategory", null);
				customerCreation.put("StateCode", "10000");
				customerCreation.put("StateName", null);
				customerCreation.put("Status", "Y");
				customerCreation.put("Street", "");
				customerCreation.put("TaxExemptedId", null);
				customerCreation.put("TelephoneNo1", "");
				customerCreation.put("TelephoneNo2", null);
				customerCreation.put("TelephoneNo3", null);
				customerCreation.put("Title", "1");
				customerCreation.put("Type", null);
				customerCreation.put("VipFlag", null);
				customerCreation.put("VrTinNo", null);
			//	customerCreation.put("WhatsappCode", "260");
				customerCreation.put("WhatsappDesc", "1");
			//	customerCreation.put("WhatsappNo", mobileNo);
				customerCreation.put("Zone", "1");
				
				String custRefNo = "";
				
				try {
					String cusReq = mapper.writeValueAsString(customerCreation);
					String cusSaveApi = saveCustomerApi;
					log.info("Customer Save Request: "+cusReq);
					log.info("Customer Save Calling: "+cusSaveApi);
					
					String apiResponse = thread.callZambiaComApi(cusSaveApi,cusReq);
					
					log.info("Customer Save Response: "+apiResponse);
					Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> custResult = cust.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
					
				    custRefNo = custResult.get("SuccessId") == null ? "" : custResult.get("SuccessId").toString();
					
					if(custResult == null) {
						String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				log.info("CUSTOMER CREATION BLOCK END: "+new Date());
				
				//==============================CUSTOMER CREATION BLOCK END=============================================
				
				
		/*	
				//==============================SAVE VEHICLE INFO BLOCK START=============================================
				log.info("SAVE VEHICLE INFO START: "+new Date());
				
				Map<String,Object> vehicleInfo = new HashMap<String,Object>();
				vehicleInfo.put("Insuranceid", "100046");
				vehicleInfo.put("BranchCode", "136");
				//vehicleInfo.put("AxelDistance", 1);
				vehicleInfo.put("Chassisnumber", chassisNo);
				vehicleInfo.put("Color", colorId);
				vehicleInfo.put("CreatedBy", "");
				vehicleInfo.put("DisplacementInCM3", "0");
				vehicleInfo.put("EngineNumber", engineNo);
				//vehicleInfo.put("FuelType", null);
				vehicleInfo.put("Grossweight", grossWeight);
				vehicleInfo.put("ManufactureYear", manYear);
				//vehicleInfo.put("MotorCategory", null);
				vehicleInfo.put("Motorusage", motorUsage);
				vehicleInfo.put("NumberOfAxels", null);
				vehicleInfo.put("OwnerCategory", "1");
				vehicleInfo.put("Registrationnumber", regNo);
				vehicleInfo.put("ResEngineCapacity", enginecapacity);
				vehicleInfo.put("ResOwnerName", customerName);
				vehicleInfo.put("ResStatusCode", "Y");
				vehicleInfo.put("ResStatusDesc", "None");
				vehicleInfo.put("SeatingCapacity", seatingCapacity);
				vehicleInfo.put("HorsePower", "0");
				vehicleInfo.put("Tareweight", null);
				vehicleInfo.put("Vehcilemodel", model);
				vehicleInfo.put("VehcilemodelId", modelId);
				vehicleInfo.put("VehicleType", bodyType);
				vehicleInfo.put("Vehiclemake", make);
				vehicleInfo.put("VehiclemakeId", makeId);
				//vehicleInfo.put("DisplacementInCM3", null);
				vehicleInfo.put("NumberOfCylinders", 0);
				//vehicleInfo.put("PlateType", null);
				
				try {
					String saveVehicleInfo = saveVehicleInfoApi;
					log.info("Save Vehicle Info Calling: "+saveVehicleInfo);
					String token = thread.getNamibiaToken();
					String vehResponse = thread.callPhoenixApi(saveVehicleInfo, mapper.writeValueAsString(vehicleInfo), token);
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
				
				//==============================MOTOR SAVE BLOCK START=============================================
				log.info("MOTOR SAVE BLOCK START: "+new Date());
				LocalDate endDate = curDate.plusDays(Long.valueOf(inusredPeriod));
				String policyEndDate = endDate.format(formatter);
				
				Map<String,Object> motorSave = new HashMap<String,Object>();
				motorSave.put("AboutVehicle", null);
				motorSave.put("AcExecutiveId", null);
				motorSave.put("AcccessoriesSumInsured", null);
				motorSave.put("AccessoriesInformation", "");
				motorSave.put("AdditionalCircumstances", "");
				motorSave.put("AgencyCode", "13928");//local-13300,live-13928
				motorSave.put("AggregatedValue", null);
				motorSave.put("ApplicationId", "1");
				motorSave.put("AxelDistance", null);
				motorSave.put("BankingDelegation", "");
				motorSave.put("BdmCode", "2000050");//local-546544543,local-2000050
				motorSave.put("BorrowerType", null);
				motorSave.put("BranchCode", "126");
				motorSave.put("BrokerBranchCode", "1");
				motorSave.put("BrokerCode", "13928");//local-13300,live-13928
				motorSave.put("Chassisnumber", showVehResult.get("ReqChassisNumber") == null ? "" : showVehResult.get("ReqChassisNumber"));
				motorSave.put("CityLimit", null);
				motorSave.put("ClaimType", "0");
			//	motorSave.put("ClaimTypeDesc", noClaimBonus);
				motorSave.put("CollateralCompanyAddress", "");
				motorSave.put("CollateralCompanyName", "");
				motorSave.put("CollateralName", null);
				motorSave.put("CollateralYn", "N");
				motorSave.put("Color", showVehResult.get("Color") == null ? "" : showVehResult.get("Color"));
				motorSave.put("ColorDesc", showVehResult.get("Color") == null ? "" : showVehResult.get("Color"));
				motorSave.put("CommissionType", null);
				motorSave.put("CoverNoteNo", null);
				motorSave.put("CreatedBy", "Zambia_whatsapp");
				motorSave.put("CubicCapacity", showVehResult.get("ResEngineCapacity") == null ? "" : showVehResult.get("ResEngineCapacity"));
				motorSave.put("Currency", "ZMW");
				motorSave.put("CustomerCode", "2000050");//local-546544543,local-2000050
				motorSave.put("CustomerName", cus_name);
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
				motorSave.put("EndorsementYn", null);
				motorSave.put("EndtCategoryDesc", null);
				motorSave.put("EndtCount", null);
				motorSave.put("EndtPrevPolicyNo", null);
				motorSave.put("EndtPrevQuoteNo", null);
				motorSave.put("EndtStatus", null);
				motorSave.put("EngineCapacity", showVehResult.get("ResEngineCapacity") == null ? "" : showVehResult.get("ResEngineCapacity"));
				motorSave.put("EngineNumber",  showVehResult.get("EngineNumber") == null ? "" : showVehResult.get("EngineNumber"));
				motorSave.put("ExcessLimit", null);
				motorSave.put("ExchangeRate", "1.0");
				motorSave.put("FirstLossPayee", null);
				motorSave.put("FleetOwnerYn", "N");
				motorSave.put("FuelType", null);
				motorSave.put("FuelTypeDesc", null);
				motorSave.put("Gpstrackinginstalled", "N");
				motorSave.put("Grossweight", showVehResult.get("Grossweight") == null ? "" : showVehResult.get("Grossweight"));
				motorSave.put("HavePromoCode", "N");
				motorSave.put("HoldInsurancePolicy", "N");
				motorSave.put("HorsePower", "");
				motorSave.put("Idnumber", id_num);
				motorSave.put("Inflation", "");
				motorSave.put("InflationSumInsured", "");
				motorSave.put("InsuranceClass", insuranceClassId);
				motorSave.put("InsuranceClassDesc", insuranceClass);
				motorSave.put("InsuranceId", "100046");
				motorSave.put("Insurancetype", insuranceClassId);//103 check
				motorSave.put("InsurancetypeDesc", insuranceClass);
				motorSave.put("InsurerSettlement", "");
				motorSave.put("InterestedCompanyDetails", "");
				motorSave.put("IsFinanceEndt", null);
				motorSave.put("LoanAmount", 0);
				motorSave.put("LoanEndDate", null);
				motorSave.put("LoanStartDate", null);
			//	motorSave.put("LocationId", "1");
				motorSave.put("LoginId", "Zambia_whatsapp");//login
				motorSave.put("ManufactureYear", showVehResult.get("ManufactureYear") == null ? "" : showVehResult.get("ManufactureYear"));
				motorSave.put("MarketValue", null);
				motorSave.put("Mileage", null);
				motorSave.put("MobileCode", "260");
				motorSave.put("MobileNumber", mob_no);
				motorSave.put("ModelNumber", null);
				motorSave.put("MotorCategory", showVehResult.get("MotorCategory") == null ? "" : showVehResult.get("MotorCategory"));
				motorSave.put("Motorusage", motorUsage);//showVehResult.get("Motorusage") == null ? "" : showVehResult.get("Motorusage")
				motorSave.put("MotorusageId", motorUsageId);
				motorSave.put("MunicipalityTraffic", null);
				motorSave.put("Ncb", "0");
				motorSave.put("NcdYn", "N");
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
			//	motorSave.put("NumberOfCylinders", null);
				motorSave.put("Occupation", "9");
				motorSave.put("OrginalPolicyNo", null);
				motorSave.put("OwnerCategory", null);
				motorSave.put("PaCoverId", "0");
			//	motorSave.put("PlateType", null);
				motorSave.put("PolicyEndDate", policyEndDate); 
				motorSave.put("PolicyRenewalYn", "N");
				motorSave.put("PolicyStartDate", policyDate);
				motorSave.put("PolicyType", "1");
				motorSave.put("PreviousInsuranceYN", "N");
				motorSave.put("PreviousLossRatio", "");
				motorSave.put("ProductId", "5");
				motorSave.put("PromoCode", null);
				motorSave.put("PurchaseDate", null);
				motorSave.put("QuoteExpiryDays", "90");
				motorSave.put("RadioOrCasseteplayer", null);
				motorSave.put("RegistrationDate", null);
				motorSave.put("RegistrationYear", cusDob);//check
				motorSave.put("Registrationnumber", showVehResult.get("Registrationnumber") == null ? "" : showVehResult.get("Registrationnumber"));
				motorSave.put("RequestReferenceNo", "");
				motorSave.put("RoofRack", null);
			//	motorSave.put("SaveOrSubmit", "Save");
				motorSave.put("SavedFrom", "Web");
				motorSave.put("SearchFromApi", true);
				motorSave.put("SeatingCapacity", showVehResult.get("SeatingCapacity") == null ? "" : showVehResult.get("SeatingCapacity"));
				motorSave.put("SectionId", Arrays.asList(insuranceClassId));
			//	motorSave.put("SourceType", null);
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
				motorSave.put("Vehcilemodel", showVehResult.get("Vehcilemodel") == null ? "" : showVehResult.get("Vehcilemodel"));
				motorSave.put("VehcilemodelId", showVehResult.get("VehcilemodelId") == null ? "" : showVehResult.get("VehcilemodelId"));
				motorSave.put("VehicleId", 1);
				motorSave.put("VehicleType", showVehResult.get("VehicleType") == null ? "" : showVehResult.get("VehicleType"));
				motorSave.put("VehicleTypeId", bodyTypeId);
				motorSave.put("VehicleTypeIvr", "");
				motorSave.put("VehicleValueType", "");
				motorSave.put("Vehiclemake", showVehResult.get("Vehiclemake") == null ? "" : showVehResult.get("Vehiclemake"));
				motorSave.put("VehiclemakeId", showVehResult.get("VehiclemakeId") == null ? "" : showVehResult.get("VehiclemakeId"));
				motorSave.put("WindScreenSumInsured", null);
				motorSave.put("Windscreencoverrequired", null);
				motorSave.put("Zone", "1");
				motorSave.put("ZoneCirculation", null);
				motorSave.put("accident", null);
				motorSave.put("periodOfInsurance", inusredPeriod);
				LinkedHashMap<String, Object> exchangeRateScenario = new  LinkedHashMap<>();
				exchangeRateScenario.put("OldAcccessoriesSumInsured", null);
				exchangeRateScenario.put("OldCurrency", "ZMW");
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
					log.info("Save Motor Request: "+motorSaveReq);
					log.info("Save Motor Api Calling: "+saveMotorApi);
					
					String apiResponse = thread.callZambiaComApi(saveMotorApi,motorSaveReq);
					
					log.info("Save Motor Response: "+apiResponse);
					
					Map<String,Object> saveMot = mapper.readValue(apiResponse, Map.class);
					saveMotResult = saveMot.get("Result") == null ? null : (List<Map<String, Object>>) saveMot.get("Result");
					
				//	reqRefNo = saveMotResult.get("RequestReferenceNo") == null ? "" : saveMotResult.get("RequestReferenceNo").toString();
						
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
				calc.put("AgencyCode", "13928");//local-13300,live-13928
				calc.put("BranchCode", "126");
				calc.put("CdRefNo", motorSaveResult.get("CdRefNo") == null ? "" : motorSaveResult.get("CdRefNo"));
				calc.put("CoverModification", "N");
				calc.put("CreatedBy", "Zambia_whatsapp");
				calc.put("DdRefNo", motorSaveResult.get("DdRefNo") == null ? "" : motorSaveResult.get("DdRefNo"));
				calc.put("EffectiveDate", policyDate);
				calc.put("InsuranceId", "100046");
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
				List<Map<String,Object>> referral =null;
				Map<String,Object> calcRes=null;
				Map<String,Object> calcResult = null;
				try {
					String calReq = mapper.writeValueAsString(calc);
					log.info("calc Request: "+calReq);
					String calculatorApi = calcApi;
					log.info("calc API: "+calculatorApi);
					String apiResponse = thread.callZambiaComApi(calculatorApi, calReq);
					
					log.info("Calc Response: "+apiResponse);
					
					 calcRes = mapper.readValue(apiResponse, Map.class);
					 coverList = calcRes.get("CoverList") == null ? null :
						 mapper.readValue(mapper.writeValueAsString(calcRes.get("CoverList")), List.class);
					 
					 coverList = coverList.stream().filter(cov -> (cov.get("CoverId").equals("279")) || 
							 (cov.get("CoverId").equals("505") || (cov.get("CoverId").equals("504"))) ).toList();
					 
					 referral = calcRes.get("MasterReferral") == null ? null :
						 mapper.readValue(mapper.writeValueAsString(calcRes.get("MasterReferral")), List.class);
					 
				//	calcResult = calcRes.get("Result") == null ? null : (Map<String, Object>) calcRes.get("Result");
					
					if(coverList == null) {
						String errorMessgae = calcRes.get("ErrorMessage") == null ? "" : calcRes.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				
				//referral Block
				if(!referral.isEmpty()) {
					callReferralTemplate(cus_name,reqRefNo,chatNum);
					
					return "Your Quotation "+reqRefNo+" has been Referral";
				}
				
				
				
				Long premium=0L;
			//	Long vatTax =0L;
				Double vatPercentage=0D;
				
				List<Map<String,Object>> tax = (List<Map<String, Object>>) coverList.get(0).get("Taxes");
				
				
				
				BigDecimal pre = coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
						.map(m -> new BigDecimal(m.get("PremiumExcluedTaxLC").toString())).reduce(BigDecimal.ZERO, (x,y) -> x.add(y));
				
				List<Map<String,Object>> taxList = coverList.parallelStream().filter(p -> ("B".equalsIgnoreCase(p.get("CoverageType").toString()) 
						|| "D".equalsIgnoreCase(p.get("isSelected").toString())) && p.get("Taxes") != null)
						.map(m -> (List<Map<String,Object>>) m.get("Taxes")).flatMap(f -> f.stream()).collect(Collectors.toList());
				
				BigDecimal vatTax = taxList.stream().map(t -> t.get("TaxAmount") == null ? BigDecimal.ZERO : new BigDecimal(t.get("TaxAmount").toString()))
						.reduce(BigDecimal.ZERO, BigDecimal::add);
				
				vatPercentage = tax.get(0).get("TaxRate")==null?0L:Double.valueOf(tax.get(0).get("TaxRate").toString());
				
				premium = pre.longValue();
				
			//	Long totalPremium =pre.longValue()+vatTax.longValue();
				
				BigDecimal totPremium = pre.add(vatTax);
				
				String totalPremium = String.valueOf(totPremium);
						
				log.info("CALC BLOCK END: "+new Date());
				
				//==============================CALC BLOCK END=============================================================
				
				//==============================USER CREATION BLOCK START=============================================
				
				log.info("USER CREATION BLOCK START: "+new Date());
				
				Map<String,Object> userCreationMap = new HashMap<String,Object>();
				userCreationMap.put("CompanyId", "100046");
				userCreationMap.put("CustomerId", custRefNo);
				userCreationMap.put("ProductId", "5");
				userCreationMap.put("ReferenceNo", reqRefNo);
				userCreationMap.put("UserMobileNo", mob_no);
				userCreationMap.put("UserMobileCode", "260");
				userCreationMap.put("AgencyCode", "13928");//local-13300,live-13928
				
				Map<String,Object> userResult = null;
				Map<String,Object> userRes = null;
				try {
					String userCreationReq = mapper.writeValueAsString(userCreationMap);
					String userCreationApi = loginCreationApi;
					
					log.info("USER CREATION API: "+userCreationApi);
					
					String apiResponse = thread.callZambiaComApi(userCreationApi, userCreationReq);
					
					log.info("USER CREATION RESPONSE: "+apiResponse);
					
					 userRes = mapper.readValue(apiResponse, Map.class);
					
					userResult = userRes.get("Result") == null ? null : (Map<String, Object>) userRes.get("Result");
					
				//	if(userResult == null) {
				//		String errorMessgae = calcRes.get("ErrorMessage") == null ? "" : calcRes.get("ErrorMessage").toString();
				//		response = errorMessgae;
				//		return response;
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
				buypolicyMap.put("CreatedBy", "Zambia_whatsapp");
				buypolicyMap.put("ProductId", "5");
				buypolicyMap.put("ManualReferralYn", "N");
				buypolicyMap.put("Vehicles", vehiMapList);
				
				Map<String,Object> buyPolicyResult = null;
				Map<String,Object> buyPolicyRes = null;
				try {
					String buypolicyReq =objectPrint.toJson(buypolicyMap);
					String buyPolicyApi = buyPolicy;
					log.info("BUY POLICY Request: "+buypolicyReq);
					log.info("BUY POLICY API: "+buyPolicyApi);
					
					String apiResponse = thread.callZambiaComApi(buyPolicyApi, buypolicyReq);
					
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
				makePaymentMap.put("CreatedBy", "Zambia_whatsapp");
				makePaymentMap.put("EmiYn", "N");
				makePaymentMap.put("InstallmentMonth", null);
				makePaymentMap.put("InstallmentPeriod", null);
				makePaymentMap.put("InsuranceId", "100046");
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
					log.info("MAKE PAYMENT Request: "+makePayemantReq);
					log.info("MAKE PAYMENT API: "+makePaymentApi);
					String apiResponse = thread.callZambiaComApi(makePaymentApi, makePayemantReq);
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
				insertPaymentMap.put("CreatedBy", "Zambia_whatsapp");
				insertPaymentMap.put("EmiYn", "N");
				insertPaymentMap.put("IbanNumber", null);
				insertPaymentMap.put("InsuranceId", "100046");
				insertPaymentMap.put("MICRNo", null);
				insertPaymentMap.put("MobileCode1", null);
				insertPaymentMap.put("MobileNo1", null);
				insertPaymentMap.put("PayeeName", cus_name);
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
					log.info("INSERT PAYMENT Request: "+insertPaymentReq);
					log.info("INSERT PAYMENT API: "+insertPaymentApi);
					String apiResponse = thread.callZambiaComApi(insertPaymentApi, insertPaymentReq);
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
			    paymentMap.put("CompanyId", "100046");
			    paymentMap.put("WhatsappCode", "260");
			    paymentMap.put("WhtsappNo", mob_no);
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
				String apiResponse = thread.callZambiaComApi(motorDetailsApi, api_request);
				
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
				botResponceData.put("premium", pre);
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
				botResponceData.put("customer_name", cus_name);
				
				
				return botResponceData;
			
	}

	@Override
	public Object zambiaQuote(Object req)
			throws JsonMappingException, JsonProcessingException, WhatsAppValidationException {
		String response = "";
		String exception = "";
		List<Error> errorList = new ArrayList<>(2);
		Map<String, Object> botResponceData = new HashMap<String, Object>();

		// String flowDatas = new String(Base64.getDecoder().decode(req.toString()));

		Map<String, Object> requestDatas = mapper.convertValue(req, Map.class);

		// Customer page Datas
		String title = requestDatas.get("title") == null ? "" : requestDatas.get("title").toString();
		String customerName = requestDatas.get("customer_name") == null ? ""
				: requestDatas.get("customer_name").toString();
		String gender = requestDatas.get("gender") == null ? "" : requestDatas.get("gender").toString();
		String occupation = requestDatas.get("occupation") == null ? "" : requestDatas.get("occupation").toString();
		String mobileNo = requestDatas.get("mobile_number") == null ? "" : requestDatas.get("mobile_number").toString();
		String email = requestDatas.get("email") == null ? "" : requestDatas.get("email").toString();
		String idType = requestDatas.get("id_type") == null ? "" : requestDatas.get("id_type").toString();
		String idNumber = requestDatas.get("id_number") == null ? "" : requestDatas.get("id_number").toString();
		String region = requestDatas.get("region") == null ? "" : requestDatas.get("region").toString();
	//	String distict = requestDatas.get("district") == null ? "" : requestDatas.get("district").toString();
		String address = requestDatas.get("address") == null ? "" : requestDatas.get("address").toString();

		// Vehicle Page Datas
		String motorUsage = requestDatas.get("motor_usage") == null ? "" : requestDatas.get("motor_usage").toString();
		String bodyType = requestDatas.get("body_type") == null ? "" : requestDatas.get("body_type").toString();
		String make = requestDatas.get("make") == null ? "" : requestDatas.get("make").toString();
		String model = requestDatas.get("model") == null ? "" : requestDatas.get("model").toString();
		String regNo = requestDatas.get("registration_number") == null ? ""
				: requestDatas.get("registration_number").toString();
		String chassisNo = requestDatas.get("chassis_number") == null ? ""
				: requestDatas.get("chassis_number").toString();
		String engineNo = requestDatas.get("engine_number") == null ? "" : requestDatas.get("engine_number").toString();
		String enginecapacity = requestDatas.get("engine_capacity") == null ? ""
				: requestDatas.get("engine_capacity").toString();
		String seatingCapacity = requestDatas.get("seating_capacity") == null ? ""
				: requestDatas.get("seating_capacity").toString();
		String manYear = requestDatas.get("manufacture_year") == null ? ""
				: requestDatas.get("manufacture_year").toString();
		String color = requestDatas.get("color") == null ? "" : requestDatas.get("color").toString();
		String grossWeight = requestDatas.get("gross_weight") == null ? ""
				: requestDatas.get("gross_weight").toString();

		// Policy Page datas
		String insuranceClass = requestDatas.get("insurance_class") == null ? ""
				: requestDatas.get("insurance_class").toString();
		String inusredPeriod = requestDatas.get("insured_period") == null ? ""
				: requestDatas.get("insured_period").toString();
		String policyDate = requestDatas.get("policy_start_date") == null ? ""
				: requestDatas.get("policy_start_date").toString();
		String sum_insured = requestDatas.get("sum_insured") == null ? "" : requestDatas.get("sum_insured").toString();
		String extended_tppd_si = requestDatas.get("extended_tppd_si") == null ? ""
				: requestDatas.get("extended_tppd_si").toString();

		// DOB calc
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		LocalDate curDate = LocalDate.parse(policyDate, formatter);
		LocalDate minusDate = curDate.minusYears(18);
		String cusDob = minusDate.format(formatter);
		
		//Ids collect from desc 
		
		String titlteId="",genderId="",occupationId="",idTypeId="",regionId="",
				distictId="",colorId = "",motorUsageId="",bodyTypeId="",makeId="",modelId="",
				insuranceClassId="";
		
		//masterIds 
		Map<String,Object> titleMap = new HashMap<>();
		titleMap.put("Desc", title);
		titleMap.put("MasterType", "CUSTOMER_TITLE");
		titleMap.put("InsuranceId", "100046");
		
		try {
			String titileReq = mapper.writeValueAsString(titleMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi, titileReq);
			
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
		genderMap.put("InsuranceId", "100046");
		
		try {
			
			String genderReq = mapper.writeValueAsString(genderMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi, genderReq);
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
		occupationMap.put("InsuranceId", "100046");
		
		try {
			String occupationReq = mapper.writeValueAsString(occupationMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi, occupationReq);
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
		idTypeMap.put("InsuranceId", "100046");
		
		try {
			String idTypeReq = mapper.writeValueAsString(idTypeMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi, idTypeReq);
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
		regionMap.put("CountryCode", "ZMW");
		
		try {
			String regionReq = mapper.writeValueAsString(regionMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi, regionReq);
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
		districtMap.put("CountryCode", "ZMW");
		
		try {
			String districtReq = mapper.writeValueAsString(districtMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi, districtReq);
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
		insuranceClassMap.put("InsuranceId", "100046");
		
		try {
			String insuranceClassReq = mapper.writeValueAsString(insuranceClassMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi, insuranceClassReq);
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
		motorusageMap.put("InsuranceId", "100046");
		
		try {
			String motorUsageReq = mapper.writeValueAsString(motorusageMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi,motorUsageReq);
			
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
		bodyTypeMap.put("InsuranceId", "100046");
		
		try {
			String bodyTypeReq = mapper.writeValueAsString(bodyTypeMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi,bodyTypeReq);
			
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
		makeMap.put("InsuranceId", "100046");
		
		try {
			String makeReq = mapper.writeValueAsString(makeMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi,makeReq);
			
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
		modelMap.put("InsuranceId", "100046");
		
		try {
			String modelReq = mapper.writeValueAsString(modelMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi,modelReq);
			
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
		colorMap.put("InsuranceId", "100046");
		
		try {
			String colorReq = mapper.writeValueAsString(colorMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callZambiaComApi(masterApi,colorReq);
			
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

		
		// ==============================SAVE VEHICLE INFO BLOCK START=============================================
		
		  log.info("SAVE VEHICLE INFO START: "+new Date());
		 
		  Map<String,Object> vehicleInfo = new HashMap<String,Object>();
		  vehicleInfo.put("Insuranceid", "100046"); 
		  vehicleInfo.put("BranchCode", "137"); 
		  //vehicleInfo.put("AxelDistance", 1);
		  vehicleInfo.put("Chassisnumber", chassisNo); 
		  vehicleInfo.put("Color",colorId); 
		  vehicleInfo.put("CreatedBy", "");
		  vehicleInfo.put("DisplacementInCM3", "0"); 
		  vehicleInfo.put("EngineNumber", engineNo); 
		  //vehicleInfo.put("FuelType", null);
		  vehicleInfo.put("Grossweight", grossWeight);
		  vehicleInfo.put("ManufactureYear", manYear);
		  //vehicleInfo.put("MotorCategory", null); 
		  vehicleInfo.put("Motorusage", motorUsage); 
		  vehicleInfo.put("NumberOfAxels", null);
		  vehicleInfo.put("OwnerCategory", "1"); 
		  vehicleInfo.put("Registrationnumber", regNo); 
		  vehicleInfo.put("ResEngineCapacity", enginecapacity);
		  vehicleInfo.put("ResOwnerName", customerName);
		  vehicleInfo.put("ResStatusCode", "Y"); 
		  vehicleInfo.put("ResStatusDesc", "None"); 
		  vehicleInfo.put("SeatingCapacity", seatingCapacity);
		  vehicleInfo.put("HorsePower", "0"); 
		  vehicleInfo.put("Tareweight", null);
		  vehicleInfo.put("Vehcilemodel", model); 
		  vehicleInfo.put("VehcilemodelId", modelId); 
		  vehicleInfo.put("VehicleType", bodyType);
		  vehicleInfo.put("Vehiclemake", make); 
		  vehicleInfo.put("VehiclemakeId",makeId); 
		  //vehicleInfo.put("DisplacementInCM3", null);
		  vehicleInfo.put("NumberOfCylinders", 0); 
		  //vehicleInfo.put("PlateType", null);
		  
			try {
				String saveVehicleInfo = saveVehicleInfoApi;
				log.info("Save Vehicle Info Calling: " + saveVehicleInfo);
				String vehReq = mapper.writeValueAsString(vehicleInfo);
				log.info("Save Vehicle Req : " + vehReq);
				// String token =thread.getZambiaToken();
				String vehResponse = thread.callSwazilandComApi(saveVehicleInfoApi, vehReq);
				// String vehResponse
				// =thread.callPhoenixApi(saveVehicleInfo,mapper.writeValueAsString(vehicleInfo),
				// token);
				Map<String, Object> mapRes = mapper.readValue(vehResponse, Map.class);
				log.info("Save Vehicle Info Response: " + mapRes);

				Map<String, Object> vehInfoResult = mapRes.get("Result") == null ? null
						: mapper.readValue(mapper.writeValueAsString(mapRes.get("Result")), Map.class);

				if (vehInfoResult == null) {
					String errorMessgae = mapRes.get("ErrorMessage") == null ? ""
							: mapRes.get("ErrorMessage").toString();
					response = errorMessgae;
					return response;
				}
			} catch (Exception e) {
				e.printStackTrace();
				log.info(e);
			}
			log.info("SAVE VEHICLE INFO END: " + new Date());
		  
		 
		// ==============================SAVE VEHICLE INFO BLOCK END=============================================

		// ==============================CUSTOMER CREATION BLOCK START=============================================
		log.info("CUSTOMER CREATION BLOCK START: " + new Date());
		Map<String, Object> customerCreation = new HashMap<String, Object>();
		customerCreation.put("Activities", "");
		customerCreation.put("Address1", address);
		customerCreation.put("Address2", "");
		customerCreation.put("AppointmentDate", "");
		customerCreation.put("BranchCode", "126");
		customerCreation.put("BrokerBranchCode", "1");
		customerCreation.put("BusinessType", null);
		customerCreation.put("CityCode", distictId);
		customerCreation.put("CityName", region);// check district or region
		customerCreation.put("ClientName", customerName);
		customerCreation.put("Clientstatus", "Y");
		customerCreation.put("Country", "ZMB");
		customerCreation.put("CountryName", "Zambia");
		customerCreation.put("CreatedBy", "Zambia_whatsapp");// create login for whatsapp bot
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
		customerCreation.put("InsuranceId", "100046");
		customerCreation.put("IsTaxExempted", "N");
		customerCreation.put("Language", "1");
		customerCreation.put("LastName", "");
		customerCreation.put("MaritalStatus", "Single");// check
		customerCreation.put("MiddleName", "");
		customerCreation.put("MobileCode1", "260");
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
		customerCreation.put("WhatsappCode", "260");
		customerCreation.put("WhatsappDesc", "1");
		customerCreation.put("WhatsappNo", mobileNo);
		customerCreation.put("Zone", "1");

		String custRefNo = "";

		try {
			String cusReq = mapper.writeValueAsString(customerCreation);
			String cusSaveApi = saveCustomerApi;
			log.info("Customer Save Calling: " + cusSaveApi);

			String apiResponse = thread.callZambiaComApi(cusSaveApi, cusReq);

			log.info("Customer Save Response: " + apiResponse);
			Map<String, Object> cust = mapper.readValue(apiResponse, Map.class);

			Map<String, Object> custResult = cust.get("Result") == null ? null
					: mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);

			custRefNo = custResult.get("SuccessId") == null ? "" : custResult.get("SuccessId").toString();

			if (custResult == null) {
				String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.info(e);
		}

		log.info("CUSTOMER CREATION BLOCK END: " + new Date());

		// ==============================CUSTOMER CREATION BLOCK END=============================================

		// ==============================SHOW VEHICLE INFO BLOCK START=============================================
		log.info("SHOW VEHICLE INFO BLOCK START: " + new Date());
		Map<String, Object> vehInfo = new HashMap<String, Object>();
		vehInfo.put("BranchCode", "126");
		vehInfo.put("BrokerBranchCode", "1");
		vehInfo.put("CreatedBy", "Zambia_whatsapp");
		vehInfo.put("InsuranceId", "100046");
		vehInfo.put("ProductId", "5");
		vehInfo.put("ReqChassisNumber", "");
		vehInfo.put("ReqRegNumber", regNo);
		vehInfo.put("SavedFrom", "API");

		Map<String, Object> showVehResult = null;
		try {
			String showInfo = mapper.writeValueAsString(vehInfo);
			String showVehApi = showVehicleInfoApi;

			log.info("Show Vehicle Api Calling: " + showVehApi);

			String apiResponse = thread.callZambiaComApi(showVehApi, showInfo);

			log.info("Show Vehicle Response: " + apiResponse);

			Map<String, Object> showVeh = mapper.readValue(apiResponse, Map.class);
			showVehResult = showVeh.get("Result") == null ? null : (Map<String, Object>) showVeh.get("Result");

			if (showVehResult == null) {
				String errorMessgae = showVeh.get("ErrorMessage") == null ? "" : showVeh.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("SHOW VEHICLE INFO BLOCK END: " + new Date());
		// ==============================SHOW VEHICLE INFO BLOCK END=============================================

		// ==============================MOTOR SAVE BLOCK START=============================================
		log.info("MOTOR SAVE BLOCK START: " + new Date());
		LocalDate endDate = curDate.plusDays(364);
		String policyEndDate = endDate.format(formatter);

		Map<String, Object> motorSave = new HashMap<String, Object>();
		motorSave.put("AboutVehicle", null);
		motorSave.put("AcExecutiveId", null);
		motorSave.put("AcccessoriesSumInsured", null);
		motorSave.put("AccessoriesInformation", null);
		motorSave.put("AdditionalCircumstances", null);
		motorSave.put("AgencyCode", "13300");// local-13506,
		motorSave.put("AggregatedValue", null);
		motorSave.put("ApplicationId", "1");
		motorSave.put("AxelDistance", 1);
		motorSave.put("BankingDelegation", "");
		motorSave.put("BdmCode", "Zambia_whatsapp");// broker id
		motorSave.put("BorrowerType", null);
		motorSave.put("BranchCode", "126");
		motorSave.put("BrokerBranchCode", "1");
		motorSave.put("BrokerCode", "13300");// local-13506
		motorSave.put("Chassisnumber", chassisNo);
		motorSave.put("CityLimit", null);
		motorSave.put("ClaimType", "0");
		motorSave.put("ClaimTypeDesc", null);
		motorSave.put("CollateralCompanyAddress", "");
		motorSave.put("CollateralCompanyName", "");
		motorSave.put("CollateralName", null);
		motorSave.put("CollateralYn", "N");
		motorSave.put("Color", colorId);
		motorSave.put("ColorDesc", color);
		motorSave.put("CommissionType", null);
		motorSave.put("CoverNoteNo", null);
		motorSave.put("CreatedBy", "Zambia_whatsapp");// broker id
		motorSave.put("CubicCapacity", enginecapacity);
		motorSave.put("Currency", "ZMW");
		motorSave.put("CustomerCode", "Zambia_whatsapp");// broker id
		motorSave.put("CustomerName", customerName);
		motorSave.put("CustomerReferenceNo", custRefNo);
		motorSave.put("DateOfCirculation", null);
		motorSave.put("Deductibles", null);
		motorSave.put("DefenceValue", "");
		motorSave.put("DisplacementInCM3", null);
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
		motorSave.put("InsuranceClass", insuranceClassId);
		motorSave.put("InsuranceClassDesc", insuranceClass);
		motorSave.put("InsuranceId", "100046");
		motorSave.put("Insurancetype", insuranceClassId);// 103 check
		motorSave.put("InsurancetypeDesc", insuranceClass);
		motorSave.put("InsurerSettlement", "");
		motorSave.put("InterestedCompanyDetails", "");
		motorSave.put("IsFinanceEndt", null);
		motorSave.put("LoanAmount", 0);
		motorSave.put("LoanEndDate", null);
		motorSave.put("LoanStartDate", null);
		motorSave.put("LocationId", "1");
		motorSave.put("LoginId", "Zambia_whatsapp");// login
		motorSave.put("ManufactureYear", manYear);
		motorSave.put("MarketValue", null);
		motorSave.put("Mileage", null);
		motorSave.put("MobileCode", "260");
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
		motorSave.put("NoOfFemale", null);
		motorSave.put("NoOfMale", null);
		motorSave.put("NoOfPassengers", null);
		motorSave.put("NoOfVehicles", "1");
		motorSave.put("NumberOfAxels", null);
		motorSave.put("NumberOfCards", null);
		motorSave.put("NumberOfCylinders", null);
		motorSave.put("Occupation", occupationId);
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
		motorSave.put("QuoteExpiryDays", 90);
		motorSave.put("RadioOrCasseteplayer", null);
		motorSave.put("RegistrationDate", null);
		motorSave.put("RegistrationYear", cusDob);// check
		motorSave.put("Registrationnumber", regNo);
		motorSave.put("RequestReferenceNo", "");
		motorSave.put("RoofRack", null);
		motorSave.put("SaveOrSubmit", "Save");
		motorSave.put("SavedFrom", "WEB");
		motorSave.put("SearchFromApi", false);
		motorSave.put("SeatingCapacity", seatingCapacity);
		motorSave.put("SectionId", Arrays.asList(insuranceClassId));
		motorSave.put("SourceType", "Broker");
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
		motorSave.put("VehicleId", "1");
		motorSave.put("VehicleType", bodyType);
		motorSave.put("VehicleTypeId", bodyTypeId);
		motorSave.put("VehicleTypeIvr", "");
		motorSave.put("VehicleValueType", "");
		motorSave.put("Vehiclemake", make);
		motorSave.put("VehiclemakeId", makeId);
		motorSave.put("WindScreenSumInsured", null);
		motorSave.put("Windscreencoverrequired", null);
		motorSave.put("Zone", "1");
		motorSave.put("ZoneCirculation", null);
		motorSave.put("accident", null);
		motorSave.put("periodOfInsurance", inusredPeriod);// inusredPeriod
		LinkedHashMap<String, Object> exchangeRateScenario = new LinkedHashMap<>();
		exchangeRateScenario.put("OldAcccessoriesSumInsured", null);
		exchangeRateScenario.put("OldCurrency", "ZMW");
		exchangeRateScenario.put("OldExchangeRate", "1.0");
		exchangeRateScenario.put("OldSumInsured", 0);
		exchangeRateScenario.put("OldTppdIncreaeLimit", null);
		exchangeRateScenario.put("OldWindScreenSumInsured", null);

		LinkedHashMap<String, Object> excahnge = new LinkedHashMap<>();
		excahnge.put("ExchangeRateScenario", exchangeRateScenario);
		motorSave.put("Scenarios", excahnge);

		Map<String, Object> saveMotResult = null;
		Map<String, Object> saveMotorRes = new HashMap<String, Object>();
		String reqRefNo = "";
		try {
			String motorSaveReq = mapper.writeValueAsString(motorSave);
			String saveMotorApi = motorSaveApi;
			log.info("Save Motor Api Calling: " + saveMotorApi);

			String apiResponse = thread.callZambiaComApi(saveMotorApi, motorSaveReq);

			log.info("Save Motor Response: " + apiResponse);

			Map<String, Object> saveMot = mapper.readValue(apiResponse, Map.class);
			saveMotResult = saveMot.get("Result") == null ? null : (Map<String, Object>) saveMot.get("Result");

			reqRefNo = saveMotResult.get("RequestReferenceNo") == null ? ""
					: saveMotResult.get("RequestReferenceNo").toString();

			if (saveMotResult == null) {
				String errorMessgae = saveMot.get("ErrorMessage") == null ? "" : saveMot.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.info("MOTOR SAVE BLOCK END: " + new Date());
		// ==============================MOTOR SAVE BLOCK END=============================================

		// ==============================CALC BLOCK START=============================================
		log.info("CALC BLOCK START: " + new Date());

		Map<String, Object> calc = new HashMap<String, Object>();
		calc.put("AgencyCode", "13300");// local-13506
		calc.put("BranchCode", "126");
		calc.put("CdRefNo", saveMotResult.get("CdRefNo") == null ? "" : saveMotResult.get("CdRefNo"));
		calc.put("CoverModification", "N");
		calc.put("CreatedBy", "Zambia_whatsapp");
		calc.put("DdRefNo", saveMotResult.get("DdRefNo") == null ? "" : saveMotResult.get("DdRefNo"));
		calc.put("EffectiveDate", policyDate);
		calc.put("InsuranceId", "100046");
		calc.put("LocationId", "1");
		calc.put("MSRefNo", saveMotResult.get("MSRefNo") == null ? "" : saveMotResult.get("MSRefNo"));
		calc.put("PolicyEndDate", policyEndDate);
		calc.put("ProductId", "5");
		calc.put("RequestReferenceNo", reqRefNo);
		calc.put("SectionId", saveMotResult.get("SectionId") == null ? "" : saveMotResult.get("SectionId"));
		calc.put("VdRefNo", saveMotResult.get("VdRefNo") == null ? "" : saveMotResult.get("VdRefNo"));
		calc.put("VehicleId", "1");
		calc.put("productId", "5");

		List<Map<String, Object>> coverList = null;
		Map<String, Object> calcRes = null;
		Map<String, Object> calcResult = null;
		try {
			String calReq = mapper.writeValueAsString(calc);

			String calculatorApi = calcApi;
			String apiResponse = thread.callZambiaComApi(calculatorApi, calReq);

			log.info("Save Motor Response: " + apiResponse);

			calcRes = mapper.readValue(apiResponse, Map.class);
			coverList = calcRes.get("CoverList") == null ? null
					: mapper.readValue(mapper.writeValueAsString(calcRes.get("CoverList")), List.class);

			calcResult = calcRes.get("Result") == null ? null : (Map<String, Object>) calcRes.get("Result");
			
			 coverList = coverList.stream().filter(cov -> (cov.get("CoverId").equals("279")) || 
					 (cov.get("CoverId").equals("505") || (cov.get("CoverId").equals("504"))) ).toList();

			if (calcResult == null) {
				String errorMessgae = calcRes.get("ErrorMessage") == null ? "" : calcRes.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Long premium = 0L;
		Long vatTax = 0L;
		Double vatPercentage = 0D;

		List<Map<String, Object>> tax = (List<Map<String, Object>>) coverList.get(0).get("Taxes");

		BigDecimal pre = coverList.stream()
				.filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString())
						|| "D".equalsIgnoreCase(p.get("isSelected").toString()))
				.map(m -> new BigDecimal(m.get("PremiumExcluedTaxLC").toString()))
				.reduce(BigDecimal.ZERO, (x, y) -> x.add(y));

		List<Map<String, Object>> taxList = coverList.parallelStream()
				.filter(p -> ("B".equalsIgnoreCase(p.get("CoverageType").toString())
						|| "D".equalsIgnoreCase(p.get("isSelected").toString())) && p.get("Taxes") != null)
				.map(m -> (List<Map<String, Object>>) m.get("Taxes")).flatMap(f -> f.stream())
				.collect(Collectors.toList());

		vatTax = taxList.stream()
				.map(t -> t.get("TaxAmount") == null ? 0L : Double.valueOf(t.get("TaxAmount").toString()).longValue())
				.reduce(0L, (a, b) -> a + b);

		vatPercentage = tax.get(1).get("TaxRate") == null ? 0L : Double.valueOf(tax.get(1).get("TaxRate").toString());

		premium = pre.longValue();

		Long totalPremium = pre.longValue() + vatTax.longValue();

		log.info("CALC BLOCK END: " + new Date());

		// ==============================CALC BLOCK END=============================================================

		// ==============================USER CREATION BLOCK START=============================================

		log.info("USER CREATION BLOCK START: " + new Date());

		Map<String, Object> userCreationMap = new HashMap<String, Object>();
		userCreationMap.put("CompanyId", "100046");
		userCreationMap.put("CustomerId", custRefNo);
		userCreationMap.put("ProductId", "5");
		userCreationMap.put("ReferenceNo", reqRefNo);
		userCreationMap.put("UserMobileNo", mobileNo);
		userCreationMap.put("UserMobileCode", "260");
		userCreationMap.put("AgencyCode", "13300"); // local-13506

		Map<String, Object> userResult = null;
		Map<String, Object> userRes = null;
		try {
			String userCreationReq = mapper.writeValueAsString(userCreationMap);
			String userCreationApi = loginCreationApi;

			log.info("USER CREATION API: " + userCreationApi);

			String apiResponse = thread.callZambiaComApi(userCreationApi, userCreationReq);

			log.info("USER CREATION RESPONSE: " + apiResponse);

			userRes = mapper.readValue(apiResponse, Map.class);

			userResult = userRes.get("Result") == null ? null : (Map<String, Object>) userRes.get("Result");

			if (userResult == null) {
				String errorMessgae = calcRes.get("ErrorMessage") == null ? "" : calcRes.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("USER CREATION BLOCK END : " + new Date());

		// ==============================USER CREATION BLOCK END=============================================

		// ==============================BUY POLICY BLOCK START=============================================
		log.info("BUY POLICY BLOCK START : " + new Date());

		Map<String, Object> coversMap = new HashMap<>();
		coversMap.put("CoverId", "5");
		coversMap.put("SubCoverYn", "N");

		Function<Map<String, Object>, Map<String, Object>> function = fun -> {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("CoverId", fun.get("CoverId").toString());
			map.put("SubCoverYn", "N");
			return map;
		};

		List<Map<String, Object>> buyCovers = coverList.stream()
				.filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString())
						|| "D".equalsIgnoreCase(p.get("isSelected").toString()))
				.map(function).collect(Collectors.toList());

		Map<String, Object> vehicleMap = new HashMap<String, Object>();
		vehicleMap.put("SectionId", saveMotResult.get("SectionId") == null ? "" : saveMotResult.get("SectionId"));
		vehicleMap.put("Id", "1");
		vehicleMap.put("LocationId", "1");
		vehicleMap.put("Covers", buyCovers);
		List<Map<String, Object>> vehiMapList = new ArrayList<Map<String, Object>>();
		vehiMapList.add(vehicleMap);

		Map<String, Object> buypolicyMap = new HashMap<String, Object>();
		buypolicyMap.put("RequestReferenceNo", reqRefNo);
		buypolicyMap.put("CreatedBy", "Zambia_whatsapp");
		buypolicyMap.put("ProductId", "5");
		buypolicyMap.put("ManualReferralYn", "N");
		buypolicyMap.put("Vehicles", vehiMapList);

		Map<String, Object> buyPolicyResult = null;
		Map<String, Object> buyPolicyRes = null;
		try {
			String buypolicyReq = objectPrint.toJson(buypolicyMap);
			String buyPolicyApi = buyPolicy;

			log.info("BUY POLICY API: " + buyPolicyApi);

			String apiResponse = thread.callZambiaComApi(buyPolicyApi, buypolicyReq);

			log.info("BUY POLICY RESPONSE: " + apiResponse);

			buyPolicyRes = mapper.readValue(apiResponse, Map.class);
			buyPolicyResult = buyPolicyRes.get("Result") == null ? null
					: (Map<String, Object>) buyPolicyRes.get("Result");

			if (buyPolicyResult == null) {
				String errorMessgae = buyPolicyRes.get("ErrorMessage") == null ? ""
						: buyPolicyRes.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}

		} catch (Exception e) {
			e.printStackTrace();
			exception = e.getMessage();
		}
		if (StringUtils.isNotBlank(exception)) {
			errorList.add(new Error(exception, "ErrorMsg", "101"));
		}

		if (errorList.size() > 0) {
			throw new WhatsAppValidationException(errorList);

		}

		log.info("BUYPOLICY  BLOCK END : " + new Date());

		// ==============================BUY POLICY BLOCK END=============================================

		// ==============================MAKE PAYMENT BLOCK START=============================================

		log.info("MAKE PAYMENT BLOCK START : " + new Date());
		Map<String, Object> makePaymentMap = new HashMap<String, Object>();
		makePaymentMap.put("CreatedBy", "Zambia_whatsapp");
		makePaymentMap.put("EmiYn", "N");
		makePaymentMap.put("InstallmentMonth", null);
		makePaymentMap.put("InstallmentPeriod", null);
		makePaymentMap.put("InsuranceId", "100046");
		makePaymentMap.put("Premium", totalPremium);
		makePaymentMap.put("QuoteNo", buyPolicyResult.get("QuoteNo"));
		makePaymentMap.put("Remarks", "None");
		makePaymentMap.put("SubUserType", "Broker");
		makePaymentMap.put("UserType", "Broker");

		Map<String, Object> makePaymentResult = null;
		Map<String, Object> makePaymentRes = null;
		try {
			String makePayemantReq = objectPrint.toJson(makePaymentMap);

			String makePaymentApi = makePayment;
			log.info("MAKE PAYMENT API: " + makePaymentApi);
			String apiResponse = thread.callZambiaComApi(makePaymentApi, makePayemantReq);
			log.info("MAKE PAYMENT RESPONSE: " + apiResponse);

			makePaymentRes = mapper.readValue(apiResponse, Map.class);
			makePaymentResult = makePaymentRes.get("Result") == null ? null
					: (Map<String, Object>) makePaymentRes.get("Result");

			if (makePaymentResult == null) {
				String errorMessgae = buyPolicyRes.get("ErrorMessage") == null ? ""
						: buyPolicyRes.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}
		} catch (Exception e) {
			e.printStackTrace();
			exception = e.getMessage();
		}

		if (StringUtils.isNotBlank(exception)) {
			errorList.add(new Error(exception, "ErrorMsg", "101"));
		}

		if (errorList.size() > 0) {
			throw new WhatsAppValidationException(errorList);

		}

		log.info("MAKE PAYMENT BLOCK END : " + new Date());

		// ==============================MAKE PAYMENT BLOCK END=============================================

		// ==============================INSERT PAYMENT BLOCK START=============================================
		log.info("INSERT PAYMENT BLOCK START : " + new Date());

		Map<String, Object> insertPaymentMap = new HashMap<>();
		insertPaymentMap.put("AccountNumber", null);
		insertPaymentMap.put("BankName", null);
		insertPaymentMap.put("ChequeDate", "");
		insertPaymentMap.put("ChequeNo", null);
		insertPaymentMap.put("CreatedBy", "Zambia_whatsapp");
		insertPaymentMap.put("EmiYn", "N");
		insertPaymentMap.put("IbanNumber", null);
		insertPaymentMap.put("InsuranceId", "100046");
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

		Map<String, Object> insertPaymentRes = null;
		Map<String, Object> instPatmentResult = null;
		try {
			String insertPaymentReq = objectPrint.toJson(insertPaymentMap);
			String insertPaymentApi = insertPayment;
			log.info("INSERT PAYMENT API: " + insertPaymentApi);
			String apiResponse = thread.callZambiaComApi(insertPaymentApi, insertPaymentReq);
			log.info("INSERT PAYMENT RESPONSE: " + apiResponse);
			insertPaymentRes = mapper.readValue(apiResponse, Map.class);
			instPatmentResult = insertPaymentRes.get("Result") == null ? null
					: (Map<String, Object>) insertPaymentRes.get("Result");

			if (instPatmentResult == null) {
				String errorMessgae = buyPolicyRes.get("ErrorMessage") == null ? ""
						: buyPolicyRes.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}
		} catch (Exception e) {
			e.printStackTrace();
			exception = e.getMessage();
		}

		if (StringUtils.isNotBlank(exception)) {
			errorList.add(new Error(exception, "ErrorMsg", "101"));
		}

		if (errorList.size() > 0) {
			throw new WhatsAppValidationException(errorList);

		}

		String marchantRefNo = instPatmentResult.get("MerchantReference") == null ? ""
				: instPatmentResult.get("MerchantReference").toString();

		String quoteNo = instPatmentResult.get("QuoteNo") == null ? "" : instPatmentResult.get("QuoteNo").toString();

		log.info("RequestRefNo : " + reqRefNo + " ||  MerchantReference : " + marchantRefNo + " || QuoteNo : " + quoteNo
				+ " ");

		log.info("INSERT PAYMENT BLOCK END : " + new Date());

		// ==============================INSERT PAYMENT BLOCK END=============================================

		// ==============================PAYMENT LINK BLOCK START=============================================
		log.info("PAYMENT LINK BLOCK START : " + new Date());

		Map<String, Object> paymentMap = new HashMap<>();
		paymentMap.put("MerchantRefNo", marchantRefNo);
		paymentMap.put("CompanyId", "100046");
		paymentMap.put("WhatsappCode", "260");
		paymentMap.put("WhtsappNo", mobileNo);
		paymentMap.put("QuoteNo", quoteNo);

		String payJson = objectPrint.toJson(paymentMap);
		String encodeReq = Base64.getEncoder().encodeToString(payJson.getBytes());

		String paymentUrl = phoenixMotorPaymentlink + encodeReq;

		log.info("PAYMENT LINK :" + paymentUrl);

		log.info("PAYMENT LINK BLOCK END : " + new Date());

		// ==============================PAYMENT LINK BLOCK END=============================================

		// ==============================WHATSAPP RESPONSE BLOCK START=============================================
		log.info("WHATSAPP RESPONSE BLOCK START : " + new Date());

		Map<String, Object> getMotorReq = new HashMap<String, Object>();
		getMotorReq.put("RequestReferenceNo", reqRefNo);

		String api_request = mapper.writeValueAsString(getMotorReq);
		String motorDetailsApi = getAllMotorDetailsApi;
		String apiResponse = thread.callZambiaComApi(motorDetailsApi, api_request);

		Map<String, Object> getMotorRes = mapper.readValue(apiResponse, Map.class);

		List<Map<String, Object>> motorRes = getMotorRes.get("Result") == null ? null
				: mapper.readValue(mapper.writeValueAsString(getMotorRes.get("Result")), List.class);

		log.info("ALL MOTOR DETAILS :" + motorRes);

		Map<String, Object> mot = motorRes.get(0);

		botResponceData.put("registration",
				mot.get("Registrationnumber") == null ? "N/A" : mot.get("Registrationnumber"));
		botResponceData.put("usage", mot.get("MotorUsageDesc") == null ? "N/A" : mot.get("MotorUsageDesc"));
		botResponceData.put("vehtype", mot.get("VehicleTypeDesc") == null ? "N/A" : mot.get("VehicleTypeDesc"));
		botResponceData.put("color", mot.get("ColorDesc") == null ? "N/A" : mot.get("ColorDesc"));
		// botResponceData.put("insurance_class",insuredClass);
		botResponceData.put("premium", pre);
		botResponceData.put("url", paymentUrl);
		botResponceData.put("vatamt", vatTax);
		botResponceData.put("suminsured", mot.get("SumInsured") == null ? "N/A" : mot.get("SumInsured"));
		botResponceData.put("chassis", mot.get("Chassisnumber") == null ? "N/A" : mot.get("Chassisnumber"));
		botResponceData.put("vat", String.valueOf(vatPercentage.longValue()));
		botResponceData.put("totalpremium", totalPremium);
		botResponceData.put("inceptiondate", policyDate);
		botResponceData.put("expirydate", policyEndDate);
		botResponceData.put("referenceno", reqRefNo);
		botResponceData.put("veh_model_desc",
				mot.get("VehcilemodelDesc") == null ? "N/A" : mot.get("VehcilemodelDesc"));
		botResponceData.put("veh_make_desc", mot.get("VehiclemakeDesc") == null ? "N/A" : mot.get("VehiclemakeDesc"));
		botResponceData.put("customer_name", customerName);

		return botResponceData;
	}

	@Override
	public Object generateZambiaQuote(Object req)
			throws JsonProcessingException, JsonMappingException, WhatsAppValidationException {

		String response = "";
		String exception = "";
		List<Error> errorList = new ArrayList<>(2);
		Map<String,Object> botResponceData = new HashMap<String,Object>();
		Map<String,Object> data = mapper.convertValue(req, Map.class);
	//	String flowDatas = new String(Base64.getDecoder().decode(req.getQuote_form()));
		
	//	Map<String,Object> requestDatas = mapper.readValue(flowDatas, Map.class);
		
		String cus_name = data.get("cus_name") == null ? "" : data.get("cus_name").toString();
		String mob_no = data.get("mob_no") == null ? "" : data.get("mob_no").toString();
		String motor_usage = data.get("motor_usage") == null ? "" : data.get("motor_usage").toString();
		String reg_no = data.get("reg_no") == null ? "" : data.get("reg_no").toString();
		String id_type = data.get("id_type") == null ? "" : data.get("id_type").toString();
		String id_num = data.get("id_num") == null ? "" : data.get("id_num").toString();
		String ins_period = data.get("ins_period") == null ? "" : data.get("ins_period").toString();
		String insuranceClass = "Full Third Party";
		String sum_insured = data.get("sum_insured") == null ? null : data.get("sum_insured").toString();
		
		String idType = "";
		if(StringUtils.isNotBlank(id_type)) {
			if(id_type.equals("1")) {
				idType = "Driver License";
			}else if(id_type.equals("2")) {
				idType = "NUIT Number";
			}else if(id_type.equals("3")) {
				idType = "Passport Number";
			}
		}
		
		String inusredPeriod = "";
		if(StringUtils.isNotBlank(ins_period)) {
			if(ins_period.equals("1")) {
				inusredPeriod = "30";
			}else if(ins_period.equals("2")) {
				inusredPeriod = "90";
			}else if(ins_period.equals("3")) {
				inusredPeriod = "180";
			}else if(ins_period.equals("4")) {
				inusredPeriod = "270";
			}else if(ins_period.equals("5")) {
				inusredPeriod = "365";
			}
		}
		String motorUsage = "";
		if(StringUtils.isNotBlank(motor_usage)) {
			if(motor_usage.equals("1")) {
				motorUsage = "Commercial";
			}else if(motor_usage.equals("2")) {
				motorUsage = "Motorcycles";
			}else if(motor_usage.equals("3")) {
				motorUsage = "Private";
			}else if(motor_usage.equals("4")) {
				motorUsage = "Public Transport Vehicle";
			}
		}
		
				//DOB calc
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
				LocalDate curDate = LocalDate.now();
				String policyDate = curDate.format(formatter);
			//	LocalDate curDate = LocalDate.parse(policyDate,formatter);
				LocalDate minusDate = curDate.minusYears(18);
				String cusDob = minusDate.format(formatter);
				
				//==============================SHOW VEHICLE INFO BLOCK START=============================================
				log.info("SHOW VEHICLE INFO BLOCK START: "+new Date());
				Map<String,Object> vehInfo = new HashMap<String,Object>();
				vehInfo.put("BranchCode", "126");
				vehInfo.put("BrokerBranchCode", "1");
				vehInfo.put("CreatedBy", "Zambia_whatsapp");
				vehInfo.put("InsuranceId", "100046");
				vehInfo.put("ProductId", "5");
				vehInfo.put("ReqChassisNumber", "");
				vehInfo.put("ReqRegNumber", reg_no);
				vehInfo.put("SavedFrom", "API");
				
				Map<String,Object> showVehResult = null;
				try {
					String showInfo = mapper.writeValueAsString(vehInfo);
					String showVehApi = showVehicleInfoApi;
					log.info("Show Vehicle Request: "+showInfo);
					log.info("Show Vehicle Api Calling: "+showVehApi);
					
					String apiResponse = thread.callZambiaComApi(showVehApi, showInfo);
					
					log.info("Show Vehicle Response: "+apiResponse);
					
					Map<String,Object> showVeh = mapper.readValue(apiResponse, Map.class);
					showVehResult = showVeh.get("Result") == null ? null : (Map<String, Object>) showVeh.get("Result");
						
					if(showVehResult == null) {
						String errorMessgae = showVeh.get("ErrorMessage") == null ? "" : showVeh.get("ErrorMessage").toString();
						response = errorMessgae;
						callVehicleNotinListTemplate(cus_name,reg_no,"919159339730");
						return response;
					}
							
				}catch(Exception e) {
					e.printStackTrace();
				}
				log.info("SHOW VEHICLE INFO BLOCK END: "+new Date());
				//==============================SHOW VEHICLE INFO BLOCK END=============================================
				
				
				//ID's Calling
				Map<String,Object> idTypeMap = new HashMap<>();
				idTypeMap.put("Desc", idType);
				idTypeMap.put("MasterType", "CUSTOMER_IDTYPE");
				idTypeMap.put("InsuranceId", "100046");
				String idTypeId = "";
				try {
					String idTypeReq = mapper.writeValueAsString(idTypeMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callZambiaComApi(masterApi, idTypeReq);
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
				String insuranceClassId="";
				Map<String,Object> insuranceClassMap = new HashMap<>();
				insuranceClassMap.put("Desc", insuranceClass);
				insuranceClassMap.put("MasterType", "INSURANCE_CLASS");
				insuranceClassMap.put("InsuranceId", "100046");
				
				try {
					String insuranceClassReq = mapper.writeValueAsString(insuranceClassMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callZambiaComApi(masterApi, insuranceClassReq);
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
				String motorUsageId="";
				Map<String,Object> motorUsageMap = new HashMap<>();
				motorUsageMap.put("Desc", motorUsage);
				motorUsageMap.put("MasterType", "MOTOR_USAGE");
				motorUsageMap.put("InsuranceId", "100046");
				
				try {
					String motorUsageReq = mapper.writeValueAsString(motorUsageMap);
					String masterApi = masterIdsApi;
					log.info("Master Ids Api Calling: "+masterApi);
					
					String apiResponse = thread.callZambiaComApi(masterApi, motorUsageReq);
					log.info("Master Api Response: "+apiResponse);

					Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
					
					if(masterApiResult.isEmpty() || masterApiResult == null) {
						String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
						response = errorMessage;
						return response;
					}else {
						motorUsageId = masterApiResult.get("Response") == null ? "" : masterApiResult.get("Response").toString();
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				String bodyTypeId="";
				String bodyType = showVehResult.get("VehicleType") == null ? "" : showVehResult.get("VehicleType").toString();
				if(StringUtils.isNotBlank(bodyType)) {
					
					Map<String,Object> bodyTypeMap = new HashMap<>();
					motorUsageMap.put("Desc", bodyType);
					motorUsageMap.put("MasterType", "BODY_TYPE");
					motorUsageMap.put("InsuranceId", "100046");
					
					try {
						String bodyTypeReq = mapper.writeValueAsString(motorUsageMap);
						String masterApi = masterIdsApi;
						log.info("Master Ids Api Calling: "+masterApi);
						
						String apiResponse = thread.callZambiaComApi(masterApi, bodyTypeReq);
						log.info("Master Api Response: "+apiResponse);

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
						log.info(e);
					}
				}
				
				//==============================CUSTOMER CREATION BLOCK START=============================================
				log.info("CUSTOMER CREATION BLOCK START: "+new Date());
				Map<String,Object> customerCreation = new HashMap<String,Object>();
				customerCreation.put("Activities", "");
				customerCreation.put("Address1", "");
				customerCreation.put("Address2", "");
				customerCreation.put("AppointmentDate", "");
				customerCreation.put("BranchCode", "126");
				customerCreation.put("BrokerBranchCode", "1");
				customerCreation.put("BusinessType", null);
				customerCreation.put("CityCode", "1");
				customerCreation.put("CityName", "Chibombo");//check district or region
				customerCreation.put("ClientName", cus_name);
				customerCreation.put("Clientstatus", "Y");
				customerCreation.put("Country", "ZMB");
				customerCreation.put("CountryName", "Zambia");
				customerCreation.put("CreatedBy", "Zambia_whatsapp");//create login for whatsapp bot
				customerCreation.put("CustomerAsInsurer", "N");
				customerCreation.put("CustomerReferenceNo", "");
				customerCreation.put("DobOrRegDate", cusDob);
				customerCreation.put("Email1", "");
				customerCreation.put("Email2", null);
				customerCreation.put("Email3", null);
				customerCreation.put("ExpiryDate", null);
				customerCreation.put("Fax", null);
				customerCreation.put("Gender", "M");
				customerCreation.put("IdNumber", id_num);
				customerCreation.put("IdType", idTypeId);
				customerCreation.put("InsuranceId", "100046");
				customerCreation.put("IsTaxExempted", "N");
				customerCreation.put("Language", "1");
			//	customerCreation.put("LastName", "");
			//	customerCreation.put("MaritalStatus", "Single");//check
			//	customerCreation.put("MiddleName", "");
				customerCreation.put("MobileCode1", "260");
				customerCreation.put("MobileCodeDesc1", "1");
				customerCreation.put("MobileNo1", mob_no);
			//	customerCreation.put("MobileNo2", "");
				customerCreation.put("MobileNo3", null);
				customerCreation.put("Nationality", "");
				customerCreation.put("Occupation", "9");
				customerCreation.put("OtherOccupation", "");
				customerCreation.put("PhoneNoCode", "260");
				customerCreation.put("PinCode", "");
				customerCreation.put("Placeofbirth", "chennai");
				customerCreation.put("PolicyHolderType", "1");
				customerCreation.put("PolicyHolderTypeid", idTypeId);
				customerCreation.put("PreferredNotification", "sms");
				customerCreation.put("ProductId", "5");
				customerCreation.put("RegionCode", "10000");
				customerCreation.put("RiskAssessmentDate", null);
				customerCreation.put("SaveOrSubmit", "Save");
				customerCreation.put("SocioProfessionalCategory", null);
				customerCreation.put("StateCode", "10000");
				customerCreation.put("StateName", null);
				customerCreation.put("Status", "Y");
				customerCreation.put("Street", "");
				customerCreation.put("TaxExemptedId", null);
				customerCreation.put("TelephoneNo1", "");
				customerCreation.put("TelephoneNo2", null);
				customerCreation.put("TelephoneNo3", null);
				customerCreation.put("Title", "1");
				customerCreation.put("Type", null);
				customerCreation.put("VipFlag", null);
				customerCreation.put("VrTinNo", null);
			//	customerCreation.put("WhatsappCode", "260");
				customerCreation.put("WhatsappDesc", "1");
			//	customerCreation.put("WhatsappNo", mobileNo);
				customerCreation.put("Zone", "1");
				
				String custRefNo = "";
				
				try {
					String cusReq = mapper.writeValueAsString(customerCreation);
					String cusSaveApi = saveCustomerApi;
					log.info("Customer Save Request: "+cusReq);
					log.info("Customer Save Calling: "+cusSaveApi);
					
					String apiResponse = thread.callZambiaComApi(cusSaveApi,cusReq);
					
					log.info("Customer Save Response: "+apiResponse);
					Map<String,Object> cust = mapper.readValue(apiResponse, Map.class);
					
					Map<String,Object> custResult = cust.get("Result") == null ? null :
						mapper.readValue(mapper.writeValueAsString(cust.get("Result")), Map.class);
					
				    custRefNo = custResult.get("SuccessId") == null ? "" : custResult.get("SuccessId").toString();
					
					if(custResult == null) {
						String errorMessgae = cust.get("ErrorMessage") == null ? "" : cust.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}
				}catch(Exception e) {
					e.printStackTrace();
					log.info(e);
				}
				
				log.info("CUSTOMER CREATION BLOCK END: "+new Date());
				
				//==============================CUSTOMER CREATION BLOCK END=============================================
				
				
		/*	
				//==============================SAVE VEHICLE INFO BLOCK START=============================================
				log.info("SAVE VEHICLE INFO START: "+new Date());
				
				Map<String,Object> vehicleInfo = new HashMap<String,Object>();
				vehicleInfo.put("Insuranceid", "100046");
				vehicleInfo.put("BranchCode", "136");
				//vehicleInfo.put("AxelDistance", 1);
				vehicleInfo.put("Chassisnumber", chassisNo);
				vehicleInfo.put("Color", colorId);
				vehicleInfo.put("CreatedBy", "");
				vehicleInfo.put("DisplacementInCM3", "0");
				vehicleInfo.put("EngineNumber", engineNo);
				//vehicleInfo.put("FuelType", null);
				vehicleInfo.put("Grossweight", grossWeight);
				vehicleInfo.put("ManufactureYear", manYear);
				//vehicleInfo.put("MotorCategory", null);
				vehicleInfo.put("Motorusage", motorUsage);
				vehicleInfo.put("NumberOfAxels", null);
				vehicleInfo.put("OwnerCategory", "1");
				vehicleInfo.put("Registrationnumber", regNo);
				vehicleInfo.put("ResEngineCapacity", enginecapacity);
				vehicleInfo.put("ResOwnerName", customerName);
				vehicleInfo.put("ResStatusCode", "Y");
				vehicleInfo.put("ResStatusDesc", "None");
				vehicleInfo.put("SeatingCapacity", seatingCapacity);
				vehicleInfo.put("HorsePower", "0");
				vehicleInfo.put("Tareweight", null);
				vehicleInfo.put("Vehcilemodel", model);
				vehicleInfo.put("VehcilemodelId", modelId);
				vehicleInfo.put("VehicleType", bodyType);
				vehicleInfo.put("Vehiclemake", make);
				vehicleInfo.put("VehiclemakeId", makeId);
				//vehicleInfo.put("DisplacementInCM3", null);
				vehicleInfo.put("NumberOfCylinders", 0);
				//vehicleInfo.put("PlateType", null);
				
				try {
					String saveVehicleInfo = saveVehicleInfoApi;
					log.info("Save Vehicle Info Calling: "+saveVehicleInfo);
					String token = thread.getNamibiaToken();
					String vehResponse = thread.callPhoenixApi(saveVehicleInfo, mapper.writeValueAsString(vehicleInfo), token);
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
				
				//==============================MOTOR SAVE BLOCK START=============================================
				log.info("MOTOR SAVE BLOCK START: "+new Date());
				LocalDate endDate = curDate.plusDays(Long.valueOf(inusredPeriod));
				String policyEndDate = endDate.format(formatter);
				
				Map<String,Object> motorSave = new HashMap<String,Object>();
				motorSave.put("AboutVehicle", null);
				motorSave.put("AcExecutiveId", null);
				motorSave.put("AcccessoriesSumInsured", null);
				motorSave.put("AccessoriesInformation", "");
				motorSave.put("AdditionalCircumstances", "");
				motorSave.put("AgencyCode", "13928");//local-13300,live-13928
				motorSave.put("AggregatedValue", null);
				motorSave.put("ApplicationId", "1");
				motorSave.put("AxelDistance", null);
				motorSave.put("BankingDelegation", "");
				motorSave.put("BdmCode", "2000050");//local-546544543,local-2000050
				motorSave.put("BorrowerType", null);
				motorSave.put("BranchCode", "126");
				motorSave.put("BrokerBranchCode", "1");
				motorSave.put("BrokerCode", "13928");//local-13300,live-13928
				motorSave.put("Chassisnumber", showVehResult.get("ReqChassisNumber") == null ? "" : showVehResult.get("ReqChassisNumber"));
				motorSave.put("CityLimit", null);
				motorSave.put("ClaimType", "0");
			//	motorSave.put("ClaimTypeDesc", noClaimBonus);
				motorSave.put("CollateralCompanyAddress", "");
				motorSave.put("CollateralCompanyName", "");
				motorSave.put("CollateralName", null);
				motorSave.put("CollateralYn", "N");
				motorSave.put("Color", showVehResult.get("Color") == null ? "" : showVehResult.get("Color"));
				motorSave.put("ColorDesc", showVehResult.get("Color") == null ? "" : showVehResult.get("Color"));
				motorSave.put("CommissionType", null);
				motorSave.put("CoverNoteNo", null);
				motorSave.put("CreatedBy", "Zambia_whatsapp");
				motorSave.put("CubicCapacity", showVehResult.get("ResEngineCapacity") == null ? "" : showVehResult.get("ResEngineCapacity"));
				motorSave.put("Currency", "ZMW");
				motorSave.put("CustomerCode", "2000050");//local-546544543,local-2000050
				motorSave.put("CustomerName", cus_name);
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
				motorSave.put("EndorsementYn", null);
				motorSave.put("EndtCategoryDesc", null);
				motorSave.put("EndtCount", null);
				motorSave.put("EndtPrevPolicyNo", null);
				motorSave.put("EndtPrevQuoteNo", null);
				motorSave.put("EndtStatus", null);
				motorSave.put("EngineCapacity", showVehResult.get("ResEngineCapacity") == null ? "" : showVehResult.get("ResEngineCapacity"));
				motorSave.put("EngineNumber",  showVehResult.get("EngineNumber") == null ? "" : showVehResult.get("EngineNumber"));
				motorSave.put("ExcessLimit", null);
				motorSave.put("ExchangeRate", "1.0");
				motorSave.put("FirstLossPayee", null);
				motorSave.put("FleetOwnerYn", "N");
				motorSave.put("FuelType", null);
				motorSave.put("FuelTypeDesc", null);
				motorSave.put("Gpstrackinginstalled", "N");
				motorSave.put("Grossweight", showVehResult.get("Grossweight") == null ? "" : showVehResult.get("Grossweight"));
				motorSave.put("HavePromoCode", "N");
				motorSave.put("HoldInsurancePolicy", "N");
				motorSave.put("HorsePower", "");
				motorSave.put("Idnumber", id_num);
				motorSave.put("Inflation", "");
				motorSave.put("InflationSumInsured", "");
				motorSave.put("InsuranceClass", insuranceClassId);
				motorSave.put("InsuranceClassDesc", insuranceClass);
				motorSave.put("InsuranceId", "100046");
				motorSave.put("Insurancetype", insuranceClassId);//103 check
				motorSave.put("InsurancetypeDesc", insuranceClass);
				motorSave.put("InsurerSettlement", "");
				motorSave.put("InterestedCompanyDetails", "");
				motorSave.put("IsFinanceEndt", null);
				motorSave.put("LoanAmount", 0);
				motorSave.put("LoanEndDate", null);
				motorSave.put("LoanStartDate", null);
			//	motorSave.put("LocationId", "1");
				motorSave.put("LoginId", "Zambia_whatsapp");//login
				motorSave.put("ManufactureYear", showVehResult.get("ManufactureYear") == null ? "" : showVehResult.get("ManufactureYear"));
				motorSave.put("MarketValue", null);
				motorSave.put("Mileage", null);
				motorSave.put("MobileCode", "260");
				motorSave.put("MobileNumber", mob_no);
				motorSave.put("ModelNumber", null);
				motorSave.put("MotorCategory", showVehResult.get("MotorCategory") == null ? "" : showVehResult.get("MotorCategory"));
				motorSave.put("Motorusage", motorUsage);//showVehResult.get("Motorusage") == null ? "" : showVehResult.get("Motorusage")
				motorSave.put("MotorusageId", motorUsageId);
				motorSave.put("MunicipalityTraffic", null);
				motorSave.put("Ncb", "0");
				motorSave.put("NcdYn", "N");
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
			//	motorSave.put("NumberOfCylinders", null);
				motorSave.put("Occupation", "9");
				motorSave.put("OrginalPolicyNo", null);
				motorSave.put("OwnerCategory", null);
				motorSave.put("PaCoverId", "0");
			//	motorSave.put("PlateType", null);
				motorSave.put("PolicyEndDate", policyEndDate); 
				motorSave.put("PolicyRenewalYn", "N");
				motorSave.put("PolicyStartDate", policyDate);
				motorSave.put("PolicyType", "1");
				motorSave.put("PreviousInsuranceYN", "N");
				motorSave.put("PreviousLossRatio", "");
				motorSave.put("ProductId", "5");
				motorSave.put("PromoCode", null);
				motorSave.put("PurchaseDate", null);
				motorSave.put("QuoteExpiryDays", "90");
				motorSave.put("RadioOrCasseteplayer", null);
				motorSave.put("RegistrationDate", null);
				motorSave.put("RegistrationYear", cusDob);//check
				motorSave.put("Registrationnumber", showVehResult.get("ReqRegNumber") == null ? "" : showVehResult.get("ReqRegNumber"));
				motorSave.put("RequestReferenceNo", "");
				motorSave.put("RoofRack", null);
			//	motorSave.put("SaveOrSubmit", "Save");
				motorSave.put("SavedFrom", "Web");
				motorSave.put("SearchFromApi", true);
				motorSave.put("SeatingCapacity", showVehResult.get("SeatingCapacity") == null ? "" : showVehResult.get("SeatingCapacity"));
				motorSave.put("SectionId", Arrays.asList(insuranceClassId));
			//	motorSave.put("SourceType", null);
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
				motorSave.put("Vehcilemodel", showVehResult.get("Vehcilemodel") == null ? "" : showVehResult.get("Vehcilemodel"));
				motorSave.put("VehcilemodelId", showVehResult.get("VehcilemodelId") == null ? "" : showVehResult.get("VehcilemodelId"));
				motorSave.put("VehicleId", 1);
				motorSave.put("VehicleType", showVehResult.get("VehicleType") == null ? "" : showVehResult.get("VehicleType"));
				motorSave.put("VehicleTypeId", bodyTypeId);
				motorSave.put("VehicleTypeIvr", "");
				motorSave.put("VehicleValueType", "");
				motorSave.put("Vehiclemake", showVehResult.get("Vehiclemake") == null ? "" : showVehResult.get("Vehiclemake"));
				motorSave.put("VehiclemakeId", showVehResult.get("VehiclemakeId") == null ? "" : showVehResult.get("VehiclemakeId"));
				motorSave.put("WindScreenSumInsured", null);
				motorSave.put("Windscreencoverrequired", null);
				motorSave.put("Zone", "1");
				motorSave.put("ZoneCirculation", null);
				motorSave.put("accident", null);
				motorSave.put("periodOfInsurance", inusredPeriod);
				LinkedHashMap<String, Object> exchangeRateScenario = new  LinkedHashMap<>();
				exchangeRateScenario.put("OldAcccessoriesSumInsured", null);
				exchangeRateScenario.put("OldCurrency", "ZMW");
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
					log.info("Save Motor Request: "+motorSaveReq);
					log.info("Save Motor Api Calling: "+saveMotorApi);
					
					String apiResponse = thread.callZambiaComApi(saveMotorApi,motorSaveReq);
					
					log.info("Save Motor Response: "+apiResponse);
					
					Map<String,Object> saveMot = mapper.readValue(apiResponse, Map.class);
					saveMotResult = saveMot.get("Result") == null ? null : (List<Map<String, Object>>) saveMot.get("Result");
					
				//	reqRefNo = saveMotResult.get("RequestReferenceNo") == null ? "" : saveMotResult.get("RequestReferenceNo").toString();
						
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
				calc.put("AgencyCode", "13928");//local-13300,live-13928
				calc.put("BranchCode", "126");
				calc.put("CdRefNo", motorSaveResult.get("CdRefNo") == null ? "" : motorSaveResult.get("CdRefNo"));
				calc.put("CoverModification", "N");
				calc.put("CreatedBy", "Zambia_whatsapp");
				calc.put("DdRefNo", motorSaveResult.get("DdRefNo") == null ? "" : motorSaveResult.get("DdRefNo"));
				calc.put("EffectiveDate", policyDate);
				calc.put("InsuranceId", "100046");
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
				List<Map<String,Object>> referral =null;
				Map<String,Object> calcRes=null;
				Map<String,Object> calcResult = null;
				try {
					String calReq = mapper.writeValueAsString(calc);
					log.info("calc Request: "+calReq);
					String calculatorApi = calcApi;
					log.info("calc API: "+calculatorApi);
					String apiResponse = thread.callZambiaComApi(calculatorApi, calReq);
					
					log.info("Calc Response: "+apiResponse);
					
					 calcRes = mapper.readValue(apiResponse, Map.class);
					 coverList = calcRes.get("CoverList") == null ? null :
						 mapper.readValue(mapper.writeValueAsString(calcRes.get("CoverList")), List.class);
					 
					 coverList = coverList.stream().filter(cov -> (cov.get("CoverId").equals("279")) || 
							 (cov.get("CoverId").equals("505") || (cov.get("CoverId").equals("504"))) ).toList();
					 
					 referral = calcRes.get("MasterReferral") == null ? null :
						 mapper.readValue(mapper.writeValueAsString(calcRes.get("MasterReferral")), List.class);
					 
				//	calcResult = calcRes.get("Result") == null ? null : (Map<String, Object>) calcRes.get("Result");
					
					if(coverList == null) {
						String errorMessgae = calcRes.get("ErrorMessage") == null ? "" : calcRes.get("ErrorMessage").toString();
						response = errorMessgae;
						return response;
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				
				//referral Block
				if(!referral.isEmpty()) {
					callReferralTemplate(cus_name,reqRefNo,"919159339730");
					
					return "Your Quotation "+reqRefNo+" has been Referral";
				}
				
				
				
				Long premium=0L;
			//	Long vatTax =0L;
				Double vatPercentage=0D;
				
				List<Map<String,Object>> tax = (List<Map<String, Object>>) coverList.get(0).get("Taxes");
				
				BigDecimal pre = coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
						.map(m -> new BigDecimal(m.get("PremiumExcluedTaxLC").toString())).reduce(BigDecimal.ZERO, (x,y) -> x.add(y));
				
				List<Map<String,Object>> taxList = coverList.parallelStream().filter(p -> ("B".equalsIgnoreCase(p.get("CoverageType").toString()) 
						|| "D".equalsIgnoreCase(p.get("isSelected").toString())) && p.get("Taxes") != null)
						.map(m -> (List<Map<String,Object>>) m.get("Taxes")).flatMap(f -> f.stream()).collect(Collectors.toList());
				
				BigDecimal vatTax = taxList.stream().map(t -> t.get("TaxAmount") == null ? BigDecimal.ZERO : new BigDecimal(t.get("TaxAmount").toString()))
						.reduce(BigDecimal.ZERO, BigDecimal::add);
				
				vatPercentage = tax.get(0).get("TaxRate")==null?0L:Double.valueOf(tax.get(0).get("TaxRate").toString());
				
				premium = pre.longValue();
				
				//Long totalPremium =pre.longValue()+vatTax.longValue();
				BigDecimal totPremium = pre.add(vatTax);
				
			String totalPremium = String.valueOf(totPremium);
						
				log.info("CALC BLOCK END: "+new Date());
				
				//==============================CALC BLOCK END=============================================================
				
				//==============================USER CREATION BLOCK START=============================================
				
				log.info("USER CREATION BLOCK START: "+new Date());
				
				Map<String,Object> userCreationMap = new HashMap<String,Object>();
				userCreationMap.put("CompanyId", "100046");
				userCreationMap.put("CustomerId", custRefNo);
				userCreationMap.put("ProductId", "5");
				userCreationMap.put("ReferenceNo", reqRefNo);
				userCreationMap.put("UserMobileNo", mob_no);
				userCreationMap.put("UserMobileCode", "260");
				userCreationMap.put("AgencyCode", "13928");//local-13300,live-13928
				
				Map<String,Object> userResult = null;
				Map<String,Object> userRes = null;
				try {
					String userCreationReq = mapper.writeValueAsString(userCreationMap);
					String userCreationApi = loginCreationApi;
					
					log.info("USER CREATION API: "+userCreationApi);
					
					String apiResponse = thread.callZambiaComApi(userCreationApi, userCreationReq);
					
					log.info("USER CREATION RESPONSE: "+apiResponse);
					
					 userRes = mapper.readValue(apiResponse, Map.class);
					
					userResult = userRes.get("Result") == null ? null : (Map<String, Object>) userRes.get("Result");
					
				//	if(userResult == null) {
				//		String errorMessgae = calcRes.get("ErrorMessage") == null ? "" : calcRes.get("ErrorMessage").toString();
				//		response = errorMessgae;
				//		return response;
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
				buypolicyMap.put("CreatedBy", "Zambia_whatsapp");
				buypolicyMap.put("ProductId", "5");
				buypolicyMap.put("ManualReferralYn", "N");
				buypolicyMap.put("Vehicles", vehiMapList);
				
				Map<String,Object> buyPolicyResult = null;
				Map<String,Object> buyPolicyRes = null;
				try {
					String buypolicyReq =objectPrint.toJson(buypolicyMap);
					String buyPolicyApi = buyPolicy;
					log.info("BUY POLICY Request: "+buypolicyReq);
					log.info("BUY POLICY API: "+buyPolicyApi);
					
					String apiResponse = thread.callZambiaComApi(buyPolicyApi, buypolicyReq);
					
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
				makePaymentMap.put("CreatedBy", "Zambia_whatsapp");
				makePaymentMap.put("EmiYn", "N");
				makePaymentMap.put("InstallmentMonth", null);
				makePaymentMap.put("InstallmentPeriod", null);
				makePaymentMap.put("InsuranceId", "100046");
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
					log.info("MAKE PAYMENT Request: "+makePayemantReq);
					log.info("MAKE PAYMENT API: "+makePaymentApi);
					String apiResponse = thread.callZambiaComApi(makePaymentApi, makePayemantReq);
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
				insertPaymentMap.put("CreatedBy", "Zambia_whatsapp");
				insertPaymentMap.put("EmiYn", "N");
				insertPaymentMap.put("IbanNumber", null);
				insertPaymentMap.put("InsuranceId", "100046");
				insertPaymentMap.put("MICRNo", null);
				insertPaymentMap.put("MobileCode1", null);
				insertPaymentMap.put("MobileNo1", null);
				insertPaymentMap.put("PayeeName", cus_name);
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
					log.info("INSERT PAYMENT Request: "+insertPaymentReq);
					log.info("INSERT PAYMENT API: "+insertPaymentApi);
					String apiResponse = thread.callZambiaComApi(insertPaymentApi, insertPaymentReq);
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
			    paymentMap.put("CompanyId", "100046");
			    paymentMap.put("WhatsappCode", "260");
			    paymentMap.put("WhtsappNo", mob_no);
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
				String apiResponse = thread.callZambiaComApi(motorDetailsApi, api_request);
				
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
				botResponceData.put("premium", pre);
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
				botResponceData.put("customer_name", cus_name);
				
				
				return botResponceData;
			
	}

	private void callReferralTemplate(String cus_name, String reqRefNo, String chatNum) {
		log.info("REFERRAL TEMPLATE BLOCK START: "+new Date());
		log.info("CURRENT CHAT NUMBER IS: "+chatNum);
		
		Map<String,Object> respMap = new HashMap<>();
		respMap.put("to", chatNum);
		respMap.put("type", "template");
		
		Map<String,Object> langMap = new HashMap<>();
		langMap.put("policy", "deterministic");
		langMap.put("code", "en");
		
		Map<String,Object> componentsMap = new HashMap<>();
		componentsMap.put("type", "body");
		
		Map<String,Object> nameMap = new HashMap<>();
		nameMap.put("type", "text");
		nameMap.put("text", cus_name);
		
		Map<String,Object> reqNoMap = new HashMap<>();
		reqNoMap.put("type", "text");
		reqNoMap.put("text", reqRefNo);
		
		componentsMap.put("parameters", Arrays.asList(nameMap,reqNoMap));
		
		Map<String,Object> tempMap = new HashMap<>();
		tempMap.put("language", langMap);
		tempMap.put("name", "referral_template_copy");
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

	private void callVehicleNotinListTemplate(String cus_name,String regNo, String chatNum) {
		log.info("VEHICLE NOT IN LIST TEMPLATE BLOCK START: "+new Date());
		log.info("CURRENT CHAT NUMBER IS: "+chatNum);
		
		Map<String,Object> respMap = new HashMap<>();
		respMap.put("to", chatNum);
		respMap.put("type", "template");
		
		Map<String,Object> langMap = new HashMap<>();
		langMap.put("policy", "deterministic");
		langMap.put("code", "en");
		
		Map<String,Object> componentsMap = new HashMap<>();
		componentsMap.put("type", "body");
		
		Map<String,Object> nameMap = new HashMap<>();
		nameMap.put("type", "text");
		nameMap.put("text", cus_name);
		
		Map<String,Object> regNoMap = new HashMap<>();
		regNoMap.put("type", "text");
		regNoMap.put("text", regNo);
		
		componentsMap.put("parameters", Arrays.asList(nameMap,regNoMap));
		
		Map<String,Object> tempMap = new HashMap<>();
		tempMap.put("language", langMap);
		tempMap.put("name", "vehicle_not_list");
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

	@Override
	public Object generateZambiaMotorQuote(Object req)
			throws JsonMappingException, JsonProcessingException, WhatsAppValidationException {
		
			String response = "";
			String exception = "";
			List<Error> errorList = new ArrayList<>(2);
			Map<String,Object> botResponceData = new HashMap<String,Object>();
			Map<String,Object> data = mapper.convertValue(req, Map.class);
		//	String flowDatas = new String(Base64.getDecoder().decode(req.getQuote_form()));
			
		//	Map<String,Object> requestDatas = mapper.readValue(flowDatas, Map.class);
			
			//Customer Details
			String customerName = data.get("CustomerName") == null ? "" : data.get("CustomerName").toString();
			String mobileNo = data.get("MobileNo") == null ? "" : data.get("MobileNo").toString();
			String idType = data.get("IdType") == null ? "" : data.get("IdType").toString();
			String idNumber = data.get("IdNumber") == null ? "" : data.get("IdNumber").toString();
			String email = data.get("Email") == null ? "" : data.get("Email").toString();
			
			//Motor Details
			String motorUsage = data.get("MotorUsage") == null ? "" : data.get("MotorUsage").toString();
			String registrationNumber = data.get("RegistrationNumber") == null ? "" : data.get("RegistrationNumber").toString();
			//String chassisNumber = data.get("chassisNumber") == null ? "" : data.get("chassisNumber").toString();
			//String engineNumber = data.get("engineNumber") == null ? "" : data.get("engineNumber").toString();
			String vehicleMake = data.get("VehicleMake") == null ? "" : data.get("VehicleMake").toString();
			String vehicleModel = data.get("VehicleModel") == null ? "" : data.get("VehicleModel").toString();
			String vehicleType = data.get("VehicleType") == null ? "" : data.get("VehicleType").toString();
			//String color = data.get("color") == null ? "" : data.get("color").toString();
			//String engineCapacity = data.get("engineCapacity") == null ? "" : data.get("engineCapacity").toString();
			//String seatingCapacity = data.get("seatingCapacity") == null ? "" : data.get("seatingCapacity").toString();
			//String manufactureYear = data.get("manufactureYear") == null ? "" : data.get("manufactureYear").toString();
			
			
			String insurancePeriod = data.get("InsurancePeriod") == null ? "" : data.get("InsurancePeriod").toString();
			String insuranceClass = data.get("InsuranceClass") == null ? "" : data.get("InsuranceClass").toString();
			String sumInsured = data.get("SumInsured") == null ? null : data.get("SumInsured").toString();
			String policyStartDate = data.get("PolicyStartDate") == null ? null : data.get("PolicyStartDate").toString();
			
			String insuranceClassId ="",motorUsageId="",bodyTypeId="",makeId="",modelId="",idTypeId="";
			
			Map<String,Object> idTypeMap = new HashMap<>();
			idTypeMap.put("Desc", idType);
			idTypeMap.put("MasterType", "CUSTOMER_IDTYPE");
			idTypeMap.put("InsuranceId", "100046");
			
			try {
				String idTypeReq = mapper.writeValueAsString(idTypeMap);
				String masterApi = masterIdsApi;
				log.info("Master Ids Api Calling: "+masterApi);
				
				String apiResponse = thread.callZambiaComApi(masterApi, idTypeReq);
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
			insuranceClassMap.put("InsuranceId", "100046");
			
			try {
				String insuranceClassReq = mapper.writeValueAsString(insuranceClassMap);
				String masterApi = masterIdsApi;
				log.info("Master Ids Api Calling: "+masterApi);
				
				String apiResponse = thread.callZambiaComApi(masterApi, insuranceClassReq);
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
			motorusageMap.put("InsuranceId", "100046");
			
			try {
				String motorUsageReq = mapper.writeValueAsString(motorusageMap);
				String masterApi = masterIdsApi;
				log.info("Master Ids Api Calling: "+masterApi);
				
				String apiResponse = thread.callZambiaComApi(masterApi,motorUsageReq);
				
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
			bodyTypeMap.put("Desc", vehicleType);
			bodyTypeMap.put("MasterType", "BODY_TYPE");
			bodyTypeMap.put("InsuranceId", "100046");
			
			try {
				String bodyTypeReq = mapper.writeValueAsString(bodyTypeMap);
				String masterApi = masterIdsApi;
				log.info("Master Ids Api Calling: "+masterApi);
				
				String apiResponse = thread.callZambiaComApi(masterApi,bodyTypeReq);
				
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
			makeMap.put("Desc", vehicleMake);
			makeMap.put("MasterType", "VEHICLE_MAKE");
			makeMap.put("InsuranceId", "100046");
			
			try {
				String makeReq = mapper.writeValueAsString(makeMap);
				String masterApi = masterIdsApi;
				log.info("Master Ids Api Calling: "+masterApi);
				
				String apiResponse = thread.callZambiaComApi(masterApi,makeReq);
				
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
			modelMap.put("Desc", vehicleModel);
			modelMap.put("MasterType", "VEHICLE_MODEL");
			modelMap.put("InsuranceId", "100046");
			
			try {
				String modelReq = mapper.writeValueAsString(modelMap);
				String masterApi = masterIdsApi;
				log.info("Master Ids Api Calling: "+masterApi);
				
				String apiResponse = thread.callZambiaComApi(masterApi,modelReq);
				
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
			
			//==============================CUSTOMER CREATION BLOCK START=============================================
			
			
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
			LocalDate startDate = LocalDate.parse(policyStartDate, formatter);
			LocalDate endDate = startDate.plusDays(Long.valueOf(insurancePeriod));
			String policyEndDate = endDate.format(formatter);
			
			LocalDate curDate = LocalDate.parse(policyStartDate,formatter);
			LocalDate minusDate = curDate.minusYears(18);
			String cusDob = minusDate.format(formatter);
			
			log.info("CUSTOMER CREATION BLOCK START: "+new Date());
			Map<String,Object> customerCreation = new HashMap<String,Object>();
			customerCreation.put("Activities", "1");
			customerCreation.put("Address1", null);
			customerCreation.put("Address2", "");
			customerCreation.put("AppointmentDate", "");
			customerCreation.put("BranchCode", "138");
			customerCreation.put("BrokerBranchCode", "1");
			customerCreation.put("BusinessType", 1);
			customerCreation.put("CityCode", null);
			customerCreation.put("CityName", null);//check district or region
			customerCreation.put("ClientName", customerName);
			customerCreation.put("Clientstatus", "Y");
			customerCreation.put("Country", "ZMB");
			customerCreation.put("CountryName", "Zambia");
			customerCreation.put("CreatedBy", "guest_zmb1");//create login for whatsapp bot
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
			customerCreation.put("InsuranceId", "100046");
			customerCreation.put("IsTaxExempted", "N");
			customerCreation.put("Language", "1");
			customerCreation.put("LastName", "");
			customerCreation.put("MaritalStatus", null);//check
			customerCreation.put("MiddleName", "");
			customerCreation.put("MobileCode1", "260");
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
			customerCreation.put("WhatsappCode", "260");
			customerCreation.put("WhatsappDesc", "1");
			customerCreation.put("WhatsappNo", mobileNo);
			customerCreation.put("Zone", "1");
			
			String custRefNo = "";
			
			try {
				String cusReq = mapper.writeValueAsString(customerCreation);
				String cusSaveApi = saveCustomerApi;
				log.info("Customer Save Calling: "+cusSaveApi);
				log.info("Customer Save Request: "+cusReq);
				
				String apiResponse = thread.callZambiaComApi(cusSaveApi,cusReq);
				
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
						
						Pattern pattern = Pattern.compile("PIZ-CUST-[0-9]+");
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
						    	getCustomerReq.put("InsuranceId", "100046");
						    	
						    	String getCusReq = mapper.writeValueAsString(getCustomerReq);
								String CustomerApi = getCustomerApi;
								log.info("GET CUSTOMER Api Calling: "+getCustomerApi);
								log.info("GET CUSTOMER Request: "+getCusReq);
								
								 apiResponse = thread.callZambiaComApi(CustomerApi,getCusReq);
								
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
			motorSave.put("AgencyCode", "13579");//local-13495,UAT-14035
			motorSave.put("AggregatedValue", null);
			motorSave.put("ApplicationId", "1");
			motorSave.put("AxelDistance", 1);
			motorSave.put("BankingDelegation", "");
			motorSave.put("BdmCode", "guest_zmb1");//uAt-2000010,local-5555555
			motorSave.put("BorrowerType", null);
			motorSave.put("BranchCode", "138");
			motorSave.put("BrokerBranchCode", "1");
			motorSave.put("BrokerCode", "13579");//local-13495,UAT-14035
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
			motorSave.put("CreatedBy", "guest_zmb1");//broker id
			motorSave.put("CubicCapacity", "1200");//doubt
			motorSave.put("Currency", "ZMW");
			motorSave.put("CustomerCode", "guest_zmb1");//uAt-2000010,local-5555555
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
			motorSave.put("InsuranceId", "100046");
			motorSave.put("Insurancetype", insuranceClassId);//103 check
			motorSave.put("InsurancetypeDesc", insuranceClass);
			motorSave.put("InsurerSettlement", "");
			motorSave.put("InterestedCompanyDetails", "");
			motorSave.put("IsFinanceEndt", null);
			motorSave.put("LoanAmount", 0);
			motorSave.put("LoanEndDate", null);
			motorSave.put("LoanStartDate", null);
			motorSave.put("LocationId", "1");
			motorSave.put("LoginId", "guest_zmb1");//login
			motorSave.put("ManufactureYear", "2023");
			motorSave.put("MarketValue", null);
			motorSave.put("Mileage", null);
			motorSave.put("MobileCode", "260");
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
			motorSave.put("PolicyStartDate", policyStartDate);
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
			motorSave.put("Registrationnumber", registrationNumber);
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
			motorSave.put("SumInsured", sumInsured);
			motorSave.put("Tareweight", null);
			motorSave.put("TiraCoverNoteNo", null);
			motorSave.put("TppdFreeLimit", null);
			motorSave.put("TppdIncreaeLimit", null);
			motorSave.put("TrailerDetails", null);
			motorSave.put("TransportHydro", null);
			motorSave.put("UsageId", "");
			motorSave.put("UserType", "Broker");
			motorSave.put("Vehcilemodel", vehicleModel);
			motorSave.put("VehcilemodelId", modelId);
			motorSave.put("VehicleId", 1);
			motorSave.put("VehicleType", vehicleType);
			motorSave.put("VehicleTypeId", bodyTypeId);
			motorSave.put("VehicleTypeIvr", "");
			motorSave.put("VehicleValueType", null);
			motorSave.put("Vehiclemake", vehicleMake);
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
				
				String apiResponse = thread.callZambiaComApi(saveMotorApi,motorSaveReq);
				
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
			calc.put("AgencyCode", "13579");//local-13506,UAT-14035
			calc.put("BranchCode", "138");
			calc.put("CdRefNo", motorSaveResult.get("CdRefNo") == null ? "" : motorSaveResult.get("CdRefNo"));
			calc.put("CoverModification", "N");
			calc.put("CreatedBy", "guest_zmb1");
			calc.put("DdRefNo", motorSaveResult.get("DdRefNo") == null ? "" : motorSaveResult.get("DdRefNo"));
			calc.put("EffectiveDate", policyStartDate);
			calc.put("InsuranceId", "100046");
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
				String apiResponse = thread.callZambiaComApi(calculatorApi, calReq);
				
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
			
			List<Map<String,Object>> basecover = coverList.stream().filter(p -> ("279".equalsIgnoreCase(p.get("CoverId").toString()))
					|| ("504".equalsIgnoreCase(p.get("CoverId").toString()))).collect(Collectors.toList());
			
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
			userCreationMap.put("CompanyId", "100046");
			userCreationMap.put("CustomerId", custRefNo);
			userCreationMap.put("ProductId", "5");
			userCreationMap.put("ReferenceNo", reqRefNo);
			userCreationMap.put("UserMobileNo", mobileNo);
			userCreationMap.put("UserMobileCode", "260");
			userCreationMap.put("AgencyCode", "13579"); //local-13506,UAT-14035
			
			Map<String,Object> userResult = null;
			Map<String,Object> userRes = null;
			try {
				String userCreationReq = mapper.writeValueAsString(userCreationMap);
				String userCreationApi = loginCreationApi;
				
				log.info("USER CREATION API: "+userCreationApi);
				
				String apiResponse = thread.callZambiaComApi(userCreationApi, userCreationReq);
				
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
			buypolicyMap.put("CreatedBy", "guest_zmb1");
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
				
				String apiResponse = thread.callZambiaComApi(buyPolicyApi, buypolicyReq);
				
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
			botResponceData.put("InceptionDate", policyStartDate);
			botResponceData.put("ExpiryDate",policyEndDate);
			botResponceData.put("ReferenceNo", reqRefNo);
			botResponceData.put("Model", mot.get("VehcilemodelDesc")==null?"N/A":mot.get("VehcilemodelDesc"));
			botResponceData.put("Make", mot.get("VehiclemakeDesc")==null?"N/A":mot.get("VehiclemakeDesc"));
			botResponceData.put("CustomerName", customerName);
			botResponceData.put("QuoteNo", buyPolicyResult.get("QuoteNo"));
			
			
			
			return botResponceData;
			
	}

	@Override
	public Object paymentLinkGenerationZambia(Object req)
			throws JsonProcessingException, JsonMappingException, WhatsAppValidationException {
		
		String response = "";
		String exception = "";
		List<Error> errorList = new ArrayList<>(2);
		Map<String, Object> botResponceData = new HashMap<String, Object>();

		Map<String, Object> data = mapper.convertValue(req, Map.class);
		
		String customerName = data.get("CustomerName") == null ? "" : data.get("CustomerName").toString();
		String quoteNo = data.get("QuoteNo") == null ? "" : data.get("QuoteNo").toString();
		String totalPremium = data.get("TotalPremium") == null ? "" : data.get("TotalPremium").toString();
		String reqRefNo = data.get("ReferenceNo") == null ? "" : data.get("ReferenceNo").toString();
		String mobileNo = data.get("MobileNo") == null ? "" : data.get("MobileNo").toString();
		
		//==============================MAKE PAYMENT BLOCK START=============================================
			
			log.info("MAKE PAYMENT BLOCK START : "+new Date());
			Map<String,Object> makePaymentMap = new HashMap<String,Object>();
			makePaymentMap.put("CreatedBy", "guest_zmb1");
			makePaymentMap.put("EmiYn", "N");
			makePaymentMap.put("InstallmentMonth", null);
			makePaymentMap.put("InstallmentPeriod", null);
			makePaymentMap.put("InsuranceId", "100046");
			makePaymentMap.put("Premium", totalPremium);
			makePaymentMap.put("QuoteNo", quoteNo);
			makePaymentMap.put("Remarks", "None");
			makePaymentMap.put("SubUserType", "b2c");
			makePaymentMap.put("UserType", "User");
			
			Map<String,Object> makePaymentResult =null;
			Map<String,Object> makePaymentRes =null;
			try {
				String makePayemantReq = objectPrint.toJson(makePaymentMap);
				
				String makePaymentApi = makePayment;
				log.info("MAKE PAYMENT API: "+makePaymentApi);
				
				log.info("MAKE PAYMENT REQUEST: "+makePayemantReq);
				String apiResponse = thread.callZambiaComApi(makePaymentApi, makePayemantReq);
				log.info("MAKE PAYMENT RESPONSE: "+apiResponse);
				
				makePaymentRes = mapper.readValue(apiResponse, Map.class);
				makePaymentResult = makePaymentRes.get("Result") == null ? null :
					(Map<String, Object>) makePaymentRes.get("Result");
				
				if(makePaymentResult == null) {
					String errorMessgae = makePaymentRes.get("ErrorMessage") == null ? "" : makePaymentRes.get("ErrorMessage").toString();
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
			insertPaymentMap.put("CreatedBy", "guest_zmb1");
			insertPaymentMap.put("EmiYn", "N");
			insertPaymentMap.put("IbanNumber", null);
			insertPaymentMap.put("InsuranceId", "100046");
			insertPaymentMap.put("MICRNo", null);
			insertPaymentMap.put("MobileCode1", "260");
			insertPaymentMap.put("MobileNo1", mobileNo);
			insertPaymentMap.put("PayeeName", customerName);
			insertPaymentMap.put("PaymentId", makePaymentResult.get("PaymentId"));
			insertPaymentMap.put("PaymentType", "5");
			insertPaymentMap.put("Payments", "");
			insertPaymentMap.put("Premium", totalPremium);
			insertPaymentMap.put("QuoteNo", quoteNo);
			insertPaymentMap.put("Remarks", "None");
			insertPaymentMap.put("SubUserType", "b2c");
			insertPaymentMap.put("UserType", "User");
			insertPaymentMap.put("WhatsappCode", null);
			insertPaymentMap.put("WhatsappNo", null);
			
			Map<String,Object> insertPaymentRes = null;
			Map<String,Object> instPatmentResult = null;
			try {
				String insertPaymentReq = objectPrint.toJson(insertPaymentMap);
				String insertPaymentApi = insertPayment;
				log.info("INSERT PAYMENT API: "+insertPaymentApi);
				log.info("INSERT PAYMENT REQUEST: "+insertPaymentReq);
				String apiResponse = thread.callZambiaComApi(insertPaymentApi, insertPaymentReq);
				log.info("INSERT PAYMENT RESPONSE: "+apiResponse);
				insertPaymentRes = mapper.readValue(apiResponse, Map.class);
				instPatmentResult = insertPaymentRes.get("Result") == null ? null :
					(Map<String, Object>) insertPaymentRes.get("Result");
				
				if(instPatmentResult == null) {
					String errorMessgae = insertPaymentRes.get("ErrorMessage") == null ? "" : insertPaymentRes.get("ErrorMessage").toString();
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
			
		//	String quoteNo = instPatmentResult.get("QuoteNo") == null ? "" :
		//		instPatmentResult.get("QuoteNo").toString();
			
			log.info("RequestRefNo : "+reqRefNo+" ||  MerchantReference : "+marchantRefNo+" || QuoteNo : "+quoteNo+" ");
			
			log.info("INSERT PAYMENT BLOCK END : "+new Date());
			
			//==============================INSERT PAYMENT BLOCK END=============================================
			
			//==============================PAYMENT LINK BLOCK START=============================================
			log.info("PAYMENT LINK BLOCK START : "+new Date());
			
		    Map<String,Object> paymentMap = new HashMap<>();
		    paymentMap.put("MerchantRefNo", marchantRefNo);
		    paymentMap.put("CompanyId", "100046");
		    paymentMap.put("WhatsappCode", "260");
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
			String apiResponse = thread.callZambiaComApi(motorDetailsApi, api_request);
			
			Map<String,Object> getMotorRes =mapper.readValue(apiResponse, Map.class);
			
			List<Map<String,Object>> motorRes =getMotorRes.get("Result")==null?null:
				mapper.readValue(mapper.writeValueAsString(getMotorRes.get("Result")), List.class);
			
			log.info("ALL MOTOR DETAILS :" +motorRes);
			
			Map<String,Object> mot = motorRes.get(0);
			
			botResponceData.put("RegNo", mot.get("Registrationnumber")==null?"N/A":mot.get("Registrationnumber"));
			botResponceData.put("VehicleUsage", mot.get("MotorUsageDesc")==null?"N/A":mot.get("MotorUsageDesc"));
			botResponceData.put("BodyType", mot.get("VehicleTypeDesc")==null?"N/A":mot.get("VehicleTypeDesc"));
		//	botResponceData.put("color",mot.get("ColorDesc")==null?"N/A":mot.get("ColorDesc"));
		//	botResponceData.put("InsuranceClass",insuranceClass);
		//	botResponceData.put("Premium", pre);
			botResponceData.put("url", paymentUrl);
		//	botResponceData.put("VatAmt", vatTaxs);
		//	botResponceData.put("SumInsured", mot.get("SumInsured")==null?"N/A":mot.get("SumInsured"));
		//	botResponceData.put("chassis", mot.get("Chassisnumber")==null?"N/A":mot.get("Chassisnumber"));
		//	botResponceData.put("VatPercentage", String.valueOf(vatPercentage.longValue()));
			botResponceData.put("TotalPremium", totalPremium);
		//	botResponceData.put("InceptionDate", policyDate);
		//	botResponceData.put("ExpiryDate",policyEndDate);
			botResponceData.put("ReferenceNo", reqRefNo);
			botResponceData.put("Model", mot.get("VehcilemodelDesc")==null?"N/A":mot.get("VehcilemodelDesc"));
			botResponceData.put("Make", mot.get("VehiclemakeDesc")==null?"N/A":mot.get("VehiclemakeDesc"));
			botResponceData.put("CustomerName", customerName);
			botResponceData.put("QuoteNo", quoteNo);
			
			
			
			return botResponceData;
	}

}
