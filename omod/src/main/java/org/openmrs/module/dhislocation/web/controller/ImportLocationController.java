package org.openmrs.module.dhislocation.web.controller;

import static org.openmrs.module.dhislocation.constant.Dhis2Constants.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.LocationTag;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.dhislocation.constant.Dhis2Constants;
import org.openmrs.module.dhislocation.constant.Dhis2Utils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mysql.jdbc.StringUtils;

@Controller
public class ImportLocationController {

	private LocationAttributeType dhisCodeAttr;
    private LocationAttributeType dhisidAttr;
    private LocationAttributeType dhisuuidAttr;
	private int rowsProcessed;
    
    @RequestMapping( value = "/module/dhislocation/importDhisLocations")
	public void importLocations(Map model, HttpServletRequest request) throws MalformedURLException, IOException {
    	rowsProcessed = 0;
    	dhisCodeAttr = getOrCreateLocationAttributeType(LOC_ATTR_DHIS_CODE_NAME);
		dhisidAttr = getOrCreateLocationAttributeType(LOC_ATTR_DHIS_OU_ID_NAME);
		dhisuuidAttr = getOrCreateLocationAttributeType(LOC_ATTR_DHIS_OU_UUID_NAME);

		String startP = request.getParameter("currentPage");
		String msg = "Locations synced successfully...";
	    JsonObject root = null;
	    JsonObject pager = null;
    	try{
    		root = Dhis2Utils.getOrganisationalUnitList(StringUtils.isEmptyOrWhitespaceOnly(startP)?null:Integer.parseInt(startP));
    		pager = root.get("pager").getAsJsonObject();
    		
			while (true){
		    	JsonArray orgUnits = root.get(ORG_UNIT_KEY).getAsJsonArray();
	
	    		for (JsonElement nel : orgUnits) {
	    			createOrUpdateLocation(nel.getAsJsonObject());
	    		}
		    	
		    	if(pager.has(ORG_UNIT_NEXT_PAGE_KEY)){
		    		root = Dhis2Utils.getOrganisationalUnitList(pager.get(ORG_UNIT_PAGE_KEY).getAsInt()+1);
		    		pager = root.getAsJsonObject("pager");
		    	}
		    	else {
		    		break;
		    	}
		    }
    	}
    	catch(Exception e){
    		e.printStackTrace();
    		msg = "Error syncing locations.... "+e.getMessage();
    	}
    	
    	if(pager != null) {
			msg += "<br><br>Details of activity are as follows <br><br>";
			msg += "<br>Total locations :"+pager.get("total");
			msg += "<br>Total pages :"+pager.get("pageCount");
			msg += "<br>Pages synced so far :"+pager.get("page");
			msg += "<br>Rows created or updated : "+rowsProcessed;
		}
    	
		model.put("message", msg );
		model.put("currentPage", pager != null?pager.get("page"):null);
	}

