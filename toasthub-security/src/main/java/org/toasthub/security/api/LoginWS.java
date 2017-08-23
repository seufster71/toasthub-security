package org.toasthub.security.api;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.toasthub.core.common.UtilSvc;
import org.toasthub.core.general.api.View;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.core.general.model.ServiceClass;
import org.toasthub.core.serviceCrawler.MicroServiceClient;
import org.toasthub.core.general.model.AppCacheServiceCrawler;
import org.toasthub.core.general.model.GlobalConstant;

import com.fasterxml.jackson.annotation.JsonView;

@RestController()
@RequestMapping("/api/login")
public class LoginWS {
	
	@Autowired 
	UtilSvc utilSvc;
	
	@Autowired 
	AppCacheServiceCrawler serviceCrawler;
	
	@Autowired
	MicroServiceClient microServiceClient;
	
	@JsonView(View.Public.class)
	@RequestMapping(value = "callService", method = RequestMethod.POST)
	public RestResponse callService(@RequestBody RestRequest request) {
		
		RestResponse response = new RestResponse();
		// set defaults
		utilSvc.setupDefaults(request);
		// validate request
		
		// call service locator
		ServiceClass serviceClass = serviceCrawler.getServiceClass("LOGIN",(String) request.getParams().get(GlobalConstant.SERVICE),
				(String) request.getParam(GlobalConstant.SVCAPIVERSION), (String) request.getParam(GlobalConstant.SVCAPPVERSION));
		// process
		if (serviceClass != null) {
			if ("LOCAL".equals(serviceClass.getLocation()) && serviceClass.getServiceProcessor() != null) {
				// use local service
				serviceClass.getServiceProcessor().process(request, response);
			} else {
				// use remote service
				request.addParam(GlobalConstant.MICROSERVICENAME, "toasthub-m-security");
				request.addParam(GlobalConstant.MICROSERVICEPATH, "api/login/callService");
				microServiceClient.process(request, response);
			}
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
