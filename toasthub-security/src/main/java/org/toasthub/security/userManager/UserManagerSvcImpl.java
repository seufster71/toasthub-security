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

package org.toasthub.security.userManager;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.Map;

import jakarta.persistence.NoResultException;

import org.picketbox.commons.cipher.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.toasthub.core.common.EntityManagerMainSvc;
import org.toasthub.core.common.UtilSvc;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.core.mail.MailSvc;
import org.toasthub.core.preference.model.PrefCacheUtil;
import org.toasthub.core.preference.model.PrefOptionValue;
import org.toasthub.security.model.LoginLog;
import org.toasthub.security.model.MyUserPrincipal;
import org.toasthub.security.model.User;

@Service("UserManagerSvc")
public class UserManagerSvcImpl implements ServiceProcessor, UserManagerSvc {
	
	@Autowired 
	@Qualifier("UserManagerDao")
	UserManagerDao userManagerDao;
	
	@Autowired 
	MailSvc mailSvc;
	
	@Autowired 
	UtilSvc utilSvc;
	
	@Autowired 
	PrefCacheUtil prefCacheUtil;
	
	@Autowired 
	EntityManagerMainSvc entityManagerMainSvc;
	
	// Constructor
	public UserManagerSvcImpl() {}
	
	// Processor
	public void process(RestRequest request, RestResponse response) {
		String action = (String) request.getParams().get(GlobalConstant.ACTION);
		switch (action) {
		case "INIT": 
			request.addParam(PrefCacheUtil.PREFPARAMLOC, PrefCacheUtil.RESPONSE);
			prefCacheUtil.getPrefInfo(request,response);
			
			if (request.containsParam("urlaction") && "emailconfirm".equals(request.getParam("urlaction"))){
				RestRequest subRequest = new RestRequest();
				subRequest.addParam(GlobalConstant.INPUTFIELDS, request.getParam(GlobalConstant.INPUTFIELDS));
				subRequest.addParam(GlobalConstant.LANG, request.getParam(GlobalConstant.LANG));
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
			response.setStatus(RestResponse.SUCCESS);
			response.addParam("USER", ((MyUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser());
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
		default:
			break;
		}
		
	}
	
	public void init(RestRequest request, RestResponse response) {
		// get custom page Layout
		response.addParam(GlobalConstant.PAGELAYOUT,entityManagerMainSvc.getPublicLayout());
	}
	
	public void registerCheckNameInit(RestRequest request, RestResponse response) {
		// get option from main
	
	}
	
	public void registerFull(RestRequest request, RestResponse response) {
		try {
			if (!request.containsParam(PrefCacheUtil.PREFFORMKEYS)) {
				prefCacheUtil.addPrefForm(request, "REGISTRATION_FORM");
			}
			prefCacheUtil.addPrefText(request, "GLOBAL_SERVICE","REGISTRATION_SERVICE");
			prefCacheUtil.addPrefOption(request, "REGISTRATION_SERVICE");
			
			prefCacheUtil.getPrefInfo(request,response);
			
			// service status
			PrefOptionValue serviceStatus = prefCacheUtil.getPrefOption(request, "REGISTRATION_SERVICE", "REGISTRATION_SERVICE");
			if (serviceStatus.equals("false")){
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("GLOBAL_SERVICE","GLOBAL_SERVICE_DISABLED",prefCacheUtil.getLang(request)), response);
				return;
			}
			// validate
			utilSvc.validateParams(request, response);
			if ((Boolean) request.getParam(GlobalConstant.VALID) == false) {
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("GLOBAL_SERVICE", "GLOBAL_SERVICE_VALIDATION_ERROR",prefCacheUtil.getLang(request)), response);
				return;
			}
			// create empty user to fill
			request.addParam(GlobalConstant.ITEM, new User());
			// marshall
			utilSvc.marshallFields(request, response);
			
			User user = (User) request.getParam(GlobalConstant.ITEM);
			// set the default language of user
			user.setLang("en");
			// did password match
			if(!user.getPassword().equals(user.getVerifyPassword())) {
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("REGISTRATION_SERVICE", "REGISTRATION_SERVICE_MATCH_PASSWORD_FAILURE",prefCacheUtil.getLang(request)), response);
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
				utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, prefCacheUtil.getPrefText("REGISTRATION_SERVICE", "REGISTRATION_SUCCESSFUL",prefCacheUtil.getLang(request)), response);
				
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
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("REGISTRATION_SERVICE", "REGISTRATION_FAIL",prefCacheUtil.getLang(request)).concat(message), response);
			}
			response.addParam("prefFormFields",null);
		} catch (Exception e) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("REGISTRATION_SERVICE", "REGISTRATION_FAIL",prefCacheUtil.getLang(request)), response);
			e.printStackTrace();
		}
	}
	
	public void registerCheckUserName(RestRequest request, RestResponse response) {
		checkUserName(request, response);
	}
	
	public void authenticate(RestRequest request, RestResponse response) {
		if (!request.containsParam(PrefCacheUtil.PREFFORMKEYS)) {
			prefCacheUtil.addPrefForm(request, "LOGIN_FORM");
		}
		prefCacheUtil.addPrefText(request, "GLOBAL_SERVICE","LOGIN_SERVICE");
		
		prefCacheUtil.getPrefInfo(request,response);
		
		// validate
		utilSvc.validateParams(request, response);
		if ((Boolean) request.getParam(GlobalConstant.VALID) == false) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("GLOBAL_SERVICE", "GLOBAL_SERVICE_VALIDATION_ERROR",prefCacheUtil.getLang(request)), response);
			return;
		}
		
		Map<String,Object> inputList = (Map<String, Object>) request.getParam(GlobalConstant.INPUTFIELDS);
		if (!inputList.containsKey("LOGIN_FORM_USERNAME")) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("LOGIN_SERVICE", "LOGIN_SERVICE_BADUSERNAME",prefCacheUtil.getLang(request)), response);
			logAccess(new LoginLog("EMPTY_NAME",(String) request.getParam("TENANT_URLDOMAIN"),LoginLog.FAIL_BAD_USER));
			return;
		}
		User user = findUser((String) inputList.get("LOGIN_FORM_USERNAME"));
		if (user == null){
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("LOGIN_SERVICE", "LOGIN_SERVICE_BADUSERNAME",prefCacheUtil.getLang(request)), response);
			logAccess(new LoginLog((String) inputList.get("LOGIN_FORM_USERNAME"),(String) request.getParam("TENANT_URLDOMAIN"),LoginLog.FAIL_BAD_USER));
		} else {
			request.addParam("password", inputList.get("LOGIN_FORM_PASSWORD"));
			boolean authenticated = authenticate(user, request);
			if (authenticated ){
				List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
			/*	if (SecurityUtils.containsPermission(user, "ADMAREA", "R") ) {
					authorities.add(new SimpleGrantedAuthority("ADMIN"));
				}
				if (SecurityUtils.containsPermission(user, "MEMAREA", "R")) {
					authorities.add(new SimpleGrantedAuthority("MEMBER"));
				}
				if (SecurityUtils.containsPermission(user, "SYSAREA", "R")) {
					authorities.add(new SimpleGrantedAuthority("SYSTEM"));
				}*/
				response.addParam("authorities", authorities);
				response.addParam("user", user);
				
				
				//userContext.loginWS(user);
				//LoginLog loginLog = new LoginLog(user,true);
		    	//logAccess(loginLog);
		    	// return token 
		    	if (request.getParam("action").equals("LOGINAUTHENTICATE")){
		    		response.addParam("token", user.getSessionToken());
		    	}
		    	logAccess(new LoginLog(user.getUsername(),(String) request.getParam("TENANT_URLDOMAIN"),LoginLog.SUCCESS));
				utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, "Authenticated", response);
			} else if (!user.isEmailConfirm()){
				logAccess(new LoginLog(user.getUsername(),(String) request.getParam("TENANT_URLDOMAIN"),LoginLog.FAIL_BAD_EMAIL_CONFIRM));
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("LOGIN_SERVICE", "LOGIN_SERVICE_CHECK_EMAIL_CONFIRM",prefCacheUtil.getLang(request)), response);
			} else {
				logAccess(new LoginLog(user.getUsername(),(String) request.getParam("TENANT_URLDOMAIN"),LoginLog.FAIL_BAD_PASS));
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("LOGIN_SERVICE", "LOGIN_SERVICE_BADPASSWORD",prefCacheUtil.getLang(request)), response);
			}
		}
	}
	
	public void forgotPassword(RestRequest request, RestResponse response) {
		try {
			if (!request.containsParam(PrefCacheUtil.PREFFORMKEYS)) {
				prefCacheUtil.addPrefForm(request, "FORGOTPASSWORD_FORM");
			}
			prefCacheUtil.addPrefText(request, "GLOBAL_SERVICE","FORGOTPASSWORD_SERVICE");
			
			prefCacheUtil.getPrefInfo(request,response);
			
			// validate
			utilSvc.validateParams(request, response);
			if ((Boolean) request.getParam(GlobalConstant.VALID) == false) {
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("GLOBAL_SERVICE", "GLOBAL_SERVICE_VALIDATION_ERROR",prefCacheUtil.getLang(request)), response);
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
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("FORGOTPASSWORD_SERVICE", "FORGOTPASSWORD_SERVICE_USERMISSING",prefCacheUtil.getLang(request)), response);
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
					utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, prefCacheUtil.getPrefText("FORGOTPASSWORD_SERVICE", "FORGOTPASSWORD_SERVICE_SUCCESSFUL",prefCacheUtil.getLang(request)), response);
				} catch (Exception e){
					utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("FORGOTPASSWORD_SERVICE", "FORGOTPASSWORD_SERVICE_PASSWORD_FAIL",prefCacheUtil.getLang(request)), response);
					e.printStackTrace();
				}
				
			}
		} catch (Exception e) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("FORGOTPASSWORD_SERVICE", "FORGOTPASSWORD_SERVICE_FAIL",prefCacheUtil.getLang(request)), response);
			e.printStackTrace();
		}
		
	}
	
	public void changePassword(RestRequest request, RestResponse response) {
		if (!request.containsParam(PrefCacheUtil.PREFFORMKEYS)) {
			prefCacheUtil.addPrefForm(request, "PASSWORD_CHANGE_FORM");
		}
		prefCacheUtil.addPrefText(request, "GLOBAL_SERVICE","PASSWORD_CHANGE_SERVICE");
		
		prefCacheUtil.getPrefInfo(request,response);
		
		// validate
		utilSvc.validateParams(request, response);
		if ((Boolean) request.getParam(GlobalConstant.VALID) == false) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("GLOBAL_SERVICE", "GLOBAL_SERVICE_VALIDATION_ERROR",prefCacheUtil.getLang(request)), response);
			return;
		}
		
		Map<String,Object> inputList = (Map<String, Object>) request.getParam(GlobalConstant.INPUTFIELDS);
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
						mailSvc.sendEmailNotification(username, user.getEmail(), prefCacheUtil.getPrefText("PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_EMAIL_VERIFY",prefCacheUtil.getLang(request)));
						utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, prefCacheUtil.getPrefText("PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_SUCCESSFUL",prefCacheUtil.getLang(request)), response);
					} else {
						utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_OLDPASS_INCORRECT",prefCacheUtil.getLang(request)), response);
					}
				} catch (Exception e) {
					// if failed need to rollback app and single 
					utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_PASSCHANGE_FAIL",prefCacheUtil.getLang(request)), response);
					e.printStackTrace();
				}
			} else {
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_USERMISSING",prefCacheUtil.getLang(request)), response);
			}
		} else {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("PASSWORD_CHANGE_SERVICE", "PASSWORD_CHANGE_SERVICE_USER_PASS_EMPTY",prefCacheUtil.getLang(request)), response);
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
	
	
	@Override
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
		prefCacheUtil.addPrefForm(request, "CONFIRM_EMAIL_SERVICE");
		prefCacheUtil.addPrefText(request, "GLOBAL_SERVICE","CONFIRM_EMAIL_SERVICE");
		
		prefCacheUtil.getPrefInfo(request,response);
		
		// validate
		utilSvc.validateParams(request, response);
		if ((Boolean) request.getParam(GlobalConstant.VALID) == false) {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("GLOBAL_SERVICE","GLOBAL_SERVICE_VALIDATION_ERROR",prefCacheUtil.getLang(request)), response);
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
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("CONFIRM_EMAIL_SERVICE","CONFIRM_EMAIL_SERVICE_USERMISSING",prefCacheUtil.getLang(request)), response);
		} catch (Exception e){
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("CONFIRM_EMAIL_SERVICE","CONFIRM_EMAIL_SERVICE_DBERROR",prefCacheUtil.getLang(request)), response);
			e.printStackTrace();
		}
		if (user.getEmailToken().equals(token)){
			try {
				userManagerDao.updateEmailConfirm(user);
				utilSvc.addStatus(RestResponse.INFO, RestResponse.SUCCESS, prefCacheUtil.getPrefText("CONFIRM_EMAIL_SERVICE","CONFIRM_EMAIL_SERVICE_SUCCESSFUL",prefCacheUtil.getLang(request)), response);
			} catch (Exception e) {
				utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("CONFIRM_EMAIL_SERVICE","CONFIRM_EMAIL_SERVICE_FAIL",prefCacheUtil.getLang(request)), response);
				e.printStackTrace();
			}
		} else {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED, prefCacheUtil.getPrefText("CONFIRM_EMAIL_SERVICE","CONFIRM_EMAIL_SERVICE_FAIL",prefCacheUtil.getLang(request)), response);
		}
		
	}
	
	
}

