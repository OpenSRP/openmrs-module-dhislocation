package org.openmrs.module.dhislocation.web.controller;

import static org.openmrs.module.dhislocation.constant.Dhis2Constants.*;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmrs.GlobalProperty;
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
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mysql.jdbc.StringUtils;

@Controller
@RequestMapping("/module/dhislocation/importDhisLocations")
public class ImportLocationController {

	private LocationAttributeType dhisCodeAttr;
    private LocationAttributeType dhisidAttr;
    private LocationAttributeType dhisuuidAttr;
   
	private int rowsProcessed;
	private boolean syncUnderProgress;
	
	@RequestMapping(value = "/log")
	public void getFile(HttpServletResponse response) {
	    try {
	    	response.setContentType("text/plain");
	    	response.setContentType("application/force-download");
	    	response.setHeader("Content-Disposition","attachment; filename=\"" + "dhis_sync.log\"");
	    	org.apache.commons.io.IOUtils.copy(new FileInputStream(DHIS_SYNC_LOG_PATH), response.getOutputStream());
	    	response.flushBuffer();
	    } catch (IOException ex) {
	      throw new RuntimeException("IOError writing file to output stream");
	    }
	}
	
	@RequestMapping(value = "/stopSync")
	public String stopSync() {
		syncUnderProgress = false;
		return "redirect:/module/dhislocation/importDhisLocations.form";
	}

    @RequestMapping(method=RequestMethod.GET)
	public void showForm(Map model, HttpServletRequest request) {
    	String msg = "<br><br>Details of last sync performed are as follows <br><br>";
    	try{
    		GlobalProperty pagerP = Context.getAdministrationService().getGlobalPropertyObject(LAST_SYNC_DETAILS_PROPERTY);
		
	    	if(pagerP != null && pagerP.getValue() != null && !StringUtils.isEmptyOrWhitespaceOnly(pagerP.getValue().toString())) {
	    		JsonObject pager = new JsonParser().parse(pagerP.getPropertyValue()).getAsJsonObject();
				msg += "<br>Total locations :"+pager.get("total");
				msg += "<br>Total pages :"+pager.get("pageCount");
				msg += "<br>Pages synced so far :"+pager.get("page");
	
				model.put("lastPage", pager != null?pager.get("page"):null);
	    	}
    	}
    	catch(Exception e){
    		e.printStackTrace();
    		msg += "Error: "+e.getMessage();
    	}
    	model.put("syncUnderProgress", syncUnderProgress);
    	model.put("message", msg );
	}
	
