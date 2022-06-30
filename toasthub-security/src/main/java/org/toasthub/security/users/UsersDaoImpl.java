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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.toasthub.security.model.UserContext;
import org.toasthub.security.model.UserRole;

@Repository("UsersDao")
@Transactional("TransactionManagerSecurity")
public class UsersDaoImpl implements UsersDao {
	
	@Autowired
	protected EntityManagerSecuritySvc entityManagerSecuritySvc;
	
	@Autowired
	protected UtilSvc utilSvc;
	
	@Autowired 
	UserContext userContext;
	
	public void saveUser(RestRequest request, RestResponse response) throws Exception {
		User user = (User) request.getParam("sysUser");
		EntityManager emain = entityManagerSecuritySvc.getInstance();
		emain.merge(user);
		Set<UserRole> userRoles = new HashSet<UserRole>();
		UserRole userRole = new UserRole();
		userRole.setUser(user);
		userRole.setRole((Role) emain.createQuery("FROM Role r WHERE r.code = :code").setParameter("code","M").getSingleResult());
		userRoles.add(userRole);
		emain.merge(userRoles);
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
	
	public User findUserById(Long id) throws Exception {
		User user = null;
		try {
			user = (User) entityManagerSecuritySvc.getInstance()
			.createQuery("FROM User as u WHERE u.id = :id AND u.archive = :archive")
			.setParameter("id", id)
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
		
		// search
		ArrayList<LinkedHashMap<String,String>> searchCriteria = null;
		if (request.containsParam(GlobalConstant.SEARCHCRITERIA) && !request.getParam(GlobalConstant.SEARCHCRITERIA).equals("")) {
			if (request.getParam(GlobalConstant.SEARCHCRITERIA) instanceof Map) {
				searchCriteria = new ArrayList<>();
				searchCriteria.add((LinkedHashMap<String, String>) request.getParam(GlobalConstant.SEARCHCRITERIA));
			} else {
				searchCriteria = (ArrayList<LinkedHashMap<String, String>>) request.getParam(GlobalConstant.SEARCHCRITERIA);
			}
			
			// Loop through all the criteria
			boolean or = false;
			
			String lookupStr = "";
			for (LinkedHashMap<String,String> item : searchCriteria) {
				if (item.containsKey(GlobalConstant.SEARCHVALUE) && !"".equals(item.get(GlobalConstant.SEARCHVALUE)) && item.containsKey(GlobalConstant.SEARCHCOLUMN)) {
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_FIRSTNAME")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "u.firstname LIKE :firstnameValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_LASTNAME")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "u.lastname LIKE :lastnameValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_USERNAME")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "u.username LIKE :usernameValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_EMAIL")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "u.email LIKE :emailValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_STATUS")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "u.active LIKE :statusValue"; 
						or = true;
					}
				}
			}
			if (!"".equals(lookupStr)) {
				if (!and) { 
					queryStr += " WHERE ( " + lookupStr + " ) ";
				} else {
					queryStr += " AND ( " + lookupStr + " ) ";
				}
			}
			
		}
		// order by
		ArrayList<LinkedHashMap<String,String>> orderCriteria = null;
		StringBuilder orderItems = new StringBuilder();
		if (request.containsParam(GlobalConstant.ORDERCRITERIA) && !request.getParam(GlobalConstant.ORDERCRITERIA).equals("")) {
			if (request.getParam(GlobalConstant.ORDERCRITERIA) instanceof Map) {
				orderCriteria = new ArrayList<>();
				orderCriteria.add((LinkedHashMap<String, String>) request.getParam(GlobalConstant.ORDERCRITERIA));
			} else {
				orderCriteria = (ArrayList<LinkedHashMap<String, String>>) request.getParam(GlobalConstant.ORDERCRITERIA);
			}
			
			// Loop through all the criteria
			boolean comma = false;
			
			
			for (LinkedHashMap<String,String> item : orderCriteria) {
				if (item.containsKey(GlobalConstant.ORDERCOLUMN) && item.containsKey(GlobalConstant.ORDERDIR)) {
					if (item.get(GlobalConstant.ORDERCOLUMN).equals("ADMIN_USER_TABLE_FIRSTNAME")){
						if (comma) { orderItems.append(","); }
						orderItems.append("u.firstname ").append(item.get(GlobalConstant.ORDERDIR));
						comma = true;
					}
					if (item.get(GlobalConstant.ORDERCOLUMN).equals("ADMIN_USER_TABLE_LASTNAME")){
						if (comma) { orderItems.append(","); }
						orderItems.append("u.lastname ").append(item.get(GlobalConstant.ORDERDIR));
						comma = true;
					}
					if (item.get(GlobalConstant.ORDERCOLUMN).equals("ADMIN_USER_TABLE_USERNAME")){
						if (comma) { orderItems.append(","); }
						orderItems.append("u.username ").append(item.get(GlobalConstant.ORDERDIR));
						comma = true;
					}
					if (item.get(GlobalConstant.ORDERCOLUMN).equals("ADMIN_USER_TABLE_EMAIL")){
						if (comma) { orderItems.append(","); }
						orderItems.append("u.email ").append(item.get(GlobalConstant.ORDERDIR));
						comma = true;
					}
					if (item.get(GlobalConstant.ORDERCOLUMN).equals("ADMIN_USER_TABLE_STATUS")){
						if (comma) { orderItems.append(","); }
						orderItems.append("u.active ").append(item.get(GlobalConstant.ORDERDIR));
						comma = true;
					}
				}
			}
		}
		if (!"".equals(orderItems.toString())) {
			queryStr += " ORDER BY ".concat(orderItems.toString());
		} else {
			// default order
			queryStr += " ORDER BY u.lastname";
		}
		
		Query query = entityManagerSecuritySvc.getInstance().createQuery(queryStr);
		
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			query.setParameter("active", (Boolean) request.getParam(GlobalConstant.ACTIVE));
		} 
		
		if (searchCriteria != null){
			for (LinkedHashMap<String,String> item : searchCriteria) {
				if (item.containsKey(GlobalConstant.SEARCHVALUE) && !"".equals(item.get(GlobalConstant.SEARCHVALUE)) && item.containsKey(GlobalConstant.SEARCHCOLUMN)) {  
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_FIRSTNAME")){
						query.setParameter("firstnameValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_LASTNAME")){
						query.setParameter("lastnameValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_USERNAME")){
						query.setParameter("usernameValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_EMAIL")){
						query.setParameter("emailValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_STATUS")){
						if ("active".equalsIgnoreCase((String)item.get(GlobalConstant.SEARCHVALUE))) {
							query.setParameter("statusValue", true);
						} else if ("disabled".equalsIgnoreCase((String)item.get(GlobalConstant.SEARCHVALUE))) {
							query.setParameter("statusValue", false);
						}
					}
				}
			}
		}

