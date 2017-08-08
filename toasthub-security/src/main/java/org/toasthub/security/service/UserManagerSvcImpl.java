/*
 * Copyright (C) 2016 The ToastHub Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.toasthub.security.service;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;

import org.picketbox.commons.cipher.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.BaseEntity;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.core.general.service.EntityManagerMainSvc;
import org.toasthub.core.general.service.MailSvc;
import org.toasthub.core.general.service.UtilSvc;
import org.toasthub.core.preference.model.AppCachePage;
import org.toasthub.core.preference.model.AppCachePageUtil;
import org.toasthub.core.preference.model.AppPageOptionValue;
import org.toasthub.security.model.LoginLog;
import org.toasthub.security.model.User;
import org.toasthub.security.model.UserContext;
import org.toasthub.security.repository.UserManagerDao;

@Service("UserManagerSvc")
public class UserManagerSvcImpl implements ServiceProcessor, UserManagerSvc {
	
	@Autowired 
	@Qualifier("UserManagerDao")
	UserManagerDao userManagerDao;
	
	@Autowired 
	MailSvc mailSvc;
	
	//@Autowired Event<NewUserEvent> newUserEvent;
	@Autowired 
	UtilSvc utilSvc;
	
	@Autowired 
	AppCachePage appCachePage;
	
	@Autowired 
	EntityManagerMainSvc entityManagerMainSvc;

	@Autowired 
	UserContext userContext;

	// Constructor
	public UserManagerSvcImpl() {}
	
	// Processor
	public void process(RestRequest request, RestResponse response) {
		String action = (String) request.getParams().get(BaseEntity.ACTION);
		
		switch (action) {
		case "INIT": 
			request.addParam(AppCachePage.APPPAGEPARAMLOC, AppCachePage.RESPONSE);
			appCachePage.getPageInfo(request,response);
			
			if (request.containsParam("urlaction") && "emailconfirm".equals(request.getParam("urlaction"))){
				RestRequest subRequest = new RestRequest();
				subRequest.addParam("inputFields", request.getParam("inputFields"));
				subRequest.addParam(BaseEntity.LANG, request.getParam(BaseEntity.LANG));
				this.confirmEmail(subRequest, response);
			}
			this.init(request,response);
			break;
		case "REGISTERCHECKNAMEINIT":
			this.registerCheckNameInit(request, response);
			break;
		case "REGISTERFULL":
			this.registerFull(request, response);
			break;
		case "REGISTERCHECKUSERNAME":
			this.registerCheckUserName(request, response);
			break;
		case "LOGINAUTHENTICATE":
			//this.userAuthenticate(request, response);
			break;
		case "TOKENAUTHENTICATE":
			//this.userAuthenticate(request, response);
			break;
		case "FORGOTPASSWORD":
			this.forgotPassword(request, response);
			break;
		case "CHANGEPASSWORD":
			this.changePassword(request, response);
			break;
		case "CONFIRMEMAIL":
			this.confirmEmail(request, response);
			break;
		case "LOGOUT":
			logout(request, response);
			break;
		default:
			break;
		}
		
	}
	
	public void init(RestRequest request, RestResponse response) {
		// get custom page Layout
		response.addParam(BaseEntity.PAGELAYOUT,entityManagerMainSvc.getPublicLayout());
	}
	
	public void registerCheckNameInit(RestRequest request, RestResponse response) {
		// get option from main
	
	}
	
	public void registerFull(RestRequest request, RestResponse response) {
		try {
			if (!request.containsParam("appForms")) {
				AppCachePageUtil.addAppForm(request, "REGISTRATION_FORM");
			}
			AppCachePageUtil.addAppText(request, "GLOBAL_SERVICE","REGISTRATION_SERVICE");
			AppCachePageUtil.addAppOption(request, "REGISTRATION_SERVICE");
			
			appCachePage.getPageInfo(request,response);
			
			// service status
			AppPageOptionValue serviceStatus = AppCachePageUtil.getAppOption(request, "REGISTRATION_SERVICE", "REGISTRATION_SERVICE");
			if (serviceStatus.getValue().equals("false")){
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request,"GLOBAL_SERVICE","GLOBAL_SERVICE_DISABLED").getValue(), response);
				return;
			}
			// validate
			utilSvc.validateParams(request, response);
			if ((Boolean) request.getParam(BaseEntity.VALID) == false) {
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "GLOBAL_SERVICE", "GLOBAL_SERVICE_VALIDATION_ERROR").getValue(), response);
				return;
			}
			// create empty user to fill
			request.addParam(BaseEntity.ITEM, new User());
			// marshall
			utilSvc.marshallFields(request, response);
			
			User user = (User) request.getParam(BaseEntity.ITEM);
			// set the default language of user
			user.setLang("en");
			// did password match
			if(!user.getPassword().equals(user.getVerifyPassword())) {
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "REGISTRATION_SERVICE", "REGISTRATION_SERVICE_MATCH_PASSWORD_FAILURE").getValue(), response);
				return;
			}
			
			try {
				
				// create salt
				byte[] salt = utilSvc.generateSalt();
				String ePassword = Base64.encodeBytes(utilSvc.getEncryptedPassword(user.getPassword(),salt));
				// Save user into main db
				user.setPassword(ePassword);
				user.setSalt(Base64.encodeBytes(salt));
				// code for email confirmation
				byte[] emailToken = utilSvc.generateSalt();
				String emailTokenString = Base64.encodeBytes(emailToken);
				// remove any equals signs
				emailTokenString = emailTokenString.replaceAll("=|&","0");
				user.setEmailToken(emailTokenString);
				// session token
				byte[] sessionToken = utilSvc.generateSalt();
				String sessionTokenString = Base64.encodeBytes(sessionToken);
				user.setSessionToken(sessionTokenString);
				// Save user into the single sign on db
				userManagerDao.saveUser(user);
				
				// send email confirmation
				String urlParams = "action=emailconfirm&username="+user.getUsername()+"&token="+emailTokenString;
				mailSvc.sendEmailConfirmation(user.getEmail() ,urlParams);
				utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, AppCachePageUtil.getAppText(request, "REGISTRATION_SERVICE", "REGISTRATION_SUCCESSFUL").getValue(), response);
				
			} 	catch (Exception e) {
				// find root exception
				Throwable rootEx = e.getCause();
				String message = "Registration Failed! Try again";
				while( rootEx != null) {
					if (rootEx.getCause() == null) {
						message = rootEx.getMessage();
						break;
					}
					rootEx = rootEx.getCause();
				}
				//helping the user with messages for existing email address and username
				if (message.contains("Duplicate entry")){
					if (message.contains("uk_useremail")){
						message = "Looks like you are already a member. Your email address is in our list!";
					}else if(message.contains("uk_userpass")){
						message = "Try a different username, the one you selected is already taken!";
					}
				}
		
				// need to do some message cleaning
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "REGISTRATION_SERVICE", "REGISTRATION_FAIL").getValue().concat(message), response);
			}
			response.addParam("appPageFormFields",null);
		} catch (Exception e) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "REGISTRATION_SERVICE", "REGISTRATION_FAIL").getValue(), response);
			e.printStackTrace();
		}
	}
	
	public void registerCheckUserName(RestRequest request, RestResponse response) {
		checkUserName(request, response);
	}
	
	public void authenticate(RestRequest request, RestResponse response) {
		if (!request.containsParam("appForms")) {
			AppCachePageUtil.addAppForm(request, "LOGIN_FORM");
		}
		AppCachePageUtil.addAppText(request, "GLOBAL_SERVICE","LOGIN_SERVICE");
		
		appCachePage.getPageInfo(request,response);
		
		// validate
		utilSvc.validateParams(request, response);
		if ((Boolean) request.getParam(BaseEntity.VALID) == false) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "GLOBAL_SERVICE", "GLOBAL_SERVICE_VALIDATION_ERROR").getValue(), response);
			return;
		}
		
		Map<String,Object> inputList = (Map<String, Object>) request.getParam("inputFields");
		User user = findUser((String) inputList.get("LOGIN_FORM_USERNAME"));
		if (user == null){
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "LOGIN_SERVICE", "LOGIN_SERVICE_BADUSERNAME").getValue(), response);
		} else {
			request.addParam("password", inputList.get("LOGIN_FORM_PASSWORD"));
			boolean authenticated = authenticate(user, request);
			if (authenticated ){
				List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
				 if ( "admin".equals(request.getParams().get("username")) ) {
					 authorities.add(new SimpleGrantedAuthority("ADMIN"));
				 } else {
					 authorities.add(new SimpleGrantedAuthority("PRIVATE"));
				 }
				response.addParam("authorities", authorities);
				response.addParam("user", user);
				userContext.setCurrentUser(user);
				//userContext.loginWS(user);
				//LoginLog loginLog = new LoginLog(user,true);
		    	//logAccess(loginLog);
		    	// return token 
		    	if (request.getParam("action").equals("LOGINAUTHENTICATE")){
		    		response.addParam("token", user.getSessionToken());
		    	}
				utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, "TRUE", response);
			} else if (!user.isEmailConfirm()){
				LoginLog loginLog = new LoginLog(user,false);
		    	logAccess(loginLog);

				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "LOGIN_SERVICE", "LOGIN_SERVICE_CHECK_EMAIL_CONFIRM").getValue(), response);
			} else {
				LoginLog loginLog = new LoginLog(user,false);
		    	logAccess(loginLog);
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "LOGIN_SERVICE", "LOGIN_SERVICE_BADPASSWORD").getValue(), response);
			}
		}
	}
	
	public void forgotPassword(RestRequest request, RestResponse response) {
		try {
			if (!request.containsParam("appForms")) {
				AppCachePageUtil.addAppForm(request, "FORGOTPASSWORD_FORM");
			}
			AppCachePageUtil.addAppText(request, "GLOBAL_SERVICE","FORGOTPASSWORD_SERVICE");
			
			appCachePage.getPageInfo(request,response);
			
			// validate
			utilSvc.validateParams(request, response);
			if ((Boolean) request.getParam(BaseEntity.VALID) == false) {
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "GLOBAL_SERVICE", "GLOBAL_SERVICE_VALIDATION_ERROR").getValue(), response);
				return;
			}
			
			Map<String,Object> inputList = (Map<String, Object>) request.getParam("inputFields");
			String input = (String) inputList.get("FORGOTPASSWORD_FORM_USERNAME");
			User user = null;
			if (input.contains("@")) {
				user = findUserByEmail(input);
			} else {
				user = findUser(input);
			}
			
			if (user == null){
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "FORGOTPASSWORD_SERVICE", "FORGOTPASSWORD_SERVICE_USERMISSING").getValue(), response);
			} else {
				try {
					// create temp password
					
					String password = utilSvc.createRandomPass(8).concat("#1aA");
					
					// create salt
					byte[] salt = utilSvc.generateSalt();
					String ePassword = Base64.encodeBytes(utilSvc.getEncryptedPassword(password,salt));
					
					// change session token
					byte[] sessionToken = utilSvc.generateSalt();
					String sessionTokenString = Base64.encodeBytes(sessionToken);
					
					userManagerDao.resetPassword(user.getUsername(), ePassword, Base64.encodeBytes(salt), sessionTokenString);
					response.addParam("forcePasswordChange", true);
					// send email confirmation
					mailSvc.sendEmailPasswordReset(user.getUsername(), user.getEmail(), password);
					utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, AppCachePageUtil.getAppText(request, "FORGOTPASSWORD_SERVICE", "FORGOTPASSWORD_SERVICE_SUCCESSFUL").getValue(), response);
				} catch (Exception e){
					utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "FORGOTPASSWORD_SERVICE", "FORGOTPASSWORD_SERVICE_PASSWORD_FAIL").getValue(), response);
					e.printStackTrace();
				}
				
			}
		} catch (Exception e) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "FORGOTPASSWORD_SERVICE", "FORGOTPASSWORD_SERVICE_FAIL").getValue(), response);
			e.printStackTrace();
		}
		
	}
	
	public void changePassword(RestRequest request, RestResponse response) {
		if (!request.containsParam("appForms")) {
			AppCachePageUtil.addAppForm(request, "PASSWORD_CHANGE_FORM");
		}
		AppCachePageUtil.addAppText(request, "GLOBAL_SERVICE","PASSWORD_CHANGE_SERVICE");
		
		appCachePage.getPageInfo(request,response);
		
		// validate
		utilSvc.validateParams(request, response);
		if ((Boolean) request.getParam(BaseEntity.VALID) == false) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "GLOBAL_SERVICE", "GLOBAL_SERVICE_VALIDATION_ERROR").getValue(), response);
			return;
		}
		
		Map<String,Object> inputList = (Map<String, Object>) request.getParam("inputFields");
		String username = (String) inputList.get("PASSWORD_CHANGE_FORM_USERNAME");
		String oldPassword = (String) inputList.get("PASSWORD_CHANGE_FORM_OLD_PASSWORD");
		String password = (String) inputList.get("PASSWORD_CHANGE_FORM_PASSWORD");
		String verifyPassword =(String) inputList.get("PASSWORD_CHANGE_FORM_VERIFYPASSWORD");
		if (username != null && !username.isEmpty() && oldPassword != null && !oldPassword.isEmpty() && password != null && !password.isEmpty() 
				&& verifyPassword != null && !verifyPassword.isEmpty() && password.equals(verifyPassword)) {
			User user = findUser(username);
			if (user != null) {
				try {
					// check old password
					String eOldPassword = Base64.encodeBytes(utilSvc.getEncryptedPassword(oldPassword, Base64.decode(user.getSalt()))); 
					if (eOldPassword.equals(user.getPassword())){
						// create salt
						byte[] salt = utilSvc.generateSalt();
						String ePassword = Base64.encodeBytes(utilSvc.getEncryptedPassword(password,salt));
						// change session token
						byte[] sessionToken = utilSvc.generateSalt();
						String sessionTokenString = Base64.encodeBytes(sessionToken);
						
						userManagerDao.changePassword(username, ePassword, Base64.encodeBytes(salt), sessionTokenString);
			
						// return token 
				    	response.addParam("token", sessionTokenString);
						mailSvc.sendEmailNotification(username, user.getEmail(), AppCachePageUtil.getAppText(request, "PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_EMAIL_VERIFY").getValue());
						utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, AppCachePageUtil.getAppText(request, "PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_SUCCESSFUL").getValue(), response);
					} else {
						utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_OLDPASS_INCORRECT").getValue(), response);
					}
				} catch (Exception e) {
					// if failed need to rollback app and single 
					utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_PASSCHANGE_FAIL").getValue(), response);
					e.printStackTrace();
				}
			} else {
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_USERMISSING").getValue(), response);
			}
		} else {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request, "PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_USER_PASS_EMPTY").getValue(), response);
		}
	}
	
	
	public boolean authenticate(User user, RestRequest request){
		boolean authenticated = false;
		String status = "Autentication Failed";
		try {
			if (!user.isEmailConfirm()){
					// authenticated
					authenticated = false;
					status = "Check your email and confirm registration";
			} else {
				if (request.getParam("action").equals("LOGINAUTHENTICATE")){
					String password = (String) request.getParam("password");
					byte[] salt = Base64.decode(user.getSalt());
					byte[] epass = utilSvc.getEncryptedPassword(password, salt);
					byte[] pass = Base64.decode(user.getPassword());
					if (Arrays.equals(pass, epass)){
						authenticated = true;
						status = "Autentication Success";
					}
				} else if (request.getParam("action").equals("TOKENAUTHENTICATE")) {
					String token = (String) request.getParam("token");
					byte[] t = Base64.decode(token);
					byte[] sessionToken = Base64.decode(user.getSessionToken());
					if (Arrays.equals(sessionToken, t)){
						authenticated = true;
						status = "Autentication Success";
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//newUserEvent.fire(new NewUserEvent(user.getFirstname(),status));
		return authenticated;
	}
	
	public boolean authenticate(User user, String token){
		boolean authenticated = false;
		String status = "Autentication Failed";
		
		if (user.isEmailConfirm() && user.getSessionToken().equals(token)){
					// update user
					
			// authenticated
			authenticated = true;
			status = "Autentication Success";
		} 
		
		//newUserEvent.fire(new NewUserEvent(user.getFirstname(),status));
		return authenticated;
	}
	
	public User findUser(String username){
		User user = null;
		try {
			user = userManagerDao.findUser(username);
		} catch (Exception e) {
			System.out.println("Exception in findUser");
		}
		return user;
	}
	
	public User findUserByEmail(String email){
		User user = null;
		try {
			user = userManagerDao.findUserByEmail(email);
		} catch (Exception e) {
			System.out.println("Exception in findUser");
		}
		return user;
	}
	
	public RestResponse checkUserName(RestRequest request, RestResponse response){
		String username = (String) request.getParams().get("username");
		User user = null;
		
		try {
			user = userManagerDao.findUser(username);
		} catch (NoResultException noe){
		} catch (Exception e){
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, "Database Query Error", response);
			e.printStackTrace();
		}
		
	
		if (user != null){
			if (!user.isActive()) {
				utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, "NOTACTIVE", response);
			} else if (user.isLocked()){
				utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, "LOCKED", response);
			} else {
				utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, "BOTHGOOD", response);
			}
		} else if (user == null){
			utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, "FULL", response);
		}
		return response;
	}
	
	

	public void logAccess(LoginLog loginLog) {
		try {
			userManagerDao.logAccess(loginLog);
		} catch (Exception e){
			
		}
	}

	//public void auditNewUser(@Observes NewUserEvent event ){
//		System.out.println("User " + event.getUserName() + " Status: " + event.getStatus());
	//}

	public void confirmEmail(RestRequest request, RestResponse response){
		AppCachePageUtil.addAppForm(request, "CONFIRM_EMAIL_SERVICE");
		AppCachePageUtil.addAppText(request, "GLOBAL_SERVICE","CONFIRM_EMAIL_SERVICE");
		
		appCachePage.getPageInfo(request,response);
		
		// validate
		utilSvc.validateParams(request, response);
		if ((Boolean) request.getParam(BaseEntity.VALID) == false) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request,"GLOBAL_SERVICE","GLOBAL_SERVICE_VALIDATION_ERROR").getValue(), response);
			return;
		}
		Map<String,Object> inputList = (Map<String, Object>) request.getParam("inputFields");
		
		//String username = (String) request.getParams().get("username");
		//String token = (String) request.getParams().get("token");
		String username = (String) inputList.get("CONFIRM_EMAIL_SERVICE_USERNAME");
		String token = (String) inputList.get("CONFIRM_EMAIL_SERVICE_TOKEN");
		
		User user = null;
		try {
			user = userManagerDao.findUser(username);
		} catch (NoResultException noe){
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request,"CONFIRM_EMAIL_SERVICE","CONFIRM_EMAIL_SERVICE_USERMISSING").getValue(), response);
		} catch (Exception e){
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request,"CONFIRM_EMAIL_SERVICE","CONFIRM_EMAIL_SERVICE_DBERROR").getValue(), response);
			e.printStackTrace();
		}
		if (user.getEmailToken().equals(token)){
			try {
				userManagerDao.updateEmailConfirm(user);
				utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, AppCachePageUtil.getAppText(request,"CONFIRM_EMAIL_SERVICE","CONFIRM_EMAIL_SERVICE_SUCCESSFUL").getValue(), response);
			} catch (Exception e) {
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request,"CONFIRM_EMAIL_SERVICE","CONFIRM_EMAIL_SERVICE_FAIL").getValue(), response);
				e.printStackTrace();
			}
		} else {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, AppCachePageUtil.getAppText(request,"CONFIRM_EMAIL_SERVICE","CONFIRM_EMAIL_SERVICE_FAIL").getValue(), response);
		}
		
	}
	
	public void logout(RestRequest request, RestResponse response) {
		// invalidate user context and terminate session
		//utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, userContext.getLogout(), response);
		// log user activity
		
	}
}

