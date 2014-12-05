package org.openmrs.module.dhislocation.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.openmrs.module.dhislocation.constant.Dhis2Constants.*;

@Controller
public class Dhis2ConfigController
{
    protected final Log log = LogFactory.getLog( getClass() );
    
    @RequestMapping( value = "/module/dhislocation/configureDhis2", method = RequestMethod.GET )
    public void showConfigForm( ModelMap model )
    {
    	GlobalProperty url = Context.getAdministrationService().getGlobalPropertyObject(URL_PROPERTY);
        GlobalProperty user = Context.getAdministrationService().getGlobalPropertyObject(USERNAME_PROPERTY);
        GlobalProperty pwd = Context.getAdministrationService().getGlobalPropertyObject(PASSWORD_PROPERTY);
        
        if(url == null) 
        	Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(URL_PROPERTY, "", "DHIS2 server URL"));
        if(user == null)
        	Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(USERNAME_PROPERTY, "", "DHIS2 server login username"));
        if(pwd == null) 
        	Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(PASSWORD_PROPERTY, "", "DHIS2 server login password"));
    }
}
