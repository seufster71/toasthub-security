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
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.toasthub.core.general.api.View;
//import org.codehaus.jackson.annotate.JsonIgnore;
//import org.codehaus.jackson.map.annotate.JsonView;
import org.toasthub.security.model.Text;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "role")
public class Role extends ToastEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	protected String code;
	protected Application application;
	protected Set<RolePermission> permissions;
	protected Instant effStart;
	protected Instant effEnd;
	// transient
	protected Long applicationId;
	protected Set<Long> permissionIds;
	protected UserRole userRole;

	// Constructors
	public Role() {
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
	}
	
	public Role(Text title) {
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setTitle(title);
	}
	
	// Setter/Getters
	@JsonView({View.Admin.class})
	@Column(name = "code")
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	
	@JsonIgnore
	@OneToMany(mappedBy = "role", cascade = CascadeType.ALL)
	public Set<RolePermission> getPermissions() {
		return permissions;
	}
	public void setPermissions(Set<RolePermission> permissions) {
		this.permissions = permissions;
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
	
	@JsonIgnore
	@Transient
	public Set<Long> getPermissionIds() {
		if (this.permissions == null){
			return this.permissionIds;
		} else {
			return new HashSet<Long>();
		}
	}
	public void setPermissoinIds(Set<Long> permissionIds) {
		this.permissionIds = permissionIds;
	}
	
	@JsonView({View.Admin.class})
	@Transient
	public UserRole getUserRole() {
		return userRole;
	}
	public void setUserRole(UserRole userRole) {
		this.userRole = userRole;
	}

}
