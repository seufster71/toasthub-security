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

package org.toasthub.security.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "user_role")
public class UserRole extends BaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	
	protected Role role;
	protected User user;
	protected Integer order;
	protected Date startDate;
	protected Date endDate;
	
	// transient
	protected Long roleId;
	
	// Constructor
	public UserRole(){}
	
	public UserRole(User user, Role role){
		this.user = user;
		this.role = role;
	}
	
	public UserRole(Long id, boolean active, Integer order, Date startDate, Date endDate, Long roleId) {
		this.id = id;
		this.active = active;
		this.order = order;
		this.startDate = startDate;
		this.endDate = endDate;
		this.roleId = roleId;
	}
	
	// Methods
	@JsonIgnore
	@ManyToOne(targetEntity = Role.class)
	@JoinColumn(name = "role_id")
	public Role getRole() {
		return role;
	}
	public void setRole(Role role) {
		this.role = role;
	}
	
	@JsonIgnore
	@ManyToOne(targetEntity = User.class)
	@JoinColumn(name = "user_id")
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	
	@JsonView({View.Member.class,View.Admin.class,View.System.class})
	@Column(name = "sort_order")
	public Integer getOrder() {
		return order;
	}
	public void setOrder(Integer order) {
		this.order = order;
	}
	
	@JsonView({View.Member.class,View.Admin.class,View.System.class})
	@Column(name = "eff_start")
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	@JsonView({View.Member.class,View.Admin.class,View.System.class})
	@Column(name = "eff_end")
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	@JsonView({View.Admin.class})
	@Transient
	public Long getRoleId() {
		if (this.role == null) {
			return this.roleId;
		} else {
			return this.role.getId();
		}
	}
	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}
}
