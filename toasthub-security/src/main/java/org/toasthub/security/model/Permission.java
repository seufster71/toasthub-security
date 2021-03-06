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
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.toasthub.core.general.api.View;
import org.toasthub.security.model.Text;

import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "permission")
public class Permission extends ToastEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	protected String rights;
	protected String code;
	protected Application application;
	protected Instant effStart;
	protected Instant effEnd;
	// transient
	protected Long applicationId;
	protected RolePermission rolePermission;
	
	// constructors
	public Permission(){
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
	}
	
	public Permission(String code, Text title, String rights) {
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setCode(code);
		this.setTitle(title);
		this.setRights(rights);
	}
	

	// Setters and getters
	@JsonView({View.Admin.class})
	@Column(name = "rights")
	public String getRights() {
		return rights;
	}
	public void setRights(String rights) {
		this.rights = rights;
	}

	@JsonView({View.Admin.class})
	@Column(name = "code")
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	
	@JsonView({View.Admin.class})
	@ManyToOne(targetEntity = Application.class)
	@JoinColumn(name = "application_id")
	public Application getApplication() {
		return this.application;
	}
	public void setApplication(Application application) {
		this.application = application;
	}
	
	@JsonView({View.Member.class,View.Admin.class})
	@Column(name = "eff_start", updatable = false)
	public Instant getEffStart() {
		return effStart;
	}
	public void setEffStart(Instant effStart) {
		this.effStart = effStart;
	}
	
	@JsonView({View.Member.class,View.Admin.class})
	@Column(name = "eff_end", updatable = false)
	public Instant getEffEnd() {
		return effEnd;
	}
	public void setEffEnd(Instant effEnd) {
		this.effEnd = effEnd;
	}
	
	@JsonView({View.Admin.class})
	@Transient
	public Long getApplicationId() {
		if (this.application == null){
			return this.applicationId;
		} else {
			return this.application.getId();
		}
	}
	public void setApplicationId(Long applicationId) {
		this.applicationId = applicationId;
	}
	
	@JsonView({View.Admin.class})
	@Transient
	public RolePermission getRolePermission() {
		return this.rolePermission;
	}
	public void setRolePermission(RolePermission rolePermission) {
		this.rolePermission = rolePermission;
	}
}
