package org.openmrs.module.dhislocation.constant;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Dhis2Constants {
	public static final String PROPERTY_PREFIX = "dhislocation.";
	public static final String URL_PROPERTY = PROPERTY_PREFIX + "server_url";
	public static final String USERNAME_PROPERTY = PROPERTY_PREFIX + "dhis_username";
	public static final String PASSWORD_PROPERTY = PROPERTY_PREFIX + "dhis_password";
	public static final String LAST_SYNC_DETAILS_PROPERTY = PROPERTY_PREFIX + "last_sync_pager";
	public static final String DHIS_SYNC_LOG_PATH = System.getProperty("user.home")+"/dhis_sync.txt";
	
	public static final String ORG_UNIT_KEY = "organisationUnits";
	public static final String ORG_UNIT_ID_KEY = "id";
	public static final String ORG_UNIT_NAME_KEY = "name";
	public static final String ORG_UNIT_CODE_KEY = "code";
	public static final String ORG_UNIT_UUID_KEY = "uuid";
	public static final String ORG_UNIT_ACTIVE_KEY = "active";
	public static final String ORG_UNIT_PARENT_KEY = "parent";
	public static final String ORG_UNIT_NEXT_PAGE_KEY = "nextPage";
	public static final String ORG_UNIT_PAGE_KEY = "page";
	
	public static final String ORG_UNIT_ORG_GROUP_KEY = "organisationUnitGroups";
	public static final String ORG_UNIT_ORG_GROUP_ID_KEY = "id";
	public static final String ORG_UNIT_ORG_GROUP_NAME_KEY = "name";

	public static final String ORG_UNIT_ORG_GROUP_TAGMAP_KEY = "name";

	public static final String LOC_ATTR_DHIS_CODE_NAME = "dhis_code";
	public static final String LOC_ATTR_DHIS_OU_ID_NAME = "dhis_ou_id";
	public static final String LOC_ATTR_DHIS_OU_UUID_NAME = "dhis_ou_uuid";
	
	public static final String ORG_UNIT_LIST_URL = "/api/organisationUnits";
	public static final String ORG_UNIT_GROUP_LIST_URL = "/api/organisationUnitGroups";

	public static Date parseDhisDate(String date) throws ParseException{
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		date = date.replaceFirst("T", " ");
		try{
			return sdf.parse(date);
		}
		catch(ParseException e){
			sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
			return sdf.parse(date);
		}
	}
	
    public static void main(String[] args) throws ParseException {
		System.out.println(parseDhisDate("2011-02-23T07:53:59.000+0000"));
	}
}