		if (request.containsParam(GlobalConstant.LISTLIMIT) && (Integer) request.getParam(GlobalConstant.LISTLIMIT) != 0){
			query.setFirstResult((Integer) request.getParam(GlobalConstant.LISTSTART));
			query.setMaxResults((Integer) request.getParam(GlobalConstant.LISTLIMIT));
		}
		@SuppressWarnings("unchecked")
		List<User> users = query.getResultList();

		response.addParam(GlobalConstant.ITEMS, users);
	}

	@Override
	public void itemCount(RestRequest request, RestResponse response) throws Exception {
		String queryStr = "SELECT COUNT(DISTINCT u) FROM User AS u ";
		boolean and = false;
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			if (!and) { queryStr += " WHERE "; }
			queryStr += "u.active =:active ";
			and = true;
		}
		
		ArrayList<LinkedHashMap<String,String>> searchCriteria = null;
		if (request.containsParam(GlobalConstant.SEARCHCRITERIA) && !request.getParam(GlobalConstant.SEARCHCRITERIA).equals("")) {
			if (request.getParam(GlobalConstant.SEARCHCRITERIA) instanceof Map) {
				searchCriteria = new ArrayList<>();
				searchCriteria.add((LinkedHashMap<String, String>) request.getParam(GlobalConstant.SEARCHCRITERIA));
			} else {
				searchCriteria = (ArrayList<LinkedHashMap<String, String>>) request.getParam(GlobalConstant.SEARCHCRITERIA);
			}
			
			// Loop through all the criteria
			boolean or = false;
			
			String lookupStr = "";
			for (LinkedHashMap<String,String> item : searchCriteria) {
				if (item.containsKey(GlobalConstant.SEARCHVALUE) && !"".equals(item.get(GlobalConstant.SEARCHVALUE)) && item.containsKey(GlobalConstant.SEARCHCOLUMN)) {
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_FIRSTNAME")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "u.firstname LIKE :firstnameValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_LASTNAME")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "u.lastname LIKE :lastnameValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_USERNAME")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "u.username LIKE :usernameValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_EMAIL")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "u.email LIKE :emailValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_ROLE_TABLE_STATUS")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "u.active LIKE :statusValue"; 
						or = true;
					}
				}
			}
			if (!"".equals(lookupStr)) {
				if (!and) { 
					queryStr += " WHERE ( " + lookupStr + " ) ";
				} else {
					queryStr += " AND ( " + lookupStr + " ) ";
				}
			}
			
		}

		Query query = entityManagerSecuritySvc.getInstance().createQuery(queryStr);
		
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			query.setParameter("active", (Boolean) request.getParam(GlobalConstant.ACTIVE));
		} 
		
		if (searchCriteria != null){
			for (LinkedHashMap<String,String> item : searchCriteria) {
				if (item.containsKey(GlobalConstant.SEARCHVALUE) && !"".equals(item.get(GlobalConstant.SEARCHVALUE)) && item.containsKey(GlobalConstant.SEARCHCOLUMN)) {  
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_FIRSTNAME")){
						query.setParameter("firstnameValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_LASTNAME")){
						query.setParameter("lastnameValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_USERNAME")){
						query.setParameter("usernameValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_USER_TABLE_EMAIL")){
						query.setParameter("emailValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_ROLE_TABLE_STATUS")){
						if ("active".equalsIgnoreCase((String)item.get(GlobalConstant.SEARCHVALUE))) {
							query.setParameter("statusValue", true);
						} else if ("disabled".equalsIgnoreCase((String)item.get(GlobalConstant.SEARCHVALUE))) {
							query.setParameter("statusValue", false);
						}
					}
				}
			}
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
		
			query.setParameter("id", Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
			User user = (User) query.getSingleResult();
			
			response.addParam(GlobalConstant.ITEM, user);
		} else {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.ACTIONFAILED, "Missing ID", response);
		}
	}
	
	@Override
	public void getMembers(RestRequest request, RestResponse response) throws Exception {
		String HQLQuery = "FROM User AS u WHERE u.active = true AND u.archive = false AND u.locked = false AND u.id != :id ";
		if (request.getParam(GlobalConstant.SEARCHVALUE) != null && !((String)request.getParam(GlobalConstant.SEARCHVALUE)).isEmpty()){
			HQLQuery += "AND (u.lastname like :searchValue OR u.firstname like :searchValue) ";
		}
		HQLQuery += "ORDER BY u.lastname ASC";
		Query query = entityManagerSecuritySvc.getInstance().createQuery(HQLQuery);
		if (request.getParam(GlobalConstant.SEARCHVALUE) != null && !((String)request.getParam(GlobalConstant.SEARCHVALUE)).isEmpty()){
			query.setParameter("searchValue", ((String)request.getParam(GlobalConstant.SEARCHVALUE))+"%");
		}
		if ((Integer) request.getParam(GlobalConstant.LISTLIMIT) != 0){
			query.setFirstResult((Integer) request.getParam(GlobalConstant.LISTSTART));
			query.setMaxResults((Integer) request.getParam(GlobalConstant.LISTLIMIT));
		}
		List<User> members = (List<User>) query.setParameter("id",userContext.getCurrentUser().getId()).getResultList();
		
		response.addParam("members", members);
	}

	@Override
	public void selectList(RestRequest request, RestResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void listByUsernameEmail(RestRequest request, RestResponse response) throws Exception {
		String queryStr = "SELECT NEW User(u.id,u.firstname,u.middlename,u.lastname,u.active,u.username,u.created) FROM User AS u WHERE u.active =:active AND (u.username LIKE :usernameValue OR u.email LIKE :emailValue) ORDER BY u.firstname ASC, u.lastname ASC";
		Query query = entityManagerSecuritySvc.getInstance().createQuery(queryStr);
		query.setParameter("active", true);
		query.setParameter("usernameValue", "%"+((String)request.getParam(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
		query.setParameter("emailValue", "%"+((String)request.getParam(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
		query.setMaxResults(10);
		
		@SuppressWarnings("unchecked")
		List<User> users = query.getResultList();

		response.addParam(GlobalConstant.ITEMS, users);
	}
}
