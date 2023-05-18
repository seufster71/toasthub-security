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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.core.common.EntityManagerSecuritySvc;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.core.general.utils.TenantContext;
import org.toasthub.core.system.model.AppCacheClientDomains;
import org.toasthub.core.system.model.ClientDomain;
import org.toasthub.security.common.SecurityUtils;
import org.toasthub.security.model.LoginLog;
import org.toasthub.security.model.Role;
import org.toasthub.security.model.User;
import org.toasthub.security.model.UserRole;

@Repository("UserManagerDao")
@Transactional("TransactionManagerSecurity")
public class UserManagerDaoImpl implements UserManagerDao {
	
	@Autowired 
	protected EntityManagerSecuritySvc entityManagerSecuritySvc;
	@Autowired	
	protected AppCacheClientDomains appCacheClientDomains;
	
	@Override
	public User findUser(String username) throws Exception {
		User user = null;
		try {
			// user 
		user = (User) entityManagerSecuritySvc.getInstance()
			.createQuery("FROM User as u WHERE u.username = :username AND u.archive = :archive")
			.setParameter("username", username)
			.setParameter("archive",false)
			.getSingleResult();
			// get user roles and permissions
		List<UserRole> userRoles = entityManagerSecuritySvc.getInstance().createQuery("FROM UserRole as ur WHERE ur.user.id =:userId").setParameter("userId", user.getId()).getResultList();
		user.setPermissions(SecurityUtils.effectivePermissions(userRoles));
		} catch (NoResultException noresut){
			
		}
		return user;
	}
	
	@Override
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
	
	@Override
	public void saveUser(User user) throws Exception {
		EntityManager emain = entityManagerSecuritySvc.getInstance();
		emain.merge(user);
		Set<UserRole> userRoles = new HashSet<UserRole>();
		UserRole userRole = new UserRole();
		userRole.setUser(user);
		userRole.setRole((Role) emain.createQuery("FROM Role r WHERE r.code = :code").setParameter("code","M").getSingleResult());
		userRoles.add(userRole);
		emain.merge(userRoles);
	}

	@Override
	public void resetPassword(String username, String password, String salt, String sessionToken) throws Exception {
		EntityManager emain = entityManagerSecuritySvc.getInstance();
		int results = emain.createQuery("update User set password = :password, salt = :salt, sessionToken = :sessionToken, forceReset = :forceReset where username = :username")
				.setParameter("password",password).setParameter("salt", salt).setParameter("sessionToken",sessionToken).setParameter("forceReset", true).setParameter("username",username).executeUpdate();
		if (results == 0) {
			// throw error
			throw new Exception("Password reset Failed!");
		}
	}
	
	@Override
	public void changePassword(String username, String password, String salt, String sessionToken) throws Exception {
		EntityManager emain = entityManagerSecuritySvc.getInstance();
		int results = emain.createQuery("update User set password = :password, salt = :salt, sessionToken = :sessionToken, forceReset = :forceReset where username = :username")
				.setParameter("password",password).setParameter("salt",salt).setParameter("sessionToken",sessionToken).setParameter("forceReset", false).setParameter("username",username).executeUpdate();
		if (results == 0) {
			// throw error
			throw new Exception("Password change Failed!");
		}
	}
	
	@Override
	public void updateEmailConfirm(User user) throws Exception {
		EntityManager emain = entityManagerSecuritySvc.getInstance();
		emain.createQuery("update User set emailConfirm = :emailConfirm where id = :id").setParameter("emailConfirm",true).setParameter("id",user.getId()).executeUpdate();
	}
	
	@Override
	public void logAccess(LoginLog loginLog) throws Exception {
		EntityManager emain = entityManagerSecuritySvc.getInstance();
		String host = TenantContext.getURLDomain();
		ClientDomain cdomain = appCacheClientDomains.getClientDomains().get(host);
		loginLog.setAppname(cdomain.getAPPName());
		emain.persist(loginLog);
	}

	@Override
	public void items(RestRequest request, RestResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void itemCount(RestRequest request, RestResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void item(RestRequest request, RestResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void selectList(RestRequest request, RestResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
}
