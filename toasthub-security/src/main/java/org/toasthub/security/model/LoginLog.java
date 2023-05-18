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

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "login_log")
public class LoginLog {

	private Long id;
	private String username;
	private String ipaddress;
	private String appname;
	private String status;
	private Instant modified;
	private Instant created;
	private Long version;
	
	public final static String SUCCESS = "SUCCESS";
	public final static String FAIL_BAD_USER = "FAIL_BAD_USER";
	public final static String FAIL_BAD_PASS = "FAIL_BAD_PASS";
	public final static String FAIL_BAD_EMAIL_CONFIRM = "FAIL_BAD_EMAIL_CONFIRM";
	
	// Constructors
	public LoginLog(){}
	
	public LoginLog(String username, String ipaddress, String status){
		this.setUsername(username);
		this.setIpaddress(ipaddress);
		this.setStatus(status);
	}
	// Methods
	@Id	
	@GeneratedValue(strategy=GenerationType.IDENTITY) 
	@Column(name = "id")
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "user_name")
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@Column(name = "ip_address")
	public String getIpaddress() {
		return ipaddress;
	}
	public void setIpaddress(String ipaddress) {
		this.ipaddress = ipaddress;
	}
	
	@Column(name = "app_name")
	public String getAppname() {
		return appname;
	}
	public void setAppname(String appname) {
		this.appname = appname;
	}
	
	@Column(name = "status")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@Column(name = "modified",updatable = false, insertable = false)
	public Instant getModified() {
		return modified;
	}
	public void setModified(Instant modified) {
		this.modified = modified;
	}
	
	@Column(name = "created", updatable = false, insertable = false)
	public Instant getCreated() {
		return created;
	}
	public void setCreated(Instant created) {
		this.created = created;
	}
	
	@Version 
	@Column(name = "version")
	public Long getVersion() {
		return version;
	}
	public void setVersion(Long version) {
		this.version = version;
	}

}
