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

package org.toasthub.security.permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.core.common.EntityManagerSecuritySvc;
import org.toasthub.core.common.UtilSvc;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.security.model.Permission;
import org.toasthub.security.model.RolePermission;

@Repository("PermissionDao")
@Transactional("TransactionManagerSecurity")
public class PermissionDaoImpl implements PermissionDao {
	
	@Autowired 
	protected EntityManagerSecuritySvc entityManagerSecuritySvc;
	@Autowired
	protected UtilSvc utilSvc;
	
	@Override
	public void items(RestRequest request, RestResponse response) throws Exception {
		
		String queryStr = "SELECT DISTINCT p FROM Permission AS p JOIN FETCH p.title AS t JOIN FETCH t.langTexts as lt JOIN FETCH p.application AS a JOIN FETCH a.title AS at JOIN FETCH at.langTexts as alt "
			+ "WHERE lt.lang =:lang AND alt.lang =:lang ";
		
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			queryStr += "AND p.active =:active ";
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
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_NAME")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "lt.lang =:lang AND lt.text LIKE :nameValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_CODE")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "p.code LIKE :codeValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_RIGHTS")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "p.rights LIKE :rightsValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_APPLICATION")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "p.application.code LIKE :appValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_STATUS")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "p.active LIKE :statusValue"; 
						or = true;
					}
				}
			}
			if (!"".equals(lookupStr)) {
				queryStr += " AND ( " + lookupStr + " ) ";
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
					if (item.get(GlobalConstant.ORDERCOLUMN).equals("ADMIN_PERMISSION_TABLE_NAME")){
						if (comma) { orderItems.append(","); }
						orderItems.append("lt.text ").append(item.get(GlobalConstant.ORDERDIR));
						comma = true;
					}
					if (item.get(GlobalConstant.ORDERCOLUMN).equals("ADMIN_PERMISSION_TABLE_CODE")){
						if (comma) { orderItems.append(","); }
						orderItems.append("p.code ").append(item.get(GlobalConstant.ORDERDIR));
						comma = true;
					}
					if (item.get(GlobalConstant.ORDERCOLUMN).equals("ADMIN_PERMISSION_TABLE_RIGHTS")){
						if (comma) { orderItems.append(","); }
						orderItems.append("p.rights ").append(item.get(GlobalConstant.ORDERDIR));
						comma = true;
					}
					if (item.get(GlobalConstant.ORDERCOLUMN).equals("ADMIN_PERMISSION_TABLE_APPLICATION")){
						if (comma) { orderItems.append(","); }
						orderItems.append("p.application.code ").append(item.get(GlobalConstant.ORDERDIR));
						comma = true;
					}
					if (item.get(GlobalConstant.ORDERCOLUMN).equals("ADMIN_PERMISSION_TABLE_STATUS")){
						if (comma) { orderItems.append(","); }
						orderItems.append("p.active ").append(item.get(GlobalConstant.ORDERDIR));
						comma = true;
					}
				}
			}
		}
		if (!"".equals(orderItems.toString())) {
			queryStr += " ORDER BY ".concat(orderItems.toString());
		} else {
			// default order
			queryStr += " ORDER BY lt.text";
		}
		
		Query query = entityManagerSecuritySvc.getInstance().createQuery(queryStr);
		
		query.setParameter("lang",request.getParam(GlobalConstant.LANG));
		
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			query.setParameter("active", (Boolean) request.getParam(GlobalConstant.ACTIVE));
		} 
		
		if (searchCriteria != null){
			for (LinkedHashMap<String,String> item : searchCriteria) {
				if (item.containsKey(GlobalConstant.SEARCHVALUE) && !"".equals(item.get(GlobalConstant.SEARCHVALUE)) && item.containsKey(GlobalConstant.SEARCHCOLUMN)) {  
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_NAME")){
						query.setParameter("nameValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
						query.setParameter("lang",request.getParam(GlobalConstant.LANG));
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_CODE")){
						query.setParameter("codeValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_RIGHTS")){
						query.setParameter("rightsValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_APPLICATION")){
						query.setParameter("appValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_STATUS")){
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
		List<Permission> permissions = query.getResultList();

		response.addParam(GlobalConstant.ITEMS, permissions);
	}

	@Override
	public void itemCount(RestRequest request, RestResponse response) throws Exception {
		String queryStr = "SELECT COUNT(DISTINCT p) FROM Permission as p JOIN p.title AS t JOIN t.langTexts as lt ";
		boolean and = false;
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			if (!and) { queryStr += " WHERE "; }
			queryStr += "p.active =:active ";
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
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_NAME")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "lt.lang =:lang AND lt.text LIKE :nameValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_CODE")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "p.code LIKE :codeValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_RIGHT")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "p.rights LIKE :rightsValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_APPLICATION")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "p.application.code LIKE :appValue"; 
						or = true;
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_STATUS")){
						if (or) { lookupStr += " OR "; }
						lookupStr += "p.active LIKE :statusValue"; 
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
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_NAME")){
						query.setParameter("nameValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
						query.setParameter("lang",request.getParam(GlobalConstant.LANG));
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_CODE")){
						query.setParameter("codeValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_RIGHTS")){
						query.setParameter("rightsValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_APPLICATION")){
						query.setParameter("appValue", "%"+((String)item.get(GlobalConstant.SEARCHVALUE)).toLowerCase()+"%");
					}
					if (item.get(GlobalConstant.SEARCHCOLUMN).equals("ADMIN_PERMISSION_TABLE_STATUS")){
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
			String queryStr = "SELECT p FROM Permission AS p JOIN FETCH p.title AS t JOIN FETCH t.langTexts WHERE p.id =:id";
			Query query = entityManagerSecuritySvc.getInstance().createQuery(queryStr);
		
			query.setParameter("id", request.getParamLong(GlobalConstant.ITEMID));
			Permission permission = (Permission) query.getSingleResult();
			
			response.addParam(GlobalConstant.ITEM, permission);
		} else {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.ACTIONFAILED, "Missing ID", response);
		}
	}

	@Override
	public void rolePermissionIds(RestRequest request, RestResponse response) {
		if (request.containsParam(GlobalConstant.PARENTID) && !"".equals(request.getParam(GlobalConstant.PARENTID))) {
			String queryStr = "SELECT new RolePermission(rp.id, rp.active, rp.rights, rp.startDate, rp.endDate, rp.permission.id) FROM RolePermission AS rp WHERE rp.role.id =:id";
			Query query = entityManagerSecuritySvc.getInstance().createQuery(queryStr);
		
			query.setParameter("id", request.getParamLong(GlobalConstant.PARENTID));
			List<RolePermission> permissions = query.getResultList();
			
			response.addParam("rolePermissions", permissions);
		} else {
			utilSvc.addStatus(RestResponse.ERROR, RestResponse.ACTIONFAILED, "Missing ID", response);
		}
	}

	@Override
	public void selectList(RestRequest request, RestResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
