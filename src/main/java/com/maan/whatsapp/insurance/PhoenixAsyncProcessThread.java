package com.maan.whatsapp.insurance;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.maan.whatsapp.service.common.CommonService;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
@PropertySource("classpath:WebServiceUrl.properties")
@Lazy
public class PhoenixAsyncProcessThread {

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private CommonService cs;

	private OkHttpClient httpClient = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS)
			.connectTimeout(60, TimeUnit.SECONDS).build();

	private MediaType mediaType = MediaType.parse("application/json");

	public static Gson printReq = new Gson();
	
	@Value("${wh.phoenix.customer.title.api}")
	private String customerTitle;
	
	@Value("${wh.phoenix.customer.region}")
	private String cusRegion;
	
	@Value("${wh.phoenix.customer.occupation}")
	private String cusOccupation;
	
	@Value("${wh.phoenix.customer.idtype}")
	private String cusIdType;
	
	@Value("${wh.phoenix.stp.vehicleUsage.api}")
	private String motorUsage;
	
	@Value("${wh.phoenix.stp.bodytype.api}")
	private String bodyType;
	
	@Value("${wh.phoenix.stp.motorcategory.api}")
	private String motorCategory;
	
	@Value("${wh.phoenix.stp.vehiclecolor.api}")
	private String vehColor;
	
	@Value("${wh.get.phoenix.insuranceclass.api}")
	private String insClass;
	
	@Value("${wh.get.phoenix.insuranceperiod.api}")
	private String insPeriod;
	
	@Value("${wh.phoenix.stp.motormake.api}")
	private String motorMake;
	
	@Value("${wh.phoenix.stp.motormodel.api}")
	private String motorModel;
	
	@Value("${wh.phoenix.customer.gender}")
	private String gender;
	
	@Value("${wh.phoenix.noclaim.api}")
	private String noClaim;
	
	@Value("${wh.phoenix.customer.district}")
	private String district;

	public String getZambiaToken() {
		try {
			Map<String, Object> tokenReq = new HashMap<String,Object>();
			tokenReq.put("LoginId", "Zambia_whatsapp");
			tokenReq.put("Password", "Admin@01");
			tokenReq.put("ReLoginKey", "Y");
			
			Response response = null;
			
			String tokenJsonReq = new Gson().toJson(tokenReq);
			String tokenApi = cs.getwebserviceurlProperty().getProperty("wh.phoenix.token.api");
			RequestBody tokenReqBody = RequestBody.create(tokenJsonReq, mediaType);
			Request tokenRequest = new Request.Builder().url(tokenApi).post(tokenReqBody).build();
			
			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
					}
			};
			
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom()); 
			
			httpClient = new OkHttpClient.Builder().sslSocketFactory(sslContext.getSocketFactory(),(X509TrustManager)trustAllCerts[0]).readTimeout(60, TimeUnit.SECONDS).build(); 
			response = httpClient.newCall(tokenRequest).execute();
			String obj = response.body().toString();
			Map<String, Object> tokenRes = mapper.readValue(obj, Map.class);
			Map<String, Object> tokenObj = tokenRes.get("Result") == null ? null
					: (Map<String, Object>) tokenRes.get("Result");
			String token = tokenObj.get("Token") == null ? "" : tokenObj.get("Token").toString();
			return token;
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getCustomerTitle(String request, String token) {
		try {
			String api = this.customerTitle;
			String response = callPhoenixApi(api,request,token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	public String callPhoenixApi(String url, String request, String token) {
		String apiResponse="";
		try {
			
			Response response = null;
			RequestBody reqBody = RequestBody.create(request,mediaType);
			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException{}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException{}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
					}
			};
			
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom()); 
			
			Request apiReq = new Request.Builder().addHeader("Authorization", "Bearer " + token).url(url)
					.post(reqBody).build();
			
			httpClient = new OkHttpClient.Builder().sslSocketFactory(sslContext.getSocketFactory(),(X509TrustManager)trustAllCerts[0]).readTimeout(60, TimeUnit.SECONDS)
					.connectTimeout(60, TimeUnit.SECONDS).build();
			
			Response res = httpClient.newCall(apiReq).execute(); 
	        System.out.println("Response: " + res);

	        if (!res.isSuccessful()) {
	            throw new IOException("Unexpected response code: " + res);
	        }

	        
	        apiResponse = res.body().string();
	        System.out.println("Response Body: " + apiResponse);
			
			//response = httpClient.newCall(apiReq).execute();
			//apiResponse = response.body().toString();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return apiResponse;
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getCustomerRegion(String request,String token) {
		try {
			
			String api = this.cusRegion;
			
			String response = callPhoenixApi(api, request, token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getCustomerOccupation(String request,String token) {
		try {
			String api = this.cusOccupation;
			
			String response = callPhoenixApi(api, request, token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getCustomerIdType(String request,String token) {
		try {
			String api = this.cusIdType;
			
			String response = callPhoenixApi(api, request, token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			
            List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	public CompletableFuture<List<Map<String, String>>> getCustomerDistrict(String request, String token) {
		
		try {
			String api = this.district;
			String response = callPhoenixApi(api, request, token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc"));
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getMotorUsage(String request, String token) {
		try {
			String api = this.motorUsage;
			String response = callPhoenixApi(api, request, token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc"));
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);

		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getMotorType(String request, String token) {
		try {
			String api = this.bodyType;
			
			String response = callPhoenixApi(api, request, token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc"));
				returnResponse.add(values);
			});
			
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getMotorCategory(String request, String token) {
		try {
			String api = this.motorCategory;
			
			String response = callPhoenixApi(api, request, token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc"));
				returnResponse.add(values);
			});
			
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getManufactureYear() {
		try {
			
			int currentYear = Year.now().getValue();
			int endYear = currentYear - 35;
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			List<Long> list = LongStream.rangeClosed(endYear, currentYear).boxed().sorted(Comparator.reverseOrder())
					.collect(Collectors.toList());
			list.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.toString());
				values.put("title", data.toString());
				returnResponse.add(values);
			});
			
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getVehicleColor(String request, String token) {
		try {
			String api = this.vehColor;
			
			String response = callPhoenixApi(api, request, token);
			
			Map<String, Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getMake(String request, String token) {
		try {
			String api = this.motorMake;
			String response = callPhoenixApi(api, request, token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getModel(String request, String token) {
		try {
			String api = this.motorModel;
            String response = callPhoenixApi(api, request, token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			
		}
		return CompletableFuture.completedFuture(null);
	}

	public CompletableFuture<List<Map<String, String>>> getInsuredPeriod(String request, String token) {
		try {
			String api = this.insPeriod;
			
			String response = callPhoenixApi(api, request, token);
			
			Map<String,Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String,String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			apiData.forEach(data -> {
				Map<String,String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@Async
	public CompletableFuture<List<Map<String, String>>> getInsuranceClass(String request, String token) {
		try {
			String api = this.insClass;

			String response = callPhoenixApi(api, request, token);

			Map<String, Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String, String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			List<Map<String, String>> returnResponse = new ArrayList<Map<String, String>>();

			apiData.forEach(data -> {
				Map<String, String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}
	
	//@Async
	/*public CompletableFuture<List<Map<String,String>>> getGender(){
		try {
			List<Map<String,String>> returnResponse = new ArrayList<Map<String,String>>();
			
			Map<String,String> values = new HashMap<>();
			values.put("M", "Male");
			values.put("F", "Female");
			returnResponse.add(values);
			
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}*/

	@Async
	public CompletableFuture<List<Map<String, String>>> getGender(String request, String token) {
		try {
			
			String api = this.gender;
			String response = callPhoenixApi(api, request, token);
			
			Map<String, Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String, String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			List<Map<String, String>> returnResponse = new ArrayList<Map<String, String>>();
			
			apiData.forEach(data -> {
				Map<String, String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	public String getNamibiaToken() {
		try {
			Map<String, Object> tokenReq = new HashMap<String,Object>();
			tokenReq.put("LoginId", "NamBroker1");
			tokenReq.put("Password", "Admin@01");
			tokenReq.put("ReLoginKey", "Y");
			
			Response response = null;
			
			String tokenJsonReq = new Gson().toJson(tokenReq);
			String tokenApi = cs.getwebserviceurlProperty().getProperty("wh.phoenix.token.api");
			RequestBody tokenReqBody = RequestBody.create(tokenJsonReq, mediaType);
			Request tokenRequest = new Request.Builder().url(tokenApi).post(tokenReqBody).build();
			
			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
					}
			};
			
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom()); 
			
			httpClient = new OkHttpClient.Builder().sslSocketFactory(sslContext.getSocketFactory(),(X509TrustManager)trustAllCerts[0]).readTimeout(60, TimeUnit.SECONDS).build();
			
		/*	response = httpClient.newCall(tokenRequest).execute();
			System.out.println("Response "+response);
			String obj = response.body().toString();
			System.out.println("Obj "+obj);
			Map<String, Object> tokenRes = mapper.readValue(obj, Map.class);
			Map<String, Object> tokenObj = tokenRes.get("Result") == null ? null
					: (Map<String, Object>) tokenRes.get("Result");
			String token = tokenObj.get("Token") == null ? "" : tokenObj.get("Token").toString();
			return token; */
			Response res = httpClient.newCall(tokenRequest).execute(); 
		        //System.out.println("Response: " + res);

		        if (!res.isSuccessful()) {
		            throw new IOException("Unexpected response code: " + res);
		        }

		        
		        String responseBody = res.body().string();
		        //System.out.println("Response Body: " + responseBody);

		        
		        Map<String, Object> tokenRes = new ObjectMapper().readValue(responseBody, Map.class);

		        
		        Map<String, Object> tokenObj = (Map<String, Object>) tokenRes.getOrDefault("Result", new HashMap<>());

		        
		        String token = tokenObj.getOrDefault("Token", "").toString();

		        return token;
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public CompletableFuture<List<Map<String, String>>> getNoClaimBonus(String request, String token) {
		try {
			String api = this.noClaim;
            String response = callPhoenixApi(api, request, token);
			
			Map<String, Object> viewRes = mapper.readValue(response, Map.class);
			List<Map<String, String>> apiData = viewRes.get("Result") == null ? null
					: (List<Map<String, String>>) viewRes.get("Result");
			List<Map<String, String>> returnResponse = new ArrayList<Map<String, String>>();
			
			apiData.forEach(data -> {
				Map<String, String> values = new HashMap<>();
				values.put("id", data.get("Code").toString());
				values.put("title", data.get("CodeDesc").toString());
				returnResponse.add(values);
			});
			return CompletableFuture.completedFuture(returnResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(null);
	}

	@SuppressWarnings("unchecked")
	public String callNamibiaComApi(String url, String request) {
		String apiReponse = "";
		try {
			Response response = null;
			Map<String, Object> tokReq = new HashMap<String, Object>();
			tokReq.put("LoginId", "Wh_Nam_Broker");
			tokReq.put("Password", "Admin@01");
			tokReq.put("ReLoginKey", "Y");
			
			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
					}
			};
			
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom()); 

			httpClient = new OkHttpClient.Builder().sslSocketFactory(sslContext.getSocketFactory(),(X509TrustManager)trustAllCerts[0]).readTimeout(60, TimeUnit.SECONDS)
					.connectTimeout(60, TimeUnit.SECONDS).build();
			
			String tokenJsonReq = new Gson().toJson(tokReq);
			String tokenApi = cs.getwebserviceurlProperty().getProperty("wh.phoenix.token.api");
			
			RequestBody tokenReqBody = RequestBody.create(tokenJsonReq, mediaType);
			Request tokenReq = new Request.Builder().url(tokenApi).post(tokenReqBody).build();
			response = httpClient.newCall(tokenReq).execute();
			String obj = response.body().string();	
			Map<String, Object> tokenRes = mapper.readValue(obj, Map.class);
			Map<String, Object> tokenObj = tokenRes.get("Result") == null ? null
					: (Map<String, Object>) tokenRes.get("Result");
			String token = tokenObj.get("Token") == null ? "" : tokenObj.get("Token").toString();
			
			RequestBody apiReqBody = RequestBody.create(request, mediaType);
			Request apiReq = new Request.Builder().addHeader("Authorization", "Bearer " + token).url(url)
					.post(apiReqBody).build();
			
			response = httpClient.newCall(apiReq).execute();
			apiReponse = response.body().string();
		}
		catch(Exception e) {
		e.printStackTrace();
		}
		return apiReponse;
	}
	
	@SuppressWarnings("unchecked")
	public String callZambiaComApi(String url, String request) {
		String apiReponse = "";
		try {
			Response response = null;
			Map<String, Object> tokReq = new HashMap<String, Object>();
			tokReq.put("LoginId", "Zambia_whatsapp");
			tokReq.put("Password", "Admin@01");
			tokReq.put("ReLoginKey", "Y");
			
			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
					}
			};
			
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom()); 

			httpClient = new OkHttpClient.Builder().sslSocketFactory(sslContext.getSocketFactory(),(X509TrustManager)trustAllCerts[0]).readTimeout(60, TimeUnit.SECONDS)
					.connectTimeout(60, TimeUnit.SECONDS).build();
			
			String tokenJsonReq = new Gson().toJson(tokReq);
			String tokenApi = cs.getwebserviceurlProperty().getProperty("wh.phoenix.token.api");
			
			RequestBody tokenReqBody = RequestBody.create(tokenJsonReq, mediaType);
			Request tokenReq = new Request.Builder().url(tokenApi).post(tokenReqBody).build();
			response = httpClient.newCall(tokenReq).execute();
			String obj = response.body().string();	
			Map<String, Object> tokenRes = mapper.readValue(obj, Map.class);
			Map<String, Object> tokenObj = tokenRes.get("Result") == null ? null
					: (Map<String, Object>) tokenRes.get("Result");
			String token = tokenObj.get("Token") == null ? "" : tokenObj.get("Token").toString();
			
			RequestBody apiReqBody = RequestBody.create(request, mediaType);
			Request apiReq = new Request.Builder().addHeader("Authorization", "Bearer " + token).url(url)
					.post(apiReqBody).build();
			
			response = httpClient.newCall(apiReq).execute();
			apiReponse = response.body().string();
		}
		catch(Exception e) {
		e.printStackTrace();
		}
		return apiReponse;
	}
	
	@SuppressWarnings("unchecked")
	public String callSwazilandComApi(String url, String request) {
		String apiReponse = "";
		try {
			Response response = null;
			Map<String, Object> tokReq = new HashMap<String, Object>();
			tokReq.put("LoginId", "SZL_Whatsapp");
			tokReq.put("Password", "Admin@01");
			tokReq.put("ReLoginKey", "Y");
			
			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
					}
			};
			
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom()); 

			httpClient = new OkHttpClient.Builder().sslSocketFactory(sslContext.getSocketFactory(),(X509TrustManager)trustAllCerts[0]).readTimeout(60, TimeUnit.SECONDS)
					.connectTimeout(60, TimeUnit.SECONDS).build();
			
			String tokenJsonReq = new Gson().toJson(tokReq);
			String tokenApi = cs.getwebserviceurlProperty().getProperty("wh.phoenix.token.api");
			
			RequestBody tokenReqBody = RequestBody.create(tokenJsonReq, mediaType);
			Request tokenReq = new Request.Builder().url(tokenApi).post(tokenReqBody).build();
			response = httpClient.newCall(tokenReq).execute();
			String obj = response.body().string();	
			Map<String, Object> tokenRes = mapper.readValue(obj, Map.class);
			Map<String, Object> tokenObj = tokenRes.get("Result") == null ? null
					: (Map<String, Object>) tokenRes.get("Result");
			String token = tokenObj.get("Token") == null ? "" : tokenObj.get("Token").toString();
			
			RequestBody apiReqBody = RequestBody.create(request, mediaType);
			Request apiReq = new Request.Builder().addHeader("Authorization", "Bearer " + token).url(url)
					.post(apiReqBody).build();
			
			response = httpClient.newCall(apiReq).execute();
			apiReponse = response.body().string();
		}
		catch(Exception e) {
		e.printStackTrace();
		}
		return apiReponse;
	}
	
	public String getSwazilandToken() {
		try {
			Map<String, Object> tokenReq = new HashMap<String,Object>();
			tokenReq.put("LoginId", "SZL_Whatsapp");
			tokenReq.put("Password", "Admin@01");
			tokenReq.put("ReLoginKey", "Y");
			
			//Response response = null;
			String token ="";
			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
					}
			};
			
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom()); 

			httpClient = new OkHttpClient.Builder().sslSocketFactory(sslContext.getSocketFactory(),(X509TrustManager)trustAllCerts[0]).readTimeout(60, TimeUnit.SECONDS)
					.connectTimeout(60, TimeUnit.SECONDS).build();
			
			String tokenJsonReq = new Gson().toJson(tokenReq);
			String tokenApi = cs.getwebserviceurlProperty().getProperty("wh.phoenix.token.api");
			
			RequestBody tokenReqBody = RequestBody.create(tokenJsonReq, mediaType);
			Request tokenRequest = new Request.Builder().url(tokenApi).post(tokenReqBody).build();
		/*	response = httpClient.newCall(tokenRequest).execute();
			String obj = response.body().string();	
			Map<String, Object> tokenRes = mapper.readValue(obj, Map.class);
			Map<String, Object> tokenObj = tokenRes.get("Result") == null ? null
					: (Map<String, Object>) tokenRes.get("Result");
			String token = tokenObj.get("Token") == null ? "" : tokenObj.get("Token").toString();
			*/
			
			try (Response response = httpClient.newCall(tokenRequest).execute()) {
				if (response.isSuccessful() && response.body() != null) {
					String obj = response.body().string();
					System.out.println("Response Body: " + obj); // Debug print

					Map<String, Object> tokenRes = mapper.readValue(obj, Map.class);
					Map<String, Object> tokenObj = tokenRes.get("Result") == null ? null
							: (Map<String, Object>) tokenRes.get("Result");
                    token = tokenObj != null && tokenObj.get("Token") != null ? tokenObj.get("Token").toString() : "";

					System.out.println("Token: " + token);
				} else {
					System.out.println("HTTP Error: " + response.code());
				}
			
			return token;
		}catch(Exception e) {
			e.printStackTrace();
		}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
