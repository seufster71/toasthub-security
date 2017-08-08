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
import org.toasthub.core.general.model.ServiceCrawler;
import org.toasthub.core.general.service.EntityManagerMainSvc;
import org.toasthub.core.general.service.UtilSvc;
import org.toasthub.security.model.BaseEntity;

import com.fasterxml.jackson.annotation.JsonView;
@RestController()
@RequestMapping("/api/login")
public class LoginWS {
	
	@Autowired EntityManagerMainSvc entityManagerMainSvc;
	@Autowired UtilSvc utilSvc;
	@Autowired ServiceCrawler serviceLocator;
	
	@JsonView(View.Public.class)
	@RequestMapping(value = "callService", method = RequestMethod.POST)
	public RestResponse callService(@RequestBody RestRequest request) {
		
		RestResponse response = new RestResponse();
		// set defaults
		utilSvc.setupDefaults(request);
		// validate request
		
		response.addParam(BaseEntity.APPNAME,entityManagerMainSvc.getAppName());
		// response
		response.addParam(BaseEntity.CONTEXTPATH, entityManagerMainSvc.getAppName());
		// call service locator
		ServiceProcessor x = serviceLocator.getService("LOGIN",(String) request.getParams().get(BaseEntity.SERVICE),
				(String) request.getParam(BaseEntity.SVCAPIVERSION), (String) request.getParam(BaseEntity.SVCAPPVERSION),
				entityManagerMainSvc.getAppDomain());
		// process 
		if (x != null) {
			x.process(request, response);
		} else {
		
		}
		// response
		response.addParam(BaseEntity.PAGESTART, request.getParam(BaseEntity.PAGESTART));
		response.addParam(BaseEntity.PAGELIMIT, request.getParam(BaseEntity.PAGELIMIT));
		return response;
	}
	
	@JsonView(View.Public.class)
	@RequestMapping(value = "authenticate", method = RequestMethod.POST)
	public void authenticate(HttpServletRequest request) {
		
		//RestResponse response = new RestResponse();
		//Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		//if (auth != null && auth.isAuthenticated()){
		//	utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, "TRUE", response);
		//}
		//return response;
	}
}
