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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.BaseEntity;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.core.general.service.UtilSvc;
import org.toasthub.core.preference.model.AppCachePageUtil;
import org.toasthub.security.model.User;
import org.toasthub.security.repository.UsersDao;

@Service("UserSvc")
public class UsersSvcImpl implements ServiceProcessor, UsersSvc {

	@Autowired 
	@Qualifier("UsersDao")
	UsersDao usersDao;
	
	@Autowired 
	UtilSvc utilSvc;
	
	@Autowired 
	AppCachePageUtil appCachePageUtil;

	public void process(RestRequest request, RestResponse response) {
		String action = (String) request.getParams().get(BaseEntity.ACTION);
		
		Long count = 0l;
		switch (action) {
		case "INIT":
			appCachePageUtil.getPageInfo(request,response);
			// set paging
			this.initParams(request);
			
			this.itemCount(request, response);
			count = (Long) response.getParam(BaseEntity.ITEMCOUNT);
			this.itemColumns(request, response);
			if (count != null && count > 0){
				this.items(request, response);
			}
			response.addParam(BaseEntity.ITEMNAME, request.getParam(BaseEntity.ITEMNAME));
			break;
		case "LIST":
			this.initParams(request);
			
			this.itemCount(request, response);
			count = (Long) response.getParam(BaseEntity.ITEMCOUNT);
			this.itemColumns(request, response);
			if (count != null && count > 0){
				this.items(request, response);
			}
			response.addParam(BaseEntity.ITEMNAME, request.getParam(BaseEntity.ITEMNAME));
			break;
		case "SHOW":
			this.item(request, response);
			break;
		case "EDIT":
			// get form info
			appCachePageUtil.getPageInfo(request, response);
			// get item info
			this.item(request, response);
			break;
		case "SAVE":
			this.saveUser(request, response);
			break;
		default:
			utilSvc.addStatus(RestResponse.INFO, RestResponse.ACTIONNOTEXIST, "Action not available", response);
			break;
		}
		
		
	}
	
	protected void initParams(RestRequest request) {
		if (!request.containsParam(BaseEntity.SEARCHCOLUMN)){
			request.addParam(BaseEntity.SEARCHCOLUMN, "lastname");
		}
		if (!request.containsParam(BaseEntity.ITEMNAME)){
			request.addParam(BaseEntity.ITEMNAME, "User");
		}
		if (!request.containsParam(BaseEntity.ORDERCOLUMN)) {
			request.addParam(BaseEntity.ORDERCOLUMN, "firstname,lastname");
		}
		if (!request.containsParam(BaseEntity.ORDERDIR)) {
			request.addParam(BaseEntity.ORDERDIR, "ASC");
		}
	}
	
	public void item(RestRequest request, RestResponse response) {
		try {
			usersDao.item(request, response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void saveUser(RestRequest request, RestResponse response) {
		try {
			usersDao.saveUser(request, response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void items(RestRequest request, RestResponse response) {
		try {
			usersDao.items(request, response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void itemColumns(RestRequest request, RestResponse response){
		String itemName = (String) request.getParam(BaseEntity.ITEMNAME);
		if (itemName != null && itemName.equals("User")) {
			request.addParam("columns",User.columns);
		}
		response.addParam("columns", request.getParam("columns"));
	}
	
	public void itemCount(RestRequest request, RestResponse response) {
		try {
			usersDao.itemCount(request, response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
