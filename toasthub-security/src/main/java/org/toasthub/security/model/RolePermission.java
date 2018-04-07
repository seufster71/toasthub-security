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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.toasthub.core.general.api.View;

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
	protected Boolean canRead;
	protected Boolean canWrite;
	protected Date effStart;
	protected Date effEnd;
	
	// Constructor
	public RolePermission(){}
	
	public RolePermission(Role role, Permission permission) {
		this.role = role;
		this.permission = permission;
	}
	
	// Methods
	
	@ManyToOne(targetEntity = Role.class)
	@JoinColumn(name = "role_id")
	public Role getRole() {
		return role;
	}
	public void setRole(Role role) {
		this.role = role;
	}
	
	@ManyToOne(targetEntity = Permission.class)
	@JoinColumn(name = "permission_id")
	public Permission getPermission() {
		return permission;
	}
	public void setPermission(Permission permission) {
		this.permission = permission;
	}
	
	@JsonView({View.Member.class,View.Admin.class})
	@Column(name = "can_read")
	public Boolean getCanRead() {
		return canRead;
	}
	public void setCanRead(Boolean canRead) {
		this.canRead = canRead;
	}

	@JsonView({View.Member.class,View.Admin.class})
	@Column(name = "can_write")
	public Boolean getCanWrite() {
		return canWrite;
	}
	public void setCanWrite(Boolean canWrite) {
		this.canWrite = canWrite;
	}
	
	@JsonView({View.Member.class,View.Admin.class})
	@Column(name = "eff_start", updatable = false)
	public Date getEffStart() {
		return effStart;
	}
	public void setEffStart(Date effStart) {
		this.effStart = effStart;
	}
	
	@JsonView({View.Member.class,View.Admin.class})
	@Column(name = "eff_end", updatable = false)
	public Date getEffEnd() {
		return effEnd;
	}
	public void setEffEnd(Date effEnd) {
		this.effEnd = effEnd;
	}
}
