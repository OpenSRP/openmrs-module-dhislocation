package org.openmrs.module.dhislocation.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping( value = "/module/dhislocation/configureDhis2")
public class Dhis2ConfigController
{
    protected final Log log = LogFactory.getLog( getClass() );
    
	@RequestMapping(method = RequestMethod.GET)
    public void showConfigForm( ModelMap model )
    {
    	// do nothing for now. just show the page for property editor
    }
}
