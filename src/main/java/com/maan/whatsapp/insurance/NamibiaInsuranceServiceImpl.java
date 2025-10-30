package com.maan.whatsapp.insurance;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
import com.maan.whatsapp.service.common.CommonService;


@Service
@PropertySource("classpath:WebServiceUrl.properties")
public class NamibiaInsuranceServiceImpl implements NamibiaInsuranceService{
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private Gson objectPrint;
	
	@Autowired
	private CommonService cs;
	
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
	
	@Value("${namibia.message.api}")
	private String namibiaMsgApi;
	
	@Value("${nambia.message.auth}")
	private String namibiaMsgAuth;
	
	@Autowired
	private PhoenixUserDataDetailsRepo userDataRepo;
	
	@SuppressWarnings("unused")
	@Override
	public Object generateNamibiaQuote(InsuranceReq req) throws JsonProcessingException,WhatsAppValidationException{
		String response = "";
		String exception = "";
		List<Error> errorList = new ArrayList<>(2);
		Map<String,Object> botResponceData = new HashMap<String,Object>();
		
		String flowDatas = new String(Base64.getDecoder().decode(req.getQuote_form()));
		
		Map<String,Object> requestDatas = mapper.readValue(flowDatas, Map.class);
		
		//Customer page Datas
		String title = requestDatas.get("title") == null ? "" : requestDatas.get("title").toString();
		String customerName = requestDatas.get("customer_name") == null ? "" : requestDatas.get("customer_name").toString();
		String gender = requestDatas.get("gender") == null ? "" : requestDatas.get("gender").toString();
		String occupation = requestDatas.get("occupation") == null ? "" : requestDatas.get("occupation").toString();
		String mobileNo = requestDatas.get("mobile_number") == null ? "" : requestDatas.get("mobile_number").toString();
		String email = requestDatas.get("email_id") == null ? "" : requestDatas.get("email_id").toString();
		String idType = requestDatas.get("id_type") == null ? "" : requestDatas.get("id_type").toString();
		String idNumber = requestDatas.get("id_number") == null ? "" : requestDatas.get("id_number").toString();
		String region = requestDatas.get("region") == null ? "" : requestDatas.get("region").toString();
		String distict = requestDatas.get("district") == null ? "" : requestDatas.get("district").toString();
		String address = requestDatas.get("address") == null ? "" : requestDatas.get("address").toString();
		
		//Vehicle Page Datas
		String motorUsage = requestDatas.get("Motorusage") == null ? "" : requestDatas.get("Motorusage").toString();
		String bodyType = requestDatas.get("VehicleType") == null ? "" : requestDatas.get("VehicleType").toString();
		String make = requestDatas.get("Vehiclemake") == null ? "" : requestDatas.get("Vehiclemake").toString();
		String model = requestDatas.get("Vehcilemodel") == null ? "" : requestDatas.get("Vehcilemodel").toString();
		String regNo = requestDatas.get("Registrationnumber") == null ? "" : requestDatas.get("Registrationnumber").toString();
		String chassisNo = requestDatas.get("Chassisnumber") == null ? "" : requestDatas.get("Chassisnumber").toString();
		String engineNo = requestDatas.get("EngineNumber") == null ? "" : requestDatas.get("EngineNumber").toString();
		String enginecapacity = requestDatas.get("ResEngineCapacity") == null ? "" : requestDatas.get("ResEngineCapacity").toString();
		String seatingCapacity = requestDatas.get("SeatingCapacity") == null ? "" : requestDatas.get("SeatingCapacity").toString();
		String manYear = requestDatas.get("ManufactureYear") == null ? "" : requestDatas.get("ManufactureYear").toString();
		String color = requestDatas.get("Color") == null ? "" : requestDatas.get("Color").toString();
		String grossWeight = requestDatas.get("Grossweight") == null ? "" : requestDatas.get("Grossweight").toString();
		
		//Policy Page datas 
		String insuranceClass = requestDatas.get("insurance_class") == null ? "" : requestDatas.get("insurance_class").toString();
		String inusredPeriod = requestDatas.get("insured_period") == null ? "" : requestDatas.get("insured_period").toString();
		String policyDate = requestDatas.get("policy_start_date") == null ? "" : requestDatas.get("policy_start_date").toString();
		
		//DOB calc
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		LocalDate curDate = LocalDate.parse(policyDate,formatter);
		LocalDate minusDate = curDate.minusYears(18);
		String cusDob = minusDate.format(formatter);
		
		if("COMP".equalsIgnoreCase(insuranceClass)) {
			insuranceClass = "1";
		}else if("TPFT".equalsIgnoreCase(insuranceClass)) {
			insuranceClass = "2";
		}else if("TPO".equalsIgnoreCase(insuranceClass)) {
			insuranceClass = "3";
		}
		
		String noClaimBonus=null,accessoriesSumInsured=null,windShieldSumInsured=null,extendedTppdSumInsured=null,sumInsured=null;
		
		if(insuranceClass.equals("1")) {
			String comp_vehicle_si = requestDatas.get("comp_vehicle_si")==null?"":requestDatas.get("comp_vehicle_si").toString();
			String comp_noclaim_bonus = requestDatas.get("comp_noclaim_bonus")==null?"":requestDatas.get("comp_noclaim_bonus").toString();
			String comp_accessories_sumInured = requestDatas.get("comp_accessories_sumInured")==null?"":requestDatas.get("comp_accessories_sumInured").toString();
			String comp_windShield_sumInured = requestDatas.get("comp_windShield_sumInured")==null?"":requestDatas.get("comp_windShield_sumInured").toString();
			String comp_extended_tppd_sumInsured = requestDatas.get("comp_extended_tppd_sumInsured")==null?"":requestDatas.get("comp_extended_tppd_sumInsured").toString();
			sumInsured = comp_vehicle_si;
			noClaimBonus = comp_noclaim_bonus;
			accessoriesSumInsured = comp_accessories_sumInured;
			windShieldSumInsured = comp_windShield_sumInured;
			extendedTppdSumInsured = comp_extended_tppd_sumInsured;
		}else if(insuranceClass.equals("2")) {
			String tpft_vehicle_si = requestDatas.get("tpft_vehicle_si")==null?"":requestDatas.get("tpft_vehicle_si").toString();
			String tpft_noclaim_bonus = requestDatas.get("tpft_noclaim_bonus")==null?"":requestDatas.get("tpft_noclaim_bonus").toString();
			String tpft_accessories_sumInured = requestDatas.get("tpft_accessories_sumInured")==null?"":requestDatas.get("tpft_accessories_sumInured").toString();
			String tpft_windShield_sumInured = requestDatas.get("tpft_windShield_sumInured")==null?"":requestDatas.get("tpft_windShield_sumInured").toString();
			String tpft_extended_tppd_sumInsured = requestDatas.get("tpft_extended_tppd_sumInsured")==null?"":requestDatas.get("tpft_extended_tppd_sumInsured").toString();
			sumInsured = tpft_vehicle_si;
			noClaimBonus = tpft_noclaim_bonus;
			accessoriesSumInsured = tpft_accessories_sumInured;
			windShieldSumInsured = tpft_windShield_sumInured;
			extendedTppdSumInsured = tpft_extended_tppd_sumInsured;
		}
		
		//==============================SAVE VEHICLE INFO BLOCK START=============================================
		log.info("SAVE VEHICLE INFO START: "+new Date());
		
		Map<String,Object> vehicleInfo = new HashMap<String,Object>();
		vehicleInfo.put("Insuranceid", "100050");
		vehicleInfo.put("BranchCode", "120");
		vehicleInfo.put("AxelDistance", 1);
		vehicleInfo.put("Chassisnumber", chassisNo);
		vehicleInfo.put("Color", color);
		vehicleInfo.put("CreatedBy", "");
		vehicleInfo.put("EngineNumber", engineNo);
		vehicleInfo.put("FuelType", null);
		vehicleInfo.put("Grossweight", grossWeight);
		vehicleInfo.put("ManufactureYear", manYear);
		vehicleInfo.put("MotorCategory", null);
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
		vehicleInfo.put("Vehcilemodel", "");
		vehicleInfo.put("VehcilemodelId", model);
		vehicleInfo.put("VehicleType", bodyType);
		vehicleInfo.put("Vehiclemake", null);
		vehicleInfo.put("VehiclemakeId", make);
		vehicleInfo.put("DisplacementInCM3", null);
		vehicleInfo.put("NumberOfCylinders", null);
		vehicleInfo.put("PlateType", null);
		
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
		
				
		//==============================SAVE VEHICLE INFO BLOCK END=============================================
		
		//==============================CUSTOMER CREATION BLOCK START=============================================
		log.info("CUSTOMER CREATION BLOCK START: "+new Date());
		Map<String,Object> customerCreation = new HashMap<String,Object>();
		customerCreation.put("Activities", "");
		customerCreation.put("Address1", address);
		customerCreation.put("Address2", "");
		customerCreation.put("AppointmentDate", "");
		customerCreation.put("BranchCode", "120");
		customerCreation.put("BrokerBranchCode", "1");
		customerCreation.put("BusinessType", null);
		customerCreation.put("CityCode", "1");
		customerCreation.put("CityName", "");//check district or region
		customerCreation.put("ClientName", customerName);
		customerCreation.put("Clientstatus", "Y");
		customerCreation.put("Country", "NAM");
		customerCreation.put("CountryName", "Namibia");
		customerCreation.put("CreatedBy", "Wh_Nam_Broker");
		customerCreation.put("CustomerAsInsurer", "N");
		customerCreation.put("CustomerReferenceNo", "");
		customerCreation.put("DobOrRegDate", cusDob);
		customerCreation.put("Email1", email);
		customerCreation.put("Email2", null);
		customerCreation.put("Email3", null);
		customerCreation.put("ExpiryDate", null);
		customerCreation.put("Fax", null);
		customerCreation.put("Gender", gender);
		customerCreation.put("IdNumber", idNumber);
		customerCreation.put("IdType", idType);
		customerCreation.put("InsuranceId", "100050");
		customerCreation.put("IsTaxExempted", "N");
		customerCreation.put("Language", "1");
		customerCreation.put("LastName", "");
		customerCreation.put("MaritalStatus", "");//check
		customerCreation.put("MiddleName", "");
		customerCreation.put("MobileCode1", "264");
		customerCreation.put("MobileCodeDesc1", "1");
		customerCreation.put("MobileNo1", mobileNo);
		customerCreation.put("MobileNo2", "");
		customerCreation.put("MobileNo3", null);
		customerCreation.put("Nationality", "");
		customerCreation.put("Occupation", occupation);
		customerCreation.put("OtherOccupation", "");
		customerCreation.put("PhoneNoCode", "");
		customerCreation.put("PinCode", "");
		customerCreation.put("Placeofbirth", address);
		customerCreation.put("PolicyHolderType", "1");
		customerCreation.put("PolicyHolderTypeid", idType);
		customerCreation.put("PreferredNotification", "sms");
		customerCreation.put("ProductId", "5");
		customerCreation.put("RegionCode", region);
		customerCreation.put("RiskAssessmentDate", null);
		customerCreation.put("SaveOrSubmit", "Save");
		customerCreation.put("SocioProfessionalCategory", null);
		customerCreation.put("StateCode", distict);
		customerCreation.put("StateName", null);
		customerCreation.put("Status", "Y");
		customerCreation.put("Street", address);
		customerCreation.put("TaxExemptedId", null);
		customerCreation.put("TelephoneNo1", null);
		customerCreation.put("TelephoneNo2", null);
		customerCreation.put("TelephoneNo3", null);
		customerCreation.put("Title", title);
		customerCreation.put("Type", null);
		customerCreation.put("VipFlag", null);
		customerCreation.put("VrTinNo", null);
		customerCreation.put("WhatsappCode", "264");
		customerCreation.put("WhatsappDesc", "1");
		customerCreation.put("WhatsappNo", mobileNo);
		customerCreation.put("Zone", "1");
		
		String custRefNo = "";
		
		try {
			String cusReq = mapper.writeValueAsString(customerCreation);
			String cusSaveApi = saveCustomerApi;
			log.info("Customer Save Calling: "+cusSaveApi);
			
			String apiResponse = thread.callNamibiaComApi(cusSaveApi,cusReq);
			
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
		
		//==============================SHOW VEHICLE INFO BLOCK START=============================================
		log.info("SHOW VEHICLE INFO BLOCK START: "+new Date());
		Map<String,Object> vehInfo = new HashMap<String,Object>();
		vehInfo.put("BranchCode", "120");
		vehInfo.put("BrokerBranchCode", "1");
		vehInfo.put("CreatedBy", "Wh_Nam_Broker");
		vehInfo.put("InsuranceId", "100050");
		vehInfo.put("ProductId", "5");
		vehInfo.put("ReqChassisNumber", "");
		vehInfo.put("ReqRegNumber", regNo);
		vehInfo.put("SavedFrom", "API");
		
		Map<String,Object> showVehResult = null;
		try {
			String showInfo = mapper.writeValueAsString(vehInfo);
			String showVehApi = showVehicleInfoApi;
			
			log.info("Show Vehicle Api Calling: "+showVehApi);
			
			String apiResponse = thread.callNamibiaComApi(showVehApi, showInfo);
			
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
		motorSave.put("AcccessoriesSumInsured", accessoriesSumInsured);
		motorSave.put("AccessoriesInformation", "");
		motorSave.put("AdditionalCircumstances", "");
		motorSave.put("AgencyCode", "13506");//local-13506,
		motorSave.put("AggregatedValue", null);
		motorSave.put("ApplicationId", "1");
		motorSave.put("AxelDistance", 1);
		motorSave.put("BankingDelegation", "");
		motorSave.put("BdmCode", "5467546");//local-5467546
		motorSave.put("BorrowerType", null);
		motorSave.put("BranchCode", "120");
		motorSave.put("BrokerBranchCode", "1");
		motorSave.put("BrokerCode", "13506");//local-13506
		motorSave.put("Chassisnumber", chassisNo);
		motorSave.put("CityLimit", null);
		motorSave.put("ClaimType", noClaimBonus);
		motorSave.put("ClaimTypeDesc", noClaimBonus);
		motorSave.put("CollateralCompanyAddress", "");
		motorSave.put("CollateralCompanyName", "");
		motorSave.put("CollateralName", null);
		motorSave.put("CollateralYn", "N");
		motorSave.put("Color", color);
		motorSave.put("ColorDesc", color);
		motorSave.put("CommissionType", null);
		motorSave.put("CoverNoteNo", null);
		motorSave.put("CreatedBy", "Wh_Nam_Broker");
		motorSave.put("CubicCapacity", enginecapacity);
		motorSave.put("Currency", "NAD");
		motorSave.put("CustomerCode", "5467546");//local-5467546
		motorSave.put("CustomerName", customerName);
		motorSave.put("CustomerReferenceNo", custRefNo);
		motorSave.put("DateOfCirculation", null);
		motorSave.put("Deductibles", null);
		motorSave.put("DefenceValue", null);
		motorSave.put("DisplacementInCM3", null);
		motorSave.put("DrivenByDesc", "D");
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
		motorSave.put("ExchangeRate", "1.0");
		motorSave.put("FirstLossPayee", null);
		motorSave.put("FleetOwnerYn", "N");
		motorSave.put("FuelType", null);
		motorSave.put("Gpstrackinginstalled", "N");
		motorSave.put("Grossweight", grossWeight);
		motorSave.put("HavePromoCode", "N");
		motorSave.put("HoldInsurancePolicy", "N");
		motorSave.put("HorsePower", "0");
		motorSave.put("Idnumber", idNumber);
		motorSave.put("Inflation", null);
		motorSave.put("InsuranceClass", insuranceClass);
		motorSave.put("InsuranceClassDesc", null);
		motorSave.put("InsuranceId", "100050");
		motorSave.put("Insurancetype", "103");//103 check
		motorSave.put("InsurancetypeDesc", null);
		motorSave.put("InsurerSettlement", "");
		motorSave.put("InterestedCompanyDetails", "");
		motorSave.put("IsFinanceEndt", null);
		motorSave.put("LoanAmount", null);
		motorSave.put("LoanEndDate", null);
		motorSave.put("LoanStartDate", null);
		motorSave.put("LocationId", "1");
		motorSave.put("LoginId", "");//login
		motorSave.put("ManufactureYear", manYear);
		motorSave.put("MarketValue", null);
		motorSave.put("Mileage", null);
		motorSave.put("MobileCode", "264");
		motorSave.put("MobileNumber", mobileNo);
		motorSave.put("ModelNumber", null);
		motorSave.put("MotorCategory", null);
		motorSave.put("Motorusage", "");
		motorSave.put("MotorusageId", motorUsage);
		motorSave.put("MunicipalityTraffic", null);
		motorSave.put("NcdYn", "N");
		motorSave.put("NewValue", null);
		motorSave.put("NoOfClaimYears", null);
		motorSave.put("NoOfClaims", null);
		motorSave.put("NoOfComprehensives", "0");
		motorSave.put("NoOfFemale", null);
		motorSave.put("NoOfMale", null);
		motorSave.put("NoOfPassengers", null);
		motorSave.put("NoOfVehicles", "1");
		motorSave.put("NumberOfAxels", null);
		motorSave.put("NumberOfCards", null);
		motorSave.put("NumberOfCylinders", null);
		motorSave.put("OrginalPolicyNo", null);
		motorSave.put("OwnerCategory", null);
		motorSave.put("PaCoverId", "0");
		motorSave.put("PlateType", null);
		motorSave.put("PolicyEndDate", policyEndDate);
		motorSave.put("PolicyRenewalYn", "N");
		motorSave.put("PolicyStartDate", policyDate);
		motorSave.put("PolicyType", "0");
		motorSave.put("PreviousInsuranceYN", null);
		motorSave.put("PreviousLossRatio", null);
		motorSave.put("ProductId", "5");
		motorSave.put("PromoCode", null);
		motorSave.put("QuoteExpiryDays", null);
		motorSave.put("RadioOrCasseteplayer", null);
		motorSave.put("RegistrationDate", null);
		motorSave.put("RegistrationYear", "");//check
		motorSave.put("Registrationnumber", regNo);
		motorSave.put("RequestReferenceNo", "");
		motorSave.put("RoofRack", null);
		motorSave.put("SaveOrSubmit", "Save");
		motorSave.put("SavedFrom", "Web");
		motorSave.put("SearchFromApi", false);
		motorSave.put("SeatingCapacity", seatingCapacity);
		motorSave.put("SectionId", Arrays.asList("103"));
		motorSave.put("SourceType", "Broker");
		motorSave.put("SourceTypeId", "Broker");
		motorSave.put("SpotFogLamp", null);
		motorSave.put("Status", "Y");
		motorSave.put("Stickerno", null);
		motorSave.put("SubUserType", "Broker");
		motorSave.put("SumInsured", sumInsured);
		motorSave.put("Tareweight", null);
		motorSave.put("TiraCoverNoteNo", null);
		motorSave.put("TppdFreeLimit", null);
		motorSave.put("TppdIncreaeLimit", null);
		motorSave.put("TrailerDetails", null);
		motorSave.put("TransportHydro", null);
		motorSave.put("UsageId", null);
		motorSave.put("UserType", "Broker");
		motorSave.put("Vehcilemodel", "");
		motorSave.put("VehcilemodelId", "");
		motorSave.put("VehicleId", "1");
		motorSave.put("VehicleType", "");
		motorSave.put("VehicleTypeId", "");
		motorSave.put("VehicleTypeIvr", "");
		motorSave.put("VehicleValueType", null);
		motorSave.put("Vehiclemake", "");
		motorSave.put("VehiclemakeId", "");
		motorSave.put("WindScreenSumInsured", windShieldSumInsured);
		motorSave.put("Windscreencoverrequired", "N");
		motorSave.put("Zone", "1");
		motorSave.put("ZoneCirculation", null);
		motorSave.put("accident", null);
		motorSave.put("periodOfInsurance", inusredPeriod);
		LinkedHashMap<String, Object> exchangeRateScenario = new  LinkedHashMap<>();
		exchangeRateScenario.put("OldAcccessoriesSumInsured", null);
		exchangeRateScenario.put("OldCurrency", "NAD");
		exchangeRateScenario.put("OldExchangeRate", "1.0");
		exchangeRateScenario.put("OldSumInsured", null);
		exchangeRateScenario.put("OldTppdIncreaeLimit", null);
		exchangeRateScenario.put("OldWindScreenSumInsured", null);
		
		LinkedHashMap<String, Object> excahnge = new  LinkedHashMap<>();
		excahnge.put("ExchangeRateScenario", exchangeRateScenario);
		motorSave.put("Scenarios", excahnge);
		
		Map<String,Object> saveMotResult = null;
		Map<String,Object> saveMotorRes = new HashMap<String,Object>();
		String reqRefNo = "";
		try {
			String motorSaveReq = mapper.writeValueAsString(motorSave);
			String saveMotorApi = motorSaveApi;
			log.info("Save Motor Api Calling: "+saveMotorApi);
			
			String apiResponse = thread.callNamibiaComApi(saveMotorApi,motorSaveReq);
			
			log.info("Save Motor Response: "+apiResponse);
			
			Map<String,Object> saveMot = mapper.readValue(apiResponse, Map.class);
			saveMotResult = saveMot.get("Result") == null ? null : (Map<String, Object>) saveMot.get("Result");
			
			reqRefNo = saveMotResult.get("RequestReferenceNo") == null ? "" : saveMotResult.get("RequestReferenceNo").toString();
				
			if(saveMotResult == null) {
				String errorMessgae = saveMot.get("ErrorMessage") == null ? "" : saveMot.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		log.info("MOTOR SAVE BLOCK END: "+new Date());
		//==============================MOTOR SAVE BLOCK END=============================================
		
		//==============================CALC BLOCK START=============================================
		log.info("CALC BLOCK START: "+new Date());		
		
		Map<String,Object> calc = new HashMap<String,Object>();
		calc.put("AgencyCode", "13506");//local-13506
		calc.put("BranchCode", "120");
		calc.put("CdRefNo", saveMotResult.get("CdRefNo") == null ? "" : saveMotResult.get("CdRefNo"));
		calc.put("CoverModification", "N");
		calc.put("CreatedBy", "Wh_Nam_Broker");
		calc.put("DdRefNo", saveMotResult.get("DdRefNo") == null ? "" : saveMotResult.get("DdRefNo"));
		calc.put("EffectiveDate", policyDate);
		calc.put("InsuranceId", "100050");
		calc.put("LocationId", "1");
		calc.put("MSRefNo", saveMotResult.get("MSRefNo") == null ? "" : saveMotResult.get("MSRefNo"));
		calc.put("PolicyEndDate", policyEndDate);
		calc.put("ProductId", "5");
		calc.put("RequestReferenceNo", reqRefNo);
		calc.put("SectionId", saveMotResult.get("SectionId")==null?"":saveMotResult.get("SectionId"));
		calc.put("VdRefNo", saveMotResult.get("VdRefNo")==null?"":saveMotResult.get("VdRefNo"));
		calc.put("VehicleId", "1");
		calc.put("productId", "5");
		
		List<Map<String,Object>> coverList =null;
		Map<String,Object> calcRes=null;
		Map<String,Object> calcResult = null;
		try {
			String calReq = mapper.writeValueAsString(calc);
			
			String calculatorApi = calcApi;
			String apiResponse = thread.callNamibiaComApi(calculatorApi, calReq);
			
			log.info("Save Motor Response: "+apiResponse);
			
			 calcRes = mapper.readValue(apiResponse, Map.class);
			 coverList = calcRes.get("CoverList") == null ? null :
				 mapper.readValue(mapper.writeValueAsString(calcRes.get("CoverList")), List.class);
			 
			calcResult = calcRes.get("Result") == null ? null : (Map<String, Object>) calcRes.get("Result");
			
			if(calcResult == null) {
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
		
		vatPercentage = tax.get(1).get("TaxRate")==null?0L:Double.valueOf(tax.get(1).get("TaxRate").toString());
		
		premium = pre.longValue();
		
		Long totalPremium =pre.longValue()+vatTax.longValue();
				
		log.info("CALC BLOCK END: "+new Date());
		
		//==============================CALC BLOCK END=============================================================
		
		//==============================USER CREATION BLOCK START=============================================
		
		log.info("USER CREATION BLOCK START: "+new Date());
		
		Map<String,Object> userCreationMap = new HashMap<String,Object>();
		userCreationMap.put("CompanyId", "100050");
		userCreationMap.put("CustomerId", custRefNo);
		userCreationMap.put("ProductId", "5");
		userCreationMap.put("ReferenceNo", reqRefNo);
		userCreationMap.put("UserMobileNo", mobileNo);
		userCreationMap.put("UserMobileCode", "264");
		userCreationMap.put("AgencyCode", "13506"); //local-13506
		
		Map<String,Object> userResult = null;
		Map<String,Object> userRes = null;
		try {
			String userCreationReq = mapper.writeValueAsString(userCreationMap);
			String userCreationApi = loginCreationApi;
			
			log.info("USER CREATION API: "+userCreationApi);
			
			String apiResponse = thread.callNamibiaComApi(userCreationApi, userCreationReq);
			
			log.info("USER CREATION RESPONSE: "+apiResponse);
			
			 userRes = mapper.readValue(apiResponse, Map.class);
			
			userResult = userRes.get("Result") == null ? null : (Map<String, Object>) userRes.get("Result");
			
			if(userResult == null) {
				String errorMessgae = calcRes.get("ErrorMessage") == null ? "" : calcRes.get("ErrorMessage").toString();
				response = errorMessgae;
				return response;
			}
				
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
		vehicleMap.put("SectionId", saveMotResult.get("SectionId")==null?"":saveMotResult.get("SectionId"));
		vehicleMap.put("Id", "1");
		vehicleMap.put("LocationId", "1");
		vehicleMap.put("Covers", buyCovers);
		List<Map<String,Object>> vehiMapList =new ArrayList<Map<String,Object>>();
		vehiMapList.add(vehicleMap);
		
		Map<String,Object> buypolicyMap =new HashMap<String,Object>();
		buypolicyMap.put("RequestReferenceNo", reqRefNo);
		buypolicyMap.put("CreatedBy", "Wh_Nam_Broker");
		buypolicyMap.put("ProductId", "5");
		buypolicyMap.put("ManualReferralYn", "N");
		buypolicyMap.put("Vehicles", vehiMapList);
		
		Map<String,Object> buyPolicyResult = null;
		Map<String,Object> buyPolicyRes = null;
		try {
			String buypolicyReq =objectPrint.toJson(buypolicyMap);
			String buyPolicyApi = buyPolicy;
			
			log.info("BUY POLICY API: "+buyPolicyApi);
			
			String apiResponse = thread.callNamibiaComApi(buyPolicyApi, buypolicyReq);
			
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
		makePaymentMap.put("CreatedBy", "Wh_Nam_Broker");
		makePaymentMap.put("EmiYn", "N");
		makePaymentMap.put("InstallmentMonth", null);
		makePaymentMap.put("InstallmentPeriod", null);
		makePaymentMap.put("InsuranceId", "100050");
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
			String apiResponse = thread.callNamibiaComApi(makePaymentApi, makePayemantReq);
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
		insertPaymentMap.put("CreatedBy", "Wh_Nam_Broker");
		insertPaymentMap.put("EmiYn", "N");
		insertPaymentMap.put("IbanNumber", null);
		insertPaymentMap.put("InsuranceId", "100050");
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
			String apiResponse = thread.callNamibiaComApi(insertPaymentApi, insertPaymentReq);
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
	    paymentMap.put("CompanyId", "100050");
	    paymentMap.put("WhatsappCode", "264");
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
		String apiResponse = thread.callNamibiaComApi(motorDetailsApi, api_request);
		
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
	public Object insuranceFlowNamibia(Object req)
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
		titleMap.put("InsuranceId", "100050");

		try {
			String titileReq = mapper.writeValueAsString(titleMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: " + masterApi);

			String apiResponse = thread.callNamibiaComApi(masterApi, titileReq);

			log.info("Title Id Response: " + apiResponse);
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
		genderMap.put("InsuranceId", "100050");

		try {
			String genderReq = mapper.writeValueAsString(genderMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: " + masterApi);

			String apiResponse = thread.callNamibiaComApi(masterApi, genderReq);

			log.info("Gender Response: " + apiResponse);
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
		occupationMap.put("InsuranceId", "100050");
		
		try {
			String occupationReq = mapper.writeValueAsString(occupationMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: " + masterApi);

			String apiResponse = thread.callNamibiaComApi(masterApi, occupationReq);
			
			log.info("Occupation Id Response: " + apiResponse);
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
		idTypeMap.put("InsuranceId", "100050");
		
		try {
			String idTypeReq = mapper.writeValueAsString(idTypeMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: " + masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, idTypeReq);
			
			log.info("ID type Response: " + apiResponse);
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
		regionMap.put("CountryCode", "NAM");
		
		try {
			String regionReq = mapper.writeValueAsString(regionMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, regionReq);
			
			log.info("Region id Response: " + apiResponse);
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
		districtMap.put("CountryCode", "NAM");
		
		try {
			String districtReq = mapper.writeValueAsString(districtMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, districtReq);
			
			log.info("District Response: " + apiResponse);
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
		insuranceClassMap.put("InsuranceId", "100050");
		
		try {
			String insuranceClassReq = mapper.writeValueAsString(insuranceClassMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, insuranceClassReq);
			
			log.info("Insurance Class Response: " + apiResponse);
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
		motorusageMap.put("InsuranceId", "100050");
		
		try {
			String motorUsageReq = mapper.writeValueAsString(motorusageMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, motorUsageReq);
			
			log.info("Motor Usage Response: " + apiResponse);
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
		bodyTypeMap.put("InsuranceId", "100050");
		
		try {
			String bodyTypeReq = mapper.writeValueAsString(bodyTypeMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, bodyTypeReq);
			
			log.info("BodyType Response: " + apiResponse);
			
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
		makeMap.put("InsuranceId", "100050");
		
		try {
			String makeReq = mapper.writeValueAsString(makeMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, makeReq);
			
			log.info("Make Id Response: " + apiResponse);
			
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
		modelMap.put("InsuranceId", "100050");
		
		try {
			String modelReq = mapper.writeValueAsString(modelMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, modelReq);
			
			log.info("Model Id Response: " + apiResponse);
			
			Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
			
			Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
				mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
			
			if(masterApiResult.isEmpty() || masterApiResult == null) {
				String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
				response = errorMessage;
				return response;
			}else {
				modelId = masterApiResult.get("Response") == null ? "99999" : masterApiResult.get("Response").toString();
			}
		}catch(Exception e) {
			e.printStackTrace();
			log.info(e.getMessage());
		}
		
		//colorId
		Map<String,Object> colorMap = new HashMap<>();
		colorMap.put("Desc", color);
		colorMap.put("MasterType", "VEHICLE_COLOR");
		colorMap.put("InsuranceId", "100050");
		
		try {
			String colorReq = mapper.writeValueAsString(colorMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, colorReq);
			
			log.info("Color Id Response: " + apiResponse);
			
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
		vehicleInfo.put("Insuranceid", "100050");
		vehicleInfo.put("BranchCode", "120");
		vehicleInfo.put("AxelDistance", 1);
		vehicleInfo.put("Chassisnumber", chassisNo);
		vehicleInfo.put("Color", colorId);
		vehicleInfo.put("CreatedBy", "Wh_Nam_Broker");
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
			String vehResponse = thread.callNamibiaComApi(saveVehicleInfoApi,vehReq);
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
		customerCreation.put("BranchCode", "120");
		customerCreation.put("BrokerBranchCode", "1");
		customerCreation.put("BusinessType", null);
		customerCreation.put("CityCode", distictId);
		customerCreation.put("CityName", region);//check district or region
		customerCreation.put("ClientName", customerName);
		customerCreation.put("Clientstatus", "Y");
		customerCreation.put("Country", "NAM");
		customerCreation.put("CountryName", "Namibia");
		customerCreation.put("CreatedBy", "Wh_Nam_Broker");//create login for whatsapp bot
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
		customerCreation.put("InsuranceId", "100050");
		customerCreation.put("IsTaxExempted", "N");
		customerCreation.put("Language", "1");
		customerCreation.put("LastName", "");
		customerCreation.put("MaritalStatus", "Single");//check
		customerCreation.put("MiddleName", "");
		customerCreation.put("MobileCode1", "264");
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
		customerCreation.put("WhatsappCode", "264");
		customerCreation.put("WhatsappDesc", "1");
		customerCreation.put("WhatsappNo", mobileNo);
		customerCreation.put("Zone", "1");
		
		String custRefNo = "";
		
		try {
			String cusReq = mapper.writeValueAsString(customerCreation);
			String cusSaveApi = saveCustomerApi;
			log.info("Customer Save Calling: "+cusSaveApi);
			log.info("Customer Save Request: "+cusReq);
			
			String apiResponse = thread.callNamibiaComApi(cusSaveApi,cusReq);
			
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
		vehInfo.put("BranchCode", "120");
		vehInfo.put("BrokerBranchCode", "1");
		vehInfo.put("CreatedBy", "Wh_Nam_Broker");
		vehInfo.put("InsuranceId", "100050");
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
			
			String apiResponse = thread.callNamibiaComApi(showVehApi, showInfo);
			
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
		motorSave.put("AgencyCode", "14130");//local-13495,UAT-14130
		motorSave.put("AggregatedValue", null);
		motorSave.put("ApplicationId", "1");
		motorSave.put("AxelDistance", 1);
		motorSave.put("BankingDelegation", "");
		motorSave.put("BdmCode", "2000010");//uAt-5467546,local-5555555
		motorSave.put("BorrowerType", null);
		motorSave.put("BranchCode", "120");
		motorSave.put("BrokerBranchCode", "1");
		motorSave.put("BrokerCode", "14130");//local-13495,UAT-14130
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
		motorSave.put("CreatedBy", "Wh_Nam_Broker");//broker id
		motorSave.put("CubicCapacity", enginecapacity);//doubt
		motorSave.put("Currency", "NAD");
		motorSave.put("CustomerCode", "2000010");//uAt-5467546,local-5555555
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
		motorSave.put("InsuranceId", "100050");
		motorSave.put("Insurancetype", insuranceClassId);//103 check
		motorSave.put("InsurancetypeDesc", insuranceClass);
		motorSave.put("InsurerSettlement", "");
		motorSave.put("InterestedCompanyDetails", "");
		motorSave.put("IsFinanceEndt", null);
		motorSave.put("LoanAmount", 0);
		motorSave.put("LoanEndDate", null);
		motorSave.put("LoanStartDate", null);
//		motorSave.put("LocationId", "1");
		motorSave.put("LoginId", "Wh_Nam_Broker");//login
		motorSave.put("ManufactureYear", manYear);
		motorSave.put("MarketValue", null);
		motorSave.put("Mileage", null);
		motorSave.put("MobileCode", "264");
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
//		motorSave.put("NoOfComprehensives", null);
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
//		motorSave.put("SourceType", "Broker");
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
		exchangeRateScenario.put("OldCurrency", "NAD");
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
			
			String apiResponse = thread.callNamibiaComApi(saveMotorApi,motorSaveReq);
			
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
		calc.put("AgencyCode", "14130");//local-13506,UAT-14130
		calc.put("BranchCode", "120");
		calc.put("CdRefNo", motorSaveResult.get("CdRefNo") == null ? "" : motorSaveResult.get("CdRefNo"));
		calc.put("CoverModification", "N");
		calc.put("CreatedBy", "Wh_Nam_Broker");
		calc.put("DdRefNo", motorSaveResult.get("DdRefNo") == null ? "" : motorSaveResult.get("DdRefNo"));
		calc.put("EffectiveDate", policyDate);
		calc.put("InsuranceId", "100050");
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
			String apiResponse = thread.callNamibiaComApi(calculatorApi, calReq);
			
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
		Double vatTax =0D;
		Double vatPercentage=0D;
		
		coverList = coverList.stream().filter(f -> "5".equalsIgnoreCase(f.get("CoverId").toString())).collect(Collectors.toList());
		List<Map<String,Object>> tax = (List<Map<String, Object>>) coverList.get(0).get("Taxes");
		
		BigDecimal pre = coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
				.map(m -> new BigDecimal(m.get("PremiumExcluedTaxLC").toString())).reduce(BigDecimal.ZERO, (x,y) -> x.add(y));
		
		BigDecimal totalPremium = coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
				.map(m -> new BigDecimal(m.get("PremiumIncludedTaxLC").toString())).reduce(BigDecimal.ZERO, (x,y) -> x.add(y));
		
		List<Map<String,Object>> taxList = coverList.parallelStream().filter(p -> ("B".equalsIgnoreCase(p.get("CoverageType").toString()) 
				|| "D".equalsIgnoreCase(p.get("isSelected").toString())) && p.get("Taxes") != null)
				.map(m -> (List<Map<String,Object>>) m.get("Taxes")).flatMap(f -> f.stream()).collect(Collectors.toList());
		
		 vatTax = taxList.stream().map(t -> t.get("TaxAmount") == null ? 0D : Double.valueOf(t.get("TaxAmount").toString()).longValue())
				.reduce(0D, (a,b) -> a + b);
		
		vatPercentage = tax.get(0).get("TaxRate")==null?0L:Double.valueOf(tax.get(0).get("TaxRate").toString());
		
		premium = pre.longValue();
		
		String taxVate = totalPremium.subtract(pre).toString();
		
		//Long totalPremium =pre.longValue()+vatTax.longValue();
				
		log.info("CALC BLOCK END: "+new Date());
		
		//==============================CALC BLOCK END=============================================================
		
		//==============================USER CREATION BLOCK START=============================================
		
		log.info("USER CREATION BLOCK START: "+new Date());
		
		Map<String,Object> userCreationMap = new HashMap<String,Object>();
		userCreationMap.put("CompanyId", "100050");
		userCreationMap.put("CustomerId", custRefNo);
		userCreationMap.put("ProductId", "5");
		userCreationMap.put("ReferenceNo", reqRefNo);
		userCreationMap.put("UserMobileNo", mobileNo);
		userCreationMap.put("UserMobileCode", "264");
		userCreationMap.put("AgencyCode", "14130"); //local-13506,UAT-14130
		
		Map<String,Object> userResult = null;
		Map<String,Object> userRes = null;
		try {
			String userCreationReq = mapper.writeValueAsString(userCreationMap);
			String userCreationApi = loginCreationApi;
			
			log.info("USER CREATION API: "+userCreationApi);
			
			String apiResponse = thread.callNamibiaComApi(userCreationApi, userCreationReq);
			
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
		buypolicyMap.put("CreatedBy", "Wh_Nam_Broker");
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
			
			String apiResponse = thread.callNamibiaComApi(buyPolicyApi, buypolicyReq);
			
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
		makePaymentMap.put("CreatedBy", "Wh_Nam_Broker");
		makePaymentMap.put("EmiYn", "N");
		makePaymentMap.put("InstallmentMonth", null);
		makePaymentMap.put("InstallmentPeriod", null);
		makePaymentMap.put("InsuranceId", "100050");
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
			String apiResponse = thread.callNamibiaComApi(makePaymentApi, makePayemantReq);
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
		insertPaymentMap.put("CreatedBy", "Wh_Nam_Broker");
		insertPaymentMap.put("EmiYn", "N");
		insertPaymentMap.put("IbanNumber", null);
		insertPaymentMap.put("InsuranceId", "100050");
		insertPaymentMap.put("MICRNo", null);
		insertPaymentMap.put("MobileCode1", null);
		insertPaymentMap.put("MobileNo1", null);
		insertPaymentMap.put("PayeeName", customerName);
		insertPaymentMap.put("PaymentId", makePaymentResult.get("PaymentId"));
		insertPaymentMap.put("PaymentType", "6");
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
			String apiResponse = thread.callNamibiaComApi(insertPaymentApi, insertPaymentReq);
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
	    paymentMap.put("CompanyId", "100050");
	    paymentMap.put("WhatsappCode", "264");
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
		String apiResponse = thread.callNamibiaComApi(motorDetailsApi, api_request);
		
		Map<String,Object> getMotorRes =mapper.readValue(apiResponse, Map.class);
		
		List<Map<String,Object>> motorRes =getMotorRes.get("Result")==null?null:
			mapper.readValue(mapper.writeValueAsString(getMotorRes.get("Result")), List.class);
		
		log.info("ALL MOTOR DETAILS :" +motorRes);
		
		Map<String,Object> mot = motorRes.get(0);
		
		botResponceData.put("registration", mot.get("Registrationnumber")==null?"N/A":mot.get("Registrationnumber"));
		botResponceData.put("usage", mot.get("MotorUsageDesc")==null?"N/A":mot.get("MotorUsageDesc"));
		botResponceData.put("vehtype", mot.get("VehicleTypeDesc")==null?"N/A":mot.get("VehicleTypeDesc"));
		//botResponceData.put("color",mot.get("ColorDesc")==null?"N/A":mot.get("ColorDesc"));
		//botResponceData.put("insurance_class",insuredClass);
		botResponceData.put("premium", premium);
		botResponceData.put("url", paymentUrl);
		botResponceData.put("vatamt", taxVate);
		botResponceData.put("suminsured", mot.get("SumInsured")==null?"N/A":mot.get("SumInsured"));
		botResponceData.put("chassis", mot.get("Chassisnumber")==null?"N/A":mot.get("Chassisnumber"));
		//botResponceData.put("vat", String.valueOf(vatPercentage.longValue()));
		botResponceData.put("totalpremium", totalPremium);
		botResponceData.put("inceptiondate", policyDate);
		botResponceData.put("expirydate",policyEndDate);
		botResponceData.put("referenceno", reqRefNo);
		//botResponceData.put("veh_model_desc", mot.get("VehcilemodelDesc")==null?"N/A":mot.get("VehcilemodelDesc"));
		botResponceData.put("veh_make_desc", mot.get("VehiclemakeDesc")==null?"N/A":mot.get("VehiclemakeDesc"));
		botResponceData.put("customer_name", customerName);
		
		//Template call
		
		String templateBody="*Phoenix Namibia Assurance Company Limited*\\n\\nHi *"+botResponceData.get("customer_name")+"*\\n*Your vehicle details* "
				+ "\\nRegistration No : "+botResponceData.get("registration")+"\\nMake : "+botResponceData.get("veh_make_desc")+""
						+ "\\nVehicle Usage : "+botResponceData.get("usage")+"\\nVehicle Type : "+botResponceData.get("vehtype")+""
								+ "\\n\\n*Your Premium Details*\\nPremium : "+botResponceData.get("premium")+"\\n"
										+ "Tax Amount : "+botResponceData.get("vatamt")+"\\nTotal Premium :"+botResponceData.get("totalpremium")+"\\n\\n"
												+ "*Policy Details*\\nInception Date:"+botResponceData.get("inceptiondate")+"\\nExpiry Date:"+botResponceData.get("expirydate")+"\\n"
														+ "Reference No  :  "+botResponceData.get("referenceno")+"\\n\\nPayment Link : "+botResponceData.get("url")+"";
		
		String mobNam = mobileNo;
		int count = mobNam.length();
		if(count == 9) {
			mobNam = "264"+mobNam;
		}else if(count == 10) {
			mobNam = "91"+mobNam;
		}
		
		Map<String,Object> tempMap = new HashMap<>();
		tempMap.put("messaging_product", "whatsapp");
		tempMap.put("recipient_type", "individual");
		tempMap.put("to", mobNam);
		tempMap.put("type", "text");
		
		Map<String,Object> textMap = new HashMap<>();
		textMap.put("body", templateBody);
		
		tempMap.put("text", textMap);
		
		String twoSlash=cs.getwebserviceurlProperty().getProperty("wa.hit.slash.two");
		String oneSlash=cs.getwebserviceurlProperty().getProperty("wa.hit.slash.one");
	
		String message_text =cs.reqPrint(tempMap);
		
		message_text =message_text.replace(twoSlash, oneSlash);
		
		String apiUrl = namibiaMsgApi;
		RestTemplate restTemp = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", namibiaMsgAuth);
		
		HttpEntity<String> request = new HttpEntity<>(message_text,headers);
		
		try {
			ResponseEntity<String> responses = restTemp.postForEntity(apiUrl, request, String.class);
			log.info("Template Api Response :"+responses.getBody());
			
		}catch(Exception e) {
			e.printStackTrace();
			log.info(e);
		}
		
		/*
		 * message_text =message_text.replace(twoSlash, oneSlash);
		 * 
		 * MediaType contentType =MediaType.parse("application/json"); RequestBody body
		 * =RequestBody.create(message_text,contentType);
		 */
		
		
		
		return botResponceData;


	}

	@Override
	public Object docResponseSetter(InsuranceReq req) {
        Map<String,Object> botResponceData = new HashMap<String,Object>();
		
		//String mobileNumber = new String(Base64.getDecoder().decode(req.getWhatsAppNo()));
        
        Long mobileNumber = Long.valueOf(req.getWhatsAppNo());
		
		try {
			PhoenixUserDataDetails getApiResonse = userDataRepo.findTop1ByWaidAndCompanyId(mobileNumber,"100050");
			String resp = getApiResonse.getFlowRequest();
			resp = resp.replace("/","");
			//resp = resp.replace("{", "");
			//resp = resp.replace("}", "");
			Map<String, Object> values = mapper.readValue(resp, Map.class);
			String val = values.get("Response").toString();
			Map<String,Object> value = mapper.readValue(val, Map.class);
			System.out.println(value);
			
			botResponceData.put("RegNo",value.get("VehicleRegistrationNumber") == null ? null : value.get("VehicleRegistrationNumber").toString());
			botResponceData.put("ChassisNo",value.get("VehicleIdendicationNumber") == null ? null : value.get("VehicleIdendicationNumber").toString());
			botResponceData.put("EngineNo",value.get("EngineNumber") == null ? null : value.get("EngineNumber").toString());
			botResponceData.put("Make",value.get("Make") == null ? null : value.get("Make").toString());
			botResponceData.put("Model",value.get("SeriesName") == null ? null : value.get("SeriesName").toString());
			botResponceData.put("BodyType",value.get("VehicleDescription") == null ? null : value.get("VehicleDescription").toString());
			botResponceData.put("Tare",value.get("Tare") == null ? null : value.get("Tare").toString());
			
			return botResponceData;
			
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object quoteGenerationNamibia(Object req)
			throws JsonProcessingException, JsonMappingException, WhatsAppValidationException {



		String response = "";
		String exception = "";
		List<Error> errorList = new ArrayList<>(2);
		Map<String, Object> botResponceData = new HashMap<String, Object>();

		Map<String, Object> data = mapper.convertValue(req, Map.class);

		// Customer Request Mapping
		/*
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
		*/
		
		String customerName = data.get("cus_name") == null ? "" : data.get("cus_name").toString();
		String mobileNo = data.get("mob_num") == null ? "" : data.get("mob_num").toString();
		String botNo = data.get("whatsappBotNum") == null ? "" : data.get("whatsappBotNum").toString();
		String motorUsage = data.get("veh_usage") == null ? "" : data.get("veh_usage").toString();
		String idType = data.get("id_type") == null ? "" : data.get("id_type").toString();
		String idNumber = data.get("id_number") == null ? "" : data.get("id_number").toString();
		String sum_insured = data.get("sum_insured") == null ? "" : data.get("sum_insured").toString(); 
		String email = data.get("cus_email") == null ? "" : data.get("cus_email").toString();
		String insuranceClass = data.get("ins_class") == null ? "" : data.get("ins_class").toString();
		
		PhoenixUserDataDetails getApiResonse = userDataRepo.findTop1ByWaidAndCompanyId(Long.valueOf(botNo),"100050");
		String resp = getApiResonse.getFlowRequest();
		resp = resp.replace("/","");
		//resp = resp.replace("{", "");
		//resp = resp.replace("}", "");
		Map<String, Object> values = mapper.readValue(resp, Map.class);
		String val = values.get("Response").toString();
		Map<String,Object> value = mapper.readValue(val, Map.class);
		System.out.println(value);
		
		String regNo = value.get("VehicleRegistrationNumber") == null ? null : value.get("VehicleRegistrationNumber").toString();
		String chassisNo = value.get("VehicleIdendicationNumber") == null ? null : value.get("VehicleIdendicationNumber").toString();
		String engineNo = value.get("EngineNumber") == null ? null : value.get("EngineNumber").toString();
		String make = value.get("Make") == null ? null : value.get("Make").toString();
		String model = value.get("SeriesName") == null ? null : value.get("SeriesName").toString();
		String bodyType = value.get("VehicleDescription") == null ? null : value.get("VehicleDescription").toString();
		String grossWeight = value.get("Tare") == null ? null : value.get("Tare").toString();
		
		
		if(StringUtils.isNotEmpty(insuranceClass)) {
			if("1".equals(insuranceClass)) {
				insuranceClass = "Comprehensive";
			}else if("2".equals(insuranceClass)) {
				insuranceClass = "TPFT";
			}else if("3".equals(insuranceClass)) {
				insuranceClass = "TPL";
			}
		}
		
		if(StringUtils.isNotEmpty(motorUsage)) {
			if("1".equals(motorUsage)) {
				motorUsage = "Business";
			}else if("2".equals(motorUsage)) {
				motorUsage = "Occassional Business";
			}else if("3".equals(motorUsage)) {
				motorUsage = "Private";
			}else if("3".equals(motorUsage)) {
				motorUsage = "Yango Service";
			}
		}
		
		if(StringUtils.isNotEmpty(idType)) {
			if("1".equals(idType)) {
				idType = "Driver License";
			}else if("2".equals(idType)) {
				idType = "National Identification Card (NID)";
			}else if("3".equals(idType)) {
				idType = "Passport Number";
			}
		}

		// DOB calc
		LocalDate policyStartDate = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	//	DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	//	LocalDate policyStartDate = LocalDate.parse(policyDate, inputFormatter);
	    String	policyDate = policyStartDate.format(formatter);
	//	LocalDate curDate = LocalDate.parse(policyDate, formatter);
		LocalDate minusDate = policyStartDate.minusYears(18);
		String cusDob = minusDate.format(formatter);

		String titlteId = "", genderId = "", occupationId = "", idTypeId = "", regionId = "", distictId = "", colorId = "",
				motorUsageId = "", bodyTypeId = "", makeId = "", modelId = "", insuranceClassId = "";

		// masterIds
	/*	Map<String, Object> titleMap = new HashMap<>();
		titleMap.put("Desc", title);
		titleMap.put("MasterType", "CUSTOMER_TITLE");
		titleMap.put("InsuranceId", "100050");

		try {
			String titileReq = mapper.writeValueAsString(titleMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: " + masterApi);

			String apiResponse = thread.callNamibiaComApi(masterApi, titileReq);

			log.info("Title Id Response: " + apiResponse);
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
		} */

		// Gender Id
	/*	Map<String, Object> genderMap = new HashMap<>();
		genderMap.put("Desc", gender);
		genderMap.put("MasterType", "CUSTOMER_GENDER");
		genderMap.put("InsuranceId", "100050");

		try {
			String genderReq = mapper.writeValueAsString(genderMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: " + masterApi);

			String apiResponse = thread.callNamibiaComApi(masterApi, genderReq);

			log.info("Gender Response: " + apiResponse);
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
		}*/

		// occupationId
	/*	Map<String, Object> occupationMap = new HashMap<>();
		occupationMap.put("Desc", occupation);
		occupationMap.put("MasterType", "CUSTOMER_OCCUPATION");
		occupationMap.put("InsuranceId", "100050");
		
		try {
			String occupationReq = mapper.writeValueAsString(occupationMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: " + masterApi);

			String apiResponse = thread.callNamibiaComApi(masterApi, occupationReq);
			
			log.info("Occupation Id Response: " + apiResponse);
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
		}*/
		
		//IdTYpe
		Map<String,Object> idTypeMap = new HashMap<>();
		idTypeMap.put("Desc", idType);
		idTypeMap.put("MasterType", "CUSTOMER_IDTYPE");
		idTypeMap.put("InsuranceId", "100050");
		
		try {
			String idTypeReq = mapper.writeValueAsString(idTypeMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: " + masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, idTypeReq);
			
			log.info("ID type Response: " + apiResponse);
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
		/*Map<String,Object> regionMap = new HashMap<>();
		regionMap.put("Desc", region);
		regionMap.put("MasterType", "CUSTOMER_REGION");
		regionMap.put("CountryCode", "NAM");
		
		try {
			String regionReq = mapper.writeValueAsString(regionMap);
			String masterApi = masterIdsApi;
			log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, regionReq);
			
			log.info("Region id Response: " + apiResponse);
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
		}*/
		
		//DistrictId
	/*	Map<String,Object> districtMap = new HashMap<>();
		districtMap.put("Desc", region);
		districtMap.put("MasterType", "CUSTOMER_DISTRICT");
		districtMap.put("CountryCode", "NAM");
		
		try {
			String districtReq = mapper.writeValueAsString(districtMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, districtReq);
			
			log.info("District Response: " + apiResponse);
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
		}*/
		
		//InsuranceClassId
		Map<String,Object> insuranceClassMap = new HashMap<>();
		insuranceClassMap.put("Desc", insuranceClass);
		insuranceClassMap.put("MasterType", "INSURANCE_CLASS");
		insuranceClassMap.put("InsuranceId", "100050");
		
		try {
			String insuranceClassReq = mapper.writeValueAsString(insuranceClassMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, insuranceClassReq);
			
			log.info("Insurance Class Response: " + apiResponse);
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
		motorusageMap.put("InsuranceId", "100050");
		
		try {
			String motorUsageReq = mapper.writeValueAsString(motorusageMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, motorUsageReq);
			
			log.info("Motor Usage Response: " + apiResponse);
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
		bodyTypeMap.put("InsuranceId", "100050");
		
		try {
			String bodyTypeReq = mapper.writeValueAsString(bodyTypeMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, bodyTypeReq);
			
			log.info("BodyType Response: " + apiResponse);
			
			Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
			
			Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
				mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
			
			if(masterApiResult.isEmpty() || masterApiResult == null) {
				String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
				response = errorMessage;
				return response;
			}else {
				bodyTypeId = masterApiResult.get("Response") == null ? "97" : masterApiResult.get("Response").toString();
			}
		}catch(Exception e) {
			e.printStackTrace();
			log.info(e.getMessage());
		}
		
		//MakeId 
		Map<String,Object> makeMap = new HashMap<>();
		makeMap.put("Desc", make);
		makeMap.put("MasterType", "VEHICLE_MAKE");
		makeMap.put("InsuranceId", "100050");
		
		try {
			String makeReq = mapper.writeValueAsString(makeMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, makeReq);
			
			log.info("Make Id Response: " + apiResponse);
			
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
		modelMap.put("InsuranceId", "100050");
		
		try {
			String modelReq = mapper.writeValueAsString(modelMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, modelReq);
			
			log.info("Model Id Response: " + apiResponse);
			
			Map<String,Object> respMap = mapper.readValue(apiResponse, Map.class);
			
			Map<String,Object> masterApiResult = respMap.get("Result") == null ? null :
				mapper.readValue(mapper.writeValueAsString(respMap.get("Result")), Map.class);
			
			if(masterApiResult.isEmpty() || masterApiResult == null) {
				String errorMessage = respMap.get("ErrorMessage") == null ? "" : respMap.get("ErrorMessage").toString();
				response = errorMessage;
				return response;
			}else {
				modelId = masterApiResult.get("Response") == null ? "99999" : masterApiResult.get("Response").toString();
			}
		}catch(Exception e) {
			e.printStackTrace();
			log.info(e.getMessage());
		}
		
		//colorId
	/*	Map<String,Object> colorMap = new HashMap<>();
		colorMap.put("Desc", color);
		colorMap.put("MasterType", "VEHICLE_COLOR");
		colorMap.put("InsuranceId", "100050");
		
		try {
			String colorReq = mapper.writeValueAsString(colorMap);
			String masterApi = masterIdsApi;
	        log.info("Master Ids Api Calling: "+masterApi);
			
			String apiResponse = thread.callNamibiaComApi(masterApi, colorReq);
			
			log.info("Color Id Response: " + apiResponse);
			
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
		}*/
		
		//==============================SAVE VEHICLE INFO BLOCK START=============================================
		log.info("SAVE VEHICLE INFO START: "+new Date());
		
		Map<String,Object> vehicleInfo = new HashMap<String,Object>();
		vehicleInfo.put("Insuranceid", "100050");
		vehicleInfo.put("BranchCode", "120");
		vehicleInfo.put("AxelDistance", 1);
		vehicleInfo.put("Chassisnumber", chassisNo);
		vehicleInfo.put("Color", "471");
		vehicleInfo.put("CreatedBy", "Wh_Nam_Broker");
		vehicleInfo.put("DisplacementInCM3", "0");
		vehicleInfo.put("EngineNumber", engineNo);
		//vehicleInfo.put("FuelType", null);
		vehicleInfo.put("Grossweight", grossWeight);
		vehicleInfo.put("ManufactureYear", "2025");
		//vehicleInfo.put("MotorCategory", null);
		vehicleInfo.put("Motorusage", motorUsage);
		//vehicleInfo.put("NumberOfAxels", null);
		vehicleInfo.put("OwnerCategory", "1");
		vehicleInfo.put("Registrationnumber", regNo);
		vehicleInfo.put("ResEngineCapacity", "1200");
		vehicleInfo.put("ResOwnerName", customerName);
		vehicleInfo.put("ResStatusCode", "Y");
		vehicleInfo.put("ResStatusDesc", "None");
		vehicleInfo.put("SeatingCapacity", "5");
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
			String vehResponse = thread.callNamibiaComApi(saveVehicleInfoApi,vehReq);
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
		customerCreation.put("Address1", "Address1");
		customerCreation.put("Address2", "");
		customerCreation.put("AppointmentDate", "");
		customerCreation.put("BranchCode", "120");
		customerCreation.put("BrokerBranchCode", "1");
		customerCreation.put("BusinessType", null);
		customerCreation.put("CityCode", "1");
		customerCreation.put("CityName", "Erongo");//check district or region
		customerCreation.put("ClientName", customerName);
		customerCreation.put("Clientstatus", "Y");
		customerCreation.put("Country", "NAM");
		customerCreation.put("CountryName", "Namibia");
		customerCreation.put("CreatedBy", "Wh_Nam_Broker");//create login for whatsapp bot
		customerCreation.put("CustomerAsInsurer", "N");
		customerCreation.put("CustomerReferenceNo", "");
		customerCreation.put("DobOrRegDate", cusDob);
		customerCreation.put("Email1", email);
		customerCreation.put("Email2", null);
		customerCreation.put("Email3", null);
		customerCreation.put("ExpiryDate", null);
		customerCreation.put("Fax", null);
		customerCreation.put("Gender", "M");
		customerCreation.put("IdNumber", idNumber);
		customerCreation.put("IdType", idTypeId);
		customerCreation.put("InsuranceId", "100050");
		customerCreation.put("IsTaxExempted", "N");
		customerCreation.put("Language", "1");
		customerCreation.put("LastName", "");
		customerCreation.put("MaritalStatus", "Single");//check
		customerCreation.put("MiddleName", "");
		customerCreation.put("MobileCode1", "264");
		customerCreation.put("MobileCodeDesc1", "1");
		customerCreation.put("MobileNo1", mobileNo);
		customerCreation.put("MobileNo2", "");
		customerCreation.put("MobileNo3", null);
		customerCreation.put("Nationality", "");
		customerCreation.put("Occupation", "99999");
		customerCreation.put("OtherOccupation", "");
		customerCreation.put("PhoneNoCode", "");
		customerCreation.put("PinCode", "");
		customerCreation.put("Placeofbirth", "Address1");
		customerCreation.put("PolicyHolderType", "1");
		customerCreation.put("PolicyHolderTypeid", idTypeId);
		customerCreation.put("PreferredNotification", "sms");
		customerCreation.put("ProductId", "5");
		customerCreation.put("RegionCode", "1");
		customerCreation.put("RiskAssessmentDate", null);
		customerCreation.put("SaveOrSubmit", "Save");
		customerCreation.put("SocioProfessionalCategory", null);
		customerCreation.put("StateCode", "1");
		customerCreation.put("StateName", null);
		customerCreation.put("Status", "Y");
		customerCreation.put("Street", "Address1");
		customerCreation.put("TaxExemptedId", null);
		customerCreation.put("TelephoneNo1", null);
		customerCreation.put("TelephoneNo2", null);
		customerCreation.put("TelephoneNo3", null);
		customerCreation.put("Title", "1");
		customerCreation.put("Type", null);
		customerCreation.put("VipFlag", null);
		customerCreation.put("VrTinNo", null);
		customerCreation.put("WhatsappCode", "264");
		customerCreation.put("WhatsappDesc", "1");
		customerCreation.put("WhatsappNo", mobileNo);
		customerCreation.put("Zone", "1");
		
		String custRefNo = "";
		
		try {
			String cusReq = mapper.writeValueAsString(customerCreation);
			String cusSaveApi = saveCustomerApi;
			log.info("Customer Save Calling: "+cusSaveApi);
			log.info("Customer Save Request: "+cusReq);
			
			String apiResponse = thread.callNamibiaComApi(cusSaveApi,cusReq);
			
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
	/*	log.info("SHOW VEHICLE INFO BLOCK START: "+new Date());
		Map<String,Object> vehInfo = new HashMap<String,Object>();
		vehInfo.put("BranchCode", "120");
		vehInfo.put("BrokerBranchCode", "1");
		vehInfo.put("CreatedBy", "Wh_Nam_Broker");
		vehInfo.put("InsuranceId", "100050");
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
			
			String apiResponse = thread.callNamibiaComApi(showVehApi, showInfo);
			
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
		log.info("SHOW VEHICLE INFO BLOCK END: "+new Date()); */
		//==============================SHOW VEHICLE INFO BLOCK END=============================================
		
		//==============================MOTOR SAVE BLOCK START=============================================
		log.info("MOTOR SAVE BLOCK START: "+new Date());
		LocalDate endDate = policyStartDate.plusDays(364);
		String policyEndDate = endDate.format(formatter);
		
		Map<String,Object> motorSave = new HashMap<String,Object>();
		motorSave.put("AboutVehicle", null);
		motorSave.put("AcExecutiveId", null);
		motorSave.put("AcccessoriesSumInsured", null);
		motorSave.put("AccessoriesInformation", null);
		motorSave.put("AdditionalCircumstances", "");
		motorSave.put("AgencyCode", "14130");//local-13495,UAT-14130
		motorSave.put("AggregatedValue", null);
		motorSave.put("ApplicationId", "1");
		motorSave.put("AxelDistance", 1);
		motorSave.put("BankingDelegation", "");
		motorSave.put("BdmCode", "2000010");//uAt-5467546,local-5555555
		motorSave.put("BorrowerType", null);
		motorSave.put("BranchCode", "120");
		motorSave.put("BrokerBranchCode", "1");
		motorSave.put("BrokerCode", "14130");//local-13495,UAT-14130
		motorSave.put("Chassisnumber", chassisNo);
		motorSave.put("CityLimit", null);
		motorSave.put("ClaimType", "0");
		//motorSave.put("ClaimTypeDesc", null);
		motorSave.put("CollateralCompanyAddress", "");
		motorSave.put("CollateralCompanyName", "");
		motorSave.put("CollateralName", null);
		motorSave.put("CollateralYn", "N");
		motorSave.put("Color", colorId);
		motorSave.put("ColorDesc", "Others");
		motorSave.put("CommissionType", null);
		motorSave.put("CoverNoteNo", null);
		motorSave.put("CreatedBy", "Wh_Nam_Broker");//broker id
		motorSave.put("CubicCapacity", "1200");//doubt
		motorSave.put("Currency", "NAD");
		motorSave.put("CustomerCode", "2000010");//uAt-5467546,local-5555555
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
		motorSave.put("InsuranceId", "100050");
		motorSave.put("Insurancetype", insuranceClassId);//103 check
		motorSave.put("InsurancetypeDesc", insuranceClass);
		motorSave.put("InsurerSettlement", "");
		motorSave.put("InterestedCompanyDetails", "");
		motorSave.put("IsFinanceEndt", null);
		motorSave.put("LoanAmount", 0);
		motorSave.put("LoanEndDate", null);
		motorSave.put("LoanStartDate", null);
//		motorSave.put("LocationId", "1");
		motorSave.put("LoginId", "Wh_Nam_Broker");//login
		motorSave.put("ManufactureYear", "2025");
		motorSave.put("MarketValue", null);
		motorSave.put("Mileage", null);
		motorSave.put("MobileCode", "264");
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
//		motorSave.put("NoOfComprehensives", null);
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
		motorSave.put("SeatingCapacity", "5");
		motorSave.put("SectionId", Arrays.asList(insuranceClassId));
//		motorSave.put("SourceType", "Broker");
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
		exchangeRateScenario.put("OldCurrency", "NAD");
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
			
			String apiResponse = thread.callNamibiaComApi(saveMotorApi,motorSaveReq);
			
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
		calc.put("AgencyCode", "14130");//local-13506,UAT-14130
		calc.put("BranchCode", "120");
		calc.put("CdRefNo", motorSaveResult.get("CdRefNo") == null ? "" : motorSaveResult.get("CdRefNo"));
		calc.put("CoverModification", "N");
		calc.put("CreatedBy", "Wh_Nam_Broker");
		calc.put("DdRefNo", motorSaveResult.get("DdRefNo") == null ? "" : motorSaveResult.get("DdRefNo"));
		calc.put("EffectiveDate", policyDate);
		calc.put("InsuranceId", "100050");
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
			String apiResponse = thread.callNamibiaComApi(calculatorApi, calReq);
			
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
		Double vatTax =0D;
		Double vatPercentage=0D;
		
		coverList = coverList.stream().filter(f -> "5".equalsIgnoreCase(f.get("CoverId").toString())).collect(Collectors.toList());
		List<Map<String,Object>> tax = (List<Map<String, Object>>) coverList.get(0).get("Taxes");
		
		BigDecimal pre = coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
				.map(m -> new BigDecimal(m.get("PremiumExcluedTaxLC").toString())).reduce(BigDecimal.ZERO, (x,y) -> x.add(y));
		
		BigDecimal totalPremium = coverList.stream().filter(p -> "B".equalsIgnoreCase(p.get("CoverageType").toString()) || "D".equalsIgnoreCase(p.get("isSelected").toString()))
				.map(m -> new BigDecimal(m.get("PremiumIncludedTaxLC").toString())).reduce(BigDecimal.ZERO, (x,y) -> x.add(y));
		
		List<Map<String,Object>> taxList = coverList.parallelStream().filter(p -> ("B".equalsIgnoreCase(p.get("CoverageType").toString()) 
				|| "D".equalsIgnoreCase(p.get("isSelected").toString())) && p.get("Taxes") != null)
				.map(m -> (List<Map<String,Object>>) m.get("Taxes")).flatMap(f -> f.stream()).collect(Collectors.toList());
		
		 vatTax = taxList.stream().map(t -> t.get("TaxAmount") == null ? 0D : Double.valueOf(t.get("TaxAmount").toString()).longValue())
				.reduce(0D, (a,b) -> a + b);
		
		vatPercentage = tax.get(0).get("TaxRate")==null?0L:Double.valueOf(tax.get(0).get("TaxRate").toString());
		
		premium = pre.longValue();
		
		String taxVate = totalPremium.subtract(pre).toString();
		
		//Long totalPremium =pre.longValue()+vatTax.longValue();
				
		log.info("CALC BLOCK END: "+new Date());
		
		//==============================CALC BLOCK END=============================================================
		
		//==============================USER CREATION BLOCK START=============================================
		
		log.info("USER CREATION BLOCK START: "+new Date());
		
		Map<String,Object> userCreationMap = new HashMap<String,Object>();
		userCreationMap.put("CompanyId", "100050");
		userCreationMap.put("CustomerId", custRefNo);
		userCreationMap.put("ProductId", "5");
		userCreationMap.put("ReferenceNo", reqRefNo);
		userCreationMap.put("UserMobileNo", mobileNo);
		userCreationMap.put("UserMobileCode", "264");
		userCreationMap.put("AgencyCode", "14130"); //local-13506,UAT-14130
		
		Map<String,Object> userResult = null;
		Map<String,Object> userRes = null;
		try {
			String userCreationReq = mapper.writeValueAsString(userCreationMap);
			String userCreationApi = loginCreationApi;
			
			log.info("USER CREATION API: "+userCreationApi);
			
			String apiResponse = thread.callNamibiaComApi(userCreationApi, userCreationReq);
			
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
		buypolicyMap.put("CreatedBy", "Wh_Nam_Broker");
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
			
			String apiResponse = thread.callNamibiaComApi(buyPolicyApi, buypolicyReq);
			
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
		makePaymentMap.put("CreatedBy", "Wh_Nam_Broker");
		makePaymentMap.put("EmiYn", "N");
		makePaymentMap.put("InstallmentMonth", null);
		makePaymentMap.put("InstallmentPeriod", null);
		makePaymentMap.put("InsuranceId", "100050");
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
			String apiResponse = thread.callNamibiaComApi(makePaymentApi, makePayemantReq);
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
		insertPaymentMap.put("CreatedBy", "Wh_Nam_Broker");
		insertPaymentMap.put("EmiYn", "N");
		insertPaymentMap.put("IbanNumber", null);
		insertPaymentMap.put("InsuranceId", "100050");
		insertPaymentMap.put("MICRNo", null);
		insertPaymentMap.put("MobileCode1", null);
		insertPaymentMap.put("MobileNo1", null);
		insertPaymentMap.put("PayeeName", customerName);
		insertPaymentMap.put("PaymentId", makePaymentResult.get("PaymentId"));
		insertPaymentMap.put("PaymentType", "6");
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
			String apiResponse = thread.callNamibiaComApi(insertPaymentApi, insertPaymentReq);
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
	    paymentMap.put("CompanyId", "100050");
	    paymentMap.put("WhatsappCode", "264");
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
		String apiResponse = thread.callNamibiaComApi(motorDetailsApi, api_request);
		
		Map<String,Object> getMotorRes =mapper.readValue(apiResponse, Map.class);
		
		List<Map<String,Object>> motorRes =getMotorRes.get("Result")==null?null:
			mapper.readValue(mapper.writeValueAsString(getMotorRes.get("Result")), List.class);
		
		log.info("ALL MOTOR DETAILS :" +motorRes);
		
		Map<String,Object> mot = motorRes.get(0);
		
		botResponceData.put("registration", mot.get("Registrationnumber")==null?"N/A":mot.get("Registrationnumber"));
		botResponceData.put("usage", mot.get("MotorUsageDesc")==null?"N/A":mot.get("MotorUsageDesc"));
		botResponceData.put("vehtype", mot.get("VehicleTypeDesc")==null?"N/A":mot.get("VehicleTypeDesc"));
		//botResponceData.put("color",mot.get("ColorDesc")==null?"N/A":mot.get("ColorDesc"));
		//botResponceData.put("insurance_class",insuredClass);
		botResponceData.put("premium", premium);
		botResponceData.put("url", paymentUrl);
		botResponceData.put("vatamt", taxVate);
		botResponceData.put("suminsured", mot.get("SumInsured")==null?"N/A":mot.get("SumInsured"));
		botResponceData.put("chassis", mot.get("Chassisnumber")==null?"N/A":mot.get("Chassisnumber"));
		//botResponceData.put("vat", String.valueOf(vatPercentage.longValue()));
		botResponceData.put("totalpremium", totalPremium);
		botResponceData.put("inceptiondate", policyDate);
		botResponceData.put("expirydate",policyEndDate);
		botResponceData.put("referenceno", reqRefNo);
		//botResponceData.put("veh_model_desc", mot.get("VehcilemodelDesc")==null?"N/A":mot.get("VehcilemodelDesc"));
		botResponceData.put("veh_make_desc", mot.get("VehiclemakeDesc")==null?"N/A":mot.get("VehiclemakeDesc"));
		botResponceData.put("customer_name", customerName);
		
		/*
		//Template call
		
		String templateBody="*Phoenix Namibia Assurance Company Limited*\\n\\nHi *"+botResponceData.get("customer_name")+"*\\n*Your vehicle details* "
				+ "\\nRegistration No : "+botResponceData.get("registration")+"\\nMake : "+botResponceData.get("veh_make_desc")+""
						+ "\\nVehicle Usage : "+botResponceData.get("usage")+"\\nVehicle Type : "+botResponceData.get("vehtype")+""
								+ "\\n\\n*Your Premium Details*\\nPremium : "+botResponceData.get("premium")+"\\n"
										+ "Tax Amount : "+botResponceData.get("vatamt")+"\\nTotal Premium :"+botResponceData.get("totalpremium")+"\\n\\n"
												+ "*Policy Details*\\nInception Date:"+botResponceData.get("inceptiondate")+"\\nExpiry Date:"+botResponceData.get("expirydate")+"\\n"
														+ "Reference No  :  "+botResponceData.get("referenceno")+"\\n\\nPayment Link : "+botResponceData.get("url")+"";
		
		String mobNam = mobileNo;
		int count = mobNam.length();
		if(count == 9) {
			mobNam = "264"+mobNam;
		}else if(count == 10) {
			mobNam = "91"+mobNam;
		}
		
		Map<String,Object> tempMap = new HashMap<>();
		tempMap.put("messaging_product", "whatsapp");
		tempMap.put("recipient_type", "individual");
		tempMap.put("to", mobNam);
		tempMap.put("type", "text");
		
		Map<String,Object> textMap = new HashMap<>();
		textMap.put("body", templateBody);
		
		tempMap.put("text", textMap);
		
		String twoSlash=cs.getwebserviceurlProperty().getProperty("wa.hit.slash.two");
		String oneSlash=cs.getwebserviceurlProperty().getProperty("wa.hit.slash.one");
	
		String message_text =cs.reqPrint(tempMap);
		
		message_text =message_text.replace(twoSlash, oneSlash);
		
		String apiUrl = namibiaMsgApi;
		RestTemplate restTemp = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", namibiaMsgAuth);
		
		HttpEntity<String> request = new HttpEntity<>(message_text,headers);
		
		try {
			ResponseEntity<String> responses = restTemp.postForEntity(apiUrl, request, String.class);
			log.info("Template Api Response :"+responses.getBody());
			
		}catch(Exception e) {
			e.printStackTrace();
			log.info(e);
		}
		
		/*
		 * message_text =message_text.replace(twoSlash, oneSlash);
		 * 
		 * MediaType contentType =MediaType.parse("application/json"); RequestBody body
		 * =RequestBody.create(message_text,contentType);
		 */
		
		
		
		return botResponceData;


	
	}

	
}
