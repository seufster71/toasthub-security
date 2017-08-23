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

package org.toasthub.security.users;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.core.common.EntityManagerSecuritySvc;
import org.toasthub.core.common.UtilSvc;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.security.model.Role;
import org.toasthub.security.model.User;

@Repository("UsersDao")
@Transactional("TransactionManagerSecurity")
public class UsersDaoImpl implements UsersDao {
	
	@Autowired
	protected EntityManagerSecuritySvc entityManagerSecuritySvc;
	@Autowired
	protected UtilSvc utilSvc;
	
	public void saveUser(RestRequest request, RestResponse response) throws Exception {
		User user = (User) request.getParam("sysUser");
		EntityManager emain = entityManagerSecuritySvc.getInstance();
		Set<Role> roles = new HashSet<Role>();
		roles.add((Role) emain.createQuery("FROM Role r WHERE r.code = :code").setParameter("code","ROLE_MEMBER").getSingleResult());
		user.setRoles(roles);
		emain.merge(user);
	}
	
	public void resetPassword(String username, String password, String salt, String sessionToken) throws Exception {
		EntityManager emain = entityManagerSecuritySvc.getInstance();
		int results = emain.createQuery("update User set password = :password, salt = :salt, sessionToken = :sessionToken, forceChange = :forceChange where username = :username")
				.setParameter("password",password).setParameter("salt", salt)
				.setParameter("sessionToken",sessionToken)
				.setParameter("forceChange", true)
				.setParameter("username",username)
				.executeUpdate();
		if (results == 0) {
			// throw error
			throw new Exception("Password reset Failed!");
		}
	}
	
	public void changePassword(String username, String password, String salt, String sessionToken) throws Exception {
		EntityManager emain = entityManagerSecuritySvc.getInstance();
		int results = emain.createQuery("update User set password = :password, salt = :salt, sessionToken = :sessionToken, forceChange = :forceChange where username = :username")
				.setParameter("password",password)
				.setParameter("salt",salt)
				.setParameter("sessionToken",sessionToken)
				.setParameter("forceChange", false)
				.setParameter("username",username)
				.executeUpdate();
		if (results == 0) {
			// throw error
			throw new Exception("Password change Failed!");
		}
	}
	
	public User findUser(String username) throws Exception {
		User user = null;
		try {
		user = (User) entityManagerSecuritySvc.getInstance()
			.createQuery("FROM User as u WHERE u.username = :username AND u.archive = :archive")
			.setParameter("username", username)
			.setParameter("archive",false)
			.getSingleResult();
		} catch (NoResultException noresut){
			
		}
		return user;
	}
	
	public User findUserByEmail(String email) throws Exception {
		User user = null;
		try {
		user = (User) entityManagerSecuritySvc.getInstance()
			.createQuery("FROM User as u WHERE u.email = :email AND u.archive = :archive")
			.setParameter("email", email)
			.setParameter("archive",false)
			.getSingleResult();
		} catch (NoResultException noresut){
			
		}
		return user;
	}
	
	//@Authorize
	public void updateUser(RestRequest request, RestResponse response) throws Exception {
		User User = (User) request.getParam(GlobalConstant.ITEM);
		entityManagerSecuritySvc.getInstance().merge(User);
	}
	
	@Override
	public void items(RestRequest request, RestResponse response) throws Exception {
		
		String queryStr = "SELECT DISTINCT u FROM User AS u ";
		
		boolean and = false;
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			if (!and) { queryStr += " WHERE "; }
			queryStr += "u.active =:active ";
			and = true;
		}
		
		if (request.containsParam(GlobalConstant.SEARCHVALUE) && !request.getParam(GlobalConstant.SEARCHVALUE).equals("")){
			if (!and) { queryStr += " WHERE "; } else { queryStr += " AND "; }
			queryStr += " u.firstname LIKE :searchValue OR u.lastname LIKE :searchValue "; 
			and = true;
		}
		
		Query query = entityManagerSecuritySvc.getInstance().createQuery(queryStr);
		
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			query.setParameter("active", (Boolean) request.getParam(GlobalConstant.ACTIVE));
		} 
		
		if (request.containsParam(GlobalConstant.SEARCHVALUE) && !request.getParam(GlobalConstant.SEARCHVALUE).equals("")){
			query.setParameter("searchValue", "%"+((String)request.getParam(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
		}
		if (request.containsParam(GlobalConstant.PAGELIMIT) && (Integer) request.getParam(GlobalConstant.PAGELIMIT) != 0){
			query.setFirstResult((Integer) request.getParam(GlobalConstant.PAGESTART));
			query.setMaxResults((Integer) request.getParam(GlobalConstant.PAGELIMIT));
		}
		@SuppressWarnings("unchecked")
		List<User> users = query.getResultList();

		response.addParam(GlobalConstant.ITEMS, users);
	}

	@Override
	public void itemCount(RestRequest request, RestResponse response) throws Exception {
		String queryStr = "SELECT COUNT(*) FROM User AS u ";
		boolean and = false;
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			if (!and) { queryStr += " WHERE "; }
			queryStr += "u.active =:active ";
			and = true;
		}
		
		if (request.containsParam(GlobalConstant.SEARCHVALUE) && !request.getParam(GlobalConstant.SEARCHVALUE).equals("")){
			if (!and) { queryStr += " WHERE "; } else { queryStr += " AND "; }
			queryStr += " u.firstname LIKE :searchValue OR u.lastname LIKE :searchValue "; 
			and = true;
		}

		Query query = entityManagerSecuritySvc.getInstance().createQuery(queryStr);
		
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			query.setParameter("active", (Boolean) request.getParam(GlobalConstant.ACTIVE));
		} 
		
		if (request.containsParam(GlobalConstant.SEARCHVALUE) && !request.getParam(GlobalConstant.SEARCHVALUE).equals("")){
			query.setParameter("searchValue", "%"+((String)request.getParam(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
		}
		
		Long count = (Long) query.getSingleResult();
		if (count == null){
			count = 0l;
		}
		response.addParam(GlobalConstant.ITEMCOUNT, count);
		
	}

	@Override
	public void item(RestRequest request, RestResponse response) throws Exception {
		if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {
			String queryStr = "SELECT u FROM User AS u WHERE u.id =:id";
			Query query = entityManagerSecuritySvc.getInstance().createQuery(queryStr);
		
			query.setParameter("id", new Long((Integer) request.getParam(GlobalConstant.ITEMID)));
			User user = (User) query.getSingleResult();
			
			response.addParam(GlobalConstant.ITEM, user);
		} else {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.ACTIONFAILED, "Missing ID", response);
		}
	}
}