    @RequestMapping(method=RequestMethod.POST)
	public void importLocations(Map model, HttpServletRequest request) throws MalformedURLException, IOException {
    	rowsProcessed = 0;
    	syncUnderProgress = true;
    	
    	String startP = request.getParameter("currentPage");
    	
    	System.out.println("Starting from page "+startP);
    	    	
    	if(StringUtils.isEmptyOrWhitespaceOnly(startP)){
	    	model.put("message", "Error: A valid value for page to start sync with must be specified");
			model.put("lastPage", 0);
			return;
    	}
    	    	
    	dhisCodeAttr = getOrCreateLocationAttributeType(LOC_ATTR_DHIS_CODE_NAME);
		dhisidAttr = getOrCreateLocationAttributeType(LOC_ATTR_DHIS_OU_ID_NAME);
		dhisuuidAttr = getOrCreateLocationAttributeType(LOC_ATTR_DHIS_OU_UUID_NAME);

		String msg = "Locations synced successfully...";
	    JsonObject root = null;
	    JsonObject pager = null;

	    FileWriter log = new FileWriter(DHIS_SYNC_LOG_PATH, false);

	    try{
    		root = Dhis2Utils.getOrganisationalUnitList(StringUtils.isEmptyOrWhitespaceOnly(startP)?null:Integer.parseInt(startP));
    		pager = root.get("pager").getAsJsonObject();
    	
    		try{
	    		Context.getAdministrationService().addGlobalProperty(LAST_SYNC_DETAILS_PROPERTY, pager.toString());;
    		}
    		catch(Exception e){
    			e.printStackTrace();
    		}

    		log.write("\n"+new Date(System.currentTimeMillis())+" - PAGER INFORMATION: "+pager.toString());
    		
			while (true){
		    	JsonArray orgUnits = root.get(ORG_UNIT_KEY).getAsJsonArray();
	
	    		for (JsonElement nel : orgUnits) {
	    			createOrUpdateLocation(nel.getAsJsonObject());
	    			
		    		log.write("\n"+new Date(System.currentTimeMillis())+" - PROCESSING OU: "+nel.getAsJsonObject().toString());
	    		
		    		if(!syncUnderProgress){
			    		log.write("\n"+new Date(System.currentTimeMillis())+" - SYNC STOPPED IN THE MIDDLE ");
			    		break;
		    		}
	    		}
		    	
	    		if(!syncUnderProgress){
		    		log.write("\n"+new Date(System.currentTimeMillis())+" - SYNC STOPPED IN THE MIDDLE ");
		    		break;
	    		}
	    		
		    	if(pager.has(ORG_UNIT_NEXT_PAGE_KEY)){
		    		root = Dhis2Utils.getOrganisationalUnitList(pager.get(ORG_UNIT_PAGE_KEY).getAsInt()+1);
		    		pager = root.getAsJsonObject("pager");
		    		
		    		log.write("\n"+new Date(System.currentTimeMillis())+" - PAGER INFORMATION: "+pager.toString());
		    	}
		    	else {
		    		break;
		    	}
		    }
    	}
    	catch(Exception e){
    		e.printStackTrace();
    		msg = "Error: problem encountered while syncing locations.... "+e.getMessage();
    		
    		log.write( "\n"+new Date(System.currentTimeMillis())+" - Error: problem encountered while syncing locations.... "+e.toString());
    	}
	    finally {
			log.flush();
			log.close();
		}
    	
    	if(pager != null) {
			msg += "<br><br>Details of activity are as follows <br><br>";
			msg += "<br>Total locations :"+pager.get("total");
			msg += "<br>Total pages :"+pager.get("pageCount");
			msg += "<br>Pages synced so far :"+pager.get("page");
			msg += "<br>Rows created or updated : "+rowsProcessed;
		}

    	syncUnderProgress = false;
    	
		model.put("message", msg);
		model.put("lastPage", pager != null?pager.get("page"):null);
		model.put("syncUnderProgress", syncUnderProgress);
		
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
		
		// to prevent skipping location update incase only organizationalUnitGroup have been updated
		Set<LocationTag> dTagList = new HashSet<>();
		if(l.getTags() != null && !l.getTags().isEmpty()){
			dTagList.addAll(l.getTags());
		}
		
		if(oudet.has(ORG_UNIT_ORG_GROUP_KEY)){
			JsonArray orgunitgrps = oudet.getAsJsonArray(ORG_UNIT_ORG_GROUP_KEY);
			for (JsonElement ogroup : orgunitgrps) {
				JsonObject gp = ogroup.getAsJsonObject();
				JsonObject fullGroup = Dhis2Utils.getOrganisationalUnitGroup(gp.get(ORG_UNIT_ORG_GROUP_ID_KEY).getAsString());
				LocationTag tag = getOrCreateLocationTag(fullGroup.get(ORG_UNIT_ORG_GROUP_NAME_KEY).getAsString());
				l.addTag(tag);
				dTagList.add(tag);
			}
		}
		
		boolean newLocationTagAdded = false;
		
		if(l.getTags().size() == dTagList.size()){			
			for (LocationTag dTag : dTagList) {	
				boolean found = false;
			
				for (LocationTag lTag : l.getTags()) {
					if(lTag.getName().equalsIgnoreCase(dTag.getName())){
						found = true;
						break;
					}
				}
				
				if(!found){
					newLocationTagAdded = true;
					break;
				}
			}
		}
				
		for (LocationAttribute lt : l.getAttributes()) {
			if(lt.getAttributeType().getName().equalsIgnoreCase(LOC_ATTR_DHIS_OU_ID_NAME)){
				if(newLocationTagAdded == false
						&& lt.getDateCreated().after(parseDhisDate((oudet.get("lastUpdated").getAsString())))){
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
