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

/**
 * @author Edward H. Seufert
 */
@Entity
@Table(name = "role_permission")
public class RolePermission extends BaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	
	protected Role role;
	protected Permission permission;
	protected String r;
	protected Date s;
	protected Date e;
	protected String c;
	
	// Constructor
	public RolePermission(){}
	
	public RolePermission(Role role, Permission permission) {
		this.role = role;
		this.permission = permission;
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
	@ManyToOne(targetEntity = Permission.class)
	@JoinColumn(name = "permission_id")
	public Permission getPermission() {
		return permission;
	}
	public void setPermission(Permission permission) {
		this.permission = permission;
	}
	
	@JsonView({View.Member.class,View.Admin.class})
	@Column(name = "rights")
	public String getR() {
		return r;
	}
	public void setR(String rights) {
		this.r = rights;
	}
	
	@JsonView({View.Member.class,View.Admin.class})
	@Column(name = "eff_start", updatable = false)
	public Date getS() {
		return s;
	}
	public void setS(Date effStart) {
		this.s = effStart;
	}
	
	@JsonView({View.Member.class,View.Admin.class})
	@Column(name = "eff_end", updatable = false)
	public Date getE() {
		return e;
	}
	public void setE(Date effEnd) {
		this.e = effEnd;
	}
	
	@JsonView({View.Admin.class})
	@Transient
	public String getC() {
		return permission.getCode();
	}
	
}
