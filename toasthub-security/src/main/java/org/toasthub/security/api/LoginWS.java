package org.toasthub.security.api;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.toasthub.core.general.api.View;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.core.general.model.AppCacheServiceCrawler;
import org.toasthub.core.general.service.UtilSvc;
import org.toasthub.security.model.BaseEntity;

import com.fasterxml.jackson.annotation.JsonView;

@RestController()
@RequestMapping("/api/login")
public class LoginWS {
	
	@Autowired UtilSvc utilSvc;
	@Autowired AppCacheServiceCrawler serviceLocator;
	
	@JsonView(View.Public.class)
	@RequestMapping(value = "callService", method = RequestMethod.POST)
	public RestResponse callService(@RequestBody RestRequest request) {
		
		RestResponse response = new RestResponse();
		// set defaults
		utilSvc.setupDefaults(request);
		// validate request
		
		// call service locator
		ServiceProcessor x = serviceLocator.getServiceProcessor("LOGIN",(String) request.getParams().get(BaseEntity.SERVICE),
				(String) request.getParam(BaseEntity.SVCAPIVERSION), (String) request.getParam(BaseEntity.SVCAPPVERSION));
		// process 
		if (x != null) {
			x.process(request, response);
		} else {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, "Service is not available", response);
		}

		return response;
	}
	
	@JsonView(View.Public.class)
	@RequestMapping(value = "authenticate", method = RequestMethod.POST)
	public void authenticate(HttpServletRequest request) {
		
		// This is placeholder for filter
	}
}
