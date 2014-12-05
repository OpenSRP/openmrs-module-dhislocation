package org.openmrs.module.dhislocation.constant;

import static org.openmrs.module.dhislocation.constant.Dhis2Constants.PASSWORD_PROPERTY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.URL_PROPERTY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.USERNAME_PROPERTY;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.NoSuchElementException;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;

import com.mysql.jdbc.StringUtils;
import com.sun.org.apache.xml.internal.security.utils.Base64;

public class Dhis2Utils {

	public static Map<?,?> queryDhisServer(String serviceUrl, boolean appendServerUrl) throws IOException {
		String url = Context.getAdministrationService().getGlobalProperty(URL_PROPERTY)+ serviceUrl;
        String user = Context.getAdministrationService().getGlobalProperty(USERNAME_PROPERTY);
        String pwd = Context.getAdministrationService().getGlobalProperty(PASSWORD_PROPERTY);

        if(StringUtils.isEmptyOrWhitespaceOnly(url) || StringUtils.isEmptyOrWhitespaceOnly(user) || StringUtils.isEmptyOrWhitespaceOnly(pwd)){
        	throw new NoSuchElementException("Dhis server configuration should be completed before accessing the service");
        }
        
        try{
			HttpURLConnection conn = (HttpURLConnection) new URL(appendServerUrl?url:serviceUrl).openConnection();
			String auth = new String(Base64.encode((user+":"+pwd).getBytes ("UTF-8")));
	        conn.setRequestProperty ("Authorization", "Basic " + auth);
	        
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			ObjectMapper mapper = new ObjectMapper();
		    return mapper.readValue(br, Map.class);
        }
        catch(Exception e){
        	throw new IOException("Error connecting dhis server " + url + " : ", e);
        }
	}
	
	public static Map<?, ?> getOrganisationalUnitList(Integer page) throws MalformedURLException, IOException {
		return queryDhisServer(Dhis2Constants.ORG_UNIT_LIST_URL+(page==null?"":"?page="+page), true);
	}
	
	public static Map<?, ?> getOrganisationalUnit(String url) throws MalformedURLException, IOException {
		return queryDhisServer(url, false);
	}
	
	public static void main(String[] args) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new SimpleDateFormat("yyyy-MM-dd").parse("2014-03-02T21:16:07.359+0000".replaceFirst("T", "	").substring(0,19)));
		String dt="2014-03-02T21:16:07.359+0000".replaceFirst("T", " ").substring(0,19);
		System.out.print(dt+":"+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dt));
	}
}
