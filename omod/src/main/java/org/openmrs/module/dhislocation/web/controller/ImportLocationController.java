package org.openmrs.module.dhislocation.web.controller;

import static org.openmrs.module.dhislocation.constant.Dhis2Constants.LOC_ATTR_DHIS_CODE_NAME;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.LOC_ATTR_DHIS_OU_ID_NAME;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.LOC_ATTR_DHIS_OU_UUID_NAME;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.ORG_UNIT_ACTIVE_KEY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.ORG_UNIT_CODE_KEY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.ORG_UNIT_ID_KEY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.ORG_UNIT_KEY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.ORG_UNIT_NAME_KEY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.ORG_UNIT_NEXT_PAGE_KEY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.ORG_UNIT_PAGE_KEY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.ORG_UNIT_PARENT_KEY;
import static org.openmrs.module.dhislocation.constant.Dhis2Constants.ORG_UNIT_UUID_KEY;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.dhislocation.constant.Dhis2Utils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ImportLocationController {

	private LocationAttributeType dhisCodeAttr;
    private LocationAttributeType dhisidAttr;
    private LocationAttributeType dhisuuidAttr;
	private int rowsProcessed;
    
    @RequestMapping( value = "/module/dhislocation/importDhisLocations")
	public void importLocations(Map model) throws MalformedURLException, IOException {
    	rowsProcessed = 0;
    	dhisCodeAttr = getOrCreateLocationAttributeType(LOC_ATTR_DHIS_CODE_NAME);
		dhisidAttr = getOrCreateLocationAttributeType(LOC_ATTR_DHIS_OU_ID_NAME);
		dhisuuidAttr = getOrCreateLocationAttributeType(LOC_ATTR_DHIS_OU_UUID_NAME);

		String msg = "Locations synced successfully...";
	    Map<?, ?> rootAsMap = new HashMap();
    	Map<String,?> pager = new HashMap();
    	try{
    		rootAsMap = Dhis2Utils.getOrganisationalUnitList(null);
    		pager = (Map<String, ?>) rootAsMap.get("pager");
    		
			while (true){
		    	List<?> orgUnits = (List<?>) rootAsMap.get(ORG_UNIT_KEY);
	
		    	if(orgUnits != null && orgUnits.size() > 0){
		    		for (Object nel : orgUnits) {
		    			createOrUpdateLocation((Map<String, ?>) nel);
		    		}
		    	}
		    	
		    	if(pager.get(ORG_UNIT_NEXT_PAGE_KEY) != null){
		    		rootAsMap = Dhis2Utils.getOrganisationalUnitList((Integer) pager.get(ORG_UNIT_PAGE_KEY)+1);
		    		pager = (Map<String, ?>) rootAsMap.get("pager");
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
    	
    	if(pager.size()>0) {
			msg += "<br><br>Details of activity are as follows <br><br>";
			msg += "<br>Total locations :"+pager.get("total");
			msg += "<br>Total pages :"+pager.get("pageCount");
			msg += "<br>Pages synced so far :"+pager.get("page");
			msg += "<br>Rows created or updated : "+rowsProcessed;
		}
    	
		model.put("message", msg );
	}

    private Location createOrUpdateLocation(Map<?, ?> ou) throws MalformedURLException, IOException, ParseException{
    	rowsProcessed++;
    	
    	Map<LocationAttributeType, Object> attr = new HashMap<LocationAttributeType, Object>();
		attr.put(dhisidAttr, ou.get(ORG_UNIT_ID_KEY));
		
		Location l = null;

		List<Location> loc = Context.getLocationService().getLocations(null, null, attr , true, 0, 2);
		if(loc.size() > 1){
			throw new IllegalStateException("Multiple locations with same DHIS ID : " +loc);
		}
		else if(loc.size() == 1){
			l = loc.get(0);
		}
		else {
			l = new Location();
		}
		
		for (LocationAttribute lt : l.getAttributes()) {
			if(lt.getAttributeType().getName().equalsIgnoreCase(LOC_ATTR_DHIS_OU_ID_NAME)){
				if(lt.getDateCreated().after(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(ou.get("lastUpdated").toString().replaceFirst("T", " ")))){
					return l;
				}
			}
		}
		
		
		System.out.println("OU :: "+ou);
		Map<?, ?> oudet = Dhis2Utils.getOrganisationalUnit((String) ou.get("href"));

		l.getAttributes().clear();// needed , else it would create duplicates
		
		Object codeval = oudet.get(ORG_UNIT_CODE_KEY);
		LocationAttribute code = new LocationAttribute();
		if (codeval != null) {
			code.setAttributeType(dhisCodeAttr);
			code.setValueReferenceInternal((String) codeval);
			
			l.addAttribute(code);
		}

		Object uuidval = oudet.get(ORG_UNIT_UUID_KEY);
		LocationAttribute uuid = new LocationAttribute();
		if (uuidval != null) {
			uuid.setAttributeType(dhisuuidAttr);
			uuid.setValueReferenceInternal((String) uuidval);
			
			l.addAttribute(uuid);
		}

		Object idval = oudet.get(ORG_UNIT_ID_KEY);
		LocationAttribute id = new LocationAttribute();
		if (idval != null) {
			id.setAttributeType(dhisidAttr);
			id.setValueReferenceInternal((String) idval);

			l.addAttribute(id);
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
		l.setName((String) oudet.get(ORG_UNIT_NAME_KEY));
		//l.setPostalCode(postalCode);
		l.setRetired(!(Boolean) oudet.get(ORG_UNIT_ACTIVE_KEY));
		if(l.getRetired()){
		l.setRetireReason("Import locations activity retired locations");
		l.setRetiredBy(new User(2));
		}
		l.setChangedBy(new User(2));
		l.setDateChanged(new Date());
		//l.setStateProvince(stateProvince);
		//l.setTags(tags);
		Map<?, ?> par = (Map<?, ?>) oudet.get(ORG_UNIT_PARENT_KEY);
		if(par != null && par.size() > 0){
			Location p = getLocation(par);
			l.setParentLocation(p == null?createOrUpdateLocation(par):p);
		}
		System.out.println("LOC :: "+l);
		return Context.getLocationService().saveLocation(l);
    }
    private Location getLocation(Map<?, ?> ou) throws MalformedURLException, IOException{
    	Map<LocationAttributeType, Object> attr = new HashMap<LocationAttributeType, Object>();
		attr.put(dhisidAttr, ou.get(ORG_UNIT_ID_KEY));
		
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
}