    private Location createOrUpdateLocation(JsonObject ou) throws MalformedURLException, IOException, ParseException{
    	rowsProcessed++;
    	
    	String orgUnitId = ou.get(ORG_UNIT_ID_KEY).getAsString();
    	
    	Map<LocationAttributeType, Object> attr = new HashMap<LocationAttributeType, Object>();
		attr.put(dhisidAttr, orgUnitId);
		
		Location l = null;

		List<Location> loc = Context.getLocationService().getLocations(null, null, attr , true, 0, 2);
		if(loc.size() > 1){
			throw new IllegalStateException("Multiple locations with same DHIS ID : " +loc);
		}
		else if(loc.size() == 1){
			l = loc.get(0);
		}
		
		System.out.println("Processing OU :: "+ou);
		JsonObject oudet = Dhis2Utils.getOrganisationalUnit(orgUnitId);
		
		String locationName = oudet.get(ORG_UNIT_NAME_KEY).getAsString();
		
		if(l == null){// if no location found with an id existing in dhis find by name
			l = Context.getLocationService().getLocation(locationName);
		}
		
		// if still null create a new one
		if(l == null){
			l = new Location();
		}

		for (LocationAttribute lt : l.getAttributes()) {
			if(lt.getAttributeType().getName().equalsIgnoreCase(LOC_ATTR_DHIS_OU_ID_NAME)){
				if(lt.getDateCreated().after(parseDhisDate((oudet.get("lastUpdated").getAsString())))){
					return l;
				}
			}
		}
				
		if (oudet.has(ORG_UNIT_CODE_KEY)) {
			String codeval = oudet.get(ORG_UNIT_CODE_KEY).getAsString();
			LocationAttribute code = findLocationAttribute(dhisCodeAttr.getName(), l);
			if(code != null){
				code.setValueReferenceInternal(codeval);				
			}
			else {
				code = new LocationAttribute();
				code.setAttributeType(dhisCodeAttr);
				code.setValueReferenceInternal(codeval);

				l.addAttribute(code);
			}
		}

		if (oudet.has(ORG_UNIT_UUID_KEY)) {
			String uuidval = oudet.get(ORG_UNIT_UUID_KEY).getAsString();
			LocationAttribute uuid = findLocationAttribute(dhisuuidAttr.getName(), l);
			if(uuid != null){
				uuid.setValueReferenceInternal(uuidval);				
			}
			else {
				uuid = new LocationAttribute();
				uuid.setAttributeType(dhisuuidAttr);
				uuid.setValueReferenceInternal(uuidval);
				
				l.addAttribute(uuid);
			}
		}

		if (oudet.has(ORG_UNIT_ID_KEY)) {
			String idval = oudet.get(ORG_UNIT_ID_KEY).getAsString();
			LocationAttribute id = findLocationAttribute(dhisidAttr.getName(), l);
			if(id != null){
				id.setValueReferenceInternal(idval);
			}
			else {
				id = new LocationAttribute();
				id.setAttributeType(dhisidAttr);
				id.setValueReferenceInternal(idval);
				
				l.addAttribute(id);
			}
		}
		
		if(oudet.has(ORG_UNIT_ORG_GROUP_KEY)){
			JsonArray orgunitgrps = oudet.getAsJsonArray(ORG_UNIT_ORG_GROUP_KEY);
			for (JsonElement ogroup : orgunitgrps) {
				JsonObject gp = ogroup.getAsJsonObject();
				JsonObject fullGroup = Dhis2Utils.getOrganisationalUnitGroup(gp.get(ORG_UNIT_ORG_GROUP_ID_KEY).getAsString());
				LocationTag tag = getOrCreateLocationTag(fullGroup.get(ORG_UNIT_ORG_GROUP_NAME_KEY).getAsString());
				l.addTag(tag);
			}
		}
		
		/*l.setAddress1(address1);
		l.setAddress2(address2);
		l.setAddress3(address3);
		l.setAddress4(address4);
		l.setAddress5(address5);
		l.setAddress6(address6);*/
		/*l.setCityVillage(cityVillage);
		l.setCountry(country);
		l.setCountyDistrict(countyDistrict);
		l.setDescription(description);*/
		//l.setLatitude(latitude);
		//l.setLongitude(longitude);
		l.setName(locationName);
		//l.setPostalCode(postalCode);
		
		if(oudet.has(ORG_UNIT_ACTIVE_KEY)){
			l.setRetired(!oudet.get(ORG_UNIT_ACTIVE_KEY).getAsBoolean());
		}
		
		if(l.getRetired()){
		l.setRetireReason("Import locations activity retired locations");
		l.setRetiredBy(new User(2));
		}
		l.setChangedBy(new User(2));
		l.setDateChanged(new Date());
		//l.setStateProvince(stateProvince);
		//l.setTags(tags);
		if(oudet.has(ORG_UNIT_PARENT_KEY)){
			JsonObject par = oudet.getAsJsonObject(ORG_UNIT_PARENT_KEY);

			Location p = getLocation(par);
			l.setParentLocation(p == null?createOrUpdateLocation(par):p);
		}
		System.out.println("LOC :: "+l);
		return Context.getLocationService().saveLocation(l);
    }
    private Location getLocation(JsonObject ou) throws MalformedURLException, IOException{
    	Map<LocationAttributeType, Object> attr = new HashMap<LocationAttributeType, Object>();
		attr.put(dhisidAttr, ou.get(ORG_UNIT_ID_KEY).getAsString());
		
		Location l = null;

		List<Location> loc = Context.getLocationService().getLocations(null, null, attr , true, 0, 2);
		if(loc.size() > 1){
			throw new IllegalStateException("Multiple locations with same DHIS ID : " +loc);
		}
		else if(loc.size() == 1){
			l = loc.get(0);
		}
		return l;
    }
    
    private LocationAttribute findLocationAttribute(String attributeName, Location location){
    	for (LocationAttribute lat : location.getAttributes()) {
			if(lat.getAttributeType().getName().equalsIgnoreCase(attributeName)){
				return lat;
			}
		}
		return null;
    }
    
    private LocationAttributeType getOrCreateLocationAttributeType(String attributeName){
    	for (LocationAttributeType att : Context.getLocationService().getAllLocationAttributeTypes()) {
			if(att.getName().equalsIgnoreCase(attributeName)){
				return att;
			}
		}

    	LocationAttributeType lat = new LocationAttributeType();
    	lat.setName(attributeName);
    	lat.setDescription(attributeName);
    	lat.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
    	lat.setCreator(new User(2));
    	lat.setDateCreated(new Date());
		return Context.getLocationService().saveLocationAttributeType(lat );
    }
    
    private LocationTag getOrCreateLocationTag(String tagName){
    	for (LocationTag tag : Context.getLocationService().getAllLocationTags()) {
			if(tag.getName().equalsIgnoreCase(tagName)){
				return tag;
			}
		}

    	LocationTag ltag = new LocationTag();
    	ltag.setName(tagName);
    	ltag.setDescription(tagName);
    	ltag.setCreator(new User(2));
    	ltag.setDateCreated(new Date());
		return Context.getLocationService().saveLocationTag(ltag);
    }
}
