package org.openmrs.module.dhislocation.constant;

import static org.openmrs.module.dhislocation.constant.Dhis2Constants.PASSWORD_PROPERTY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.URL_PROPERTY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.USERNAME_PROPERTY;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.xerces.impl.dv.util.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mysql.jdbc.StringUtils;

public class Dhis2Utils {

	public static JsonObject queryDhisServer(String serviceUrl, boolean appendServerUrl) throws IOException {
		String url = Context.getAdministrationService().getGlobalProperty(URL_PROPERTY)+ serviceUrl;
        String user = Context.getAdministrationService().getGlobalProperty(USERNAME_PROPERTY);
        String pwd = Context.getAdministrationService().getGlobalProperty(PASSWORD_PROPERTY);

        if(StringUtils.isEmptyOrWhitespaceOnly(url) || StringUtils.isEmptyOrWhitespaceOnly(user) || StringUtils.isEmptyOrWhitespaceOnly(pwd)){
        	throw new NoSuchElementException("Dhis server configuration should be completed before accessing the service");
        }
        
        int maxtries = 3;
        while(true){
	        try{
				HttpURLConnection conn = (HttpURLConnection) new URL(appendServerUrl?url:serviceUrl).openConnection();
				String auth = new String(Base64.encode((user+":"+pwd).getBytes ("UTF-8")));
		        conn.setRequestProperty ("Authorization", "Basic " + auth);
		        conn.setRequestProperty("Content-Type", "application/json");
		        
				if (conn.getResponseCode() != 200) {
					throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
				}
				
				BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
				
				// DONOT UNCOMMENT 
				// System.out.println(getStringFromInputStream(br));
				return new JsonParser().parse(br).getAsJsonObject();
	        }
	        catch(Exception e){
	        	if(maxtries>0){
	        		maxtries--;
	        		try {
						Thread.sleep(1000*10);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
	        	}
	        	else{
	        		throw new IOException("Error connecting dhis server " + url + " : ", e);
	        	}
	        }
        }
	}
	
	// convert InputStream to String
		private static String getStringFromInputStream(BufferedReader reader) throws IOException {
			StringBuilder builder = new StringBuilder();
			String aux = "";

			while ((aux = reader.readLine()) != null) {
			    builder.append(aux);
			}

			return builder.toString();
		}
	
	public static JsonObject getOrganisationalUnitList(Integer page) throws MalformedURLException, IOException {
		return queryDhisServer(Dhis2Constants.ORG_UNIT_LIST_URL+".json"+(page==null?"":"?page="+page), true);
	}
	
	public static JsonObject getOrganisationalUnit(String id) throws MalformedURLException, IOException {
		return queryDhisServer(Dhis2Constants.ORG_UNIT_LIST_URL+"/"+id+".json", true);
	}
	
	public static JsonObject getOrganisationalUnitGroup(String id) throws MalformedURLException, IOException {
		return queryDhisServer(Dhis2Constants.ORG_UNIT_GROUP_LIST_URL+"/"+id+".json", true);
	}
	
	public static void main(String[] args) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new SimpleDateFormat("yyyy-MM-dd").parse("2014-03-02T21:16:07.359+0000".replaceFirst("T", "	").substring(0,19)));
		String dt="2014-03-02T21:16:07.359+0000".replaceFirst("T", " ").substring(0,19);
		System.out.print(dt+":"+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dt));
	}
}
