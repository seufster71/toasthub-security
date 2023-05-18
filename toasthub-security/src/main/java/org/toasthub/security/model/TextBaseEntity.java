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

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

@MappedSuperclass()
public class TextBaseEntity implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private Long id;
	private Instant modified;
	private Instant created;
	private Long version;
	
	// Constructor
	public TextBaseEntity() {
		this.setCreated(Instant.now());
	}
	// Setter/Getter
	@Id	
	@JsonView({View.Admin.class,View.System.class})
	@GeneratedValue(strategy=GenerationType.IDENTITY) 
	@Column(name = "id")
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@JsonView({View.Admin.class,View.System.class})
	@Column(name = "modified",updatable = false, insertable = false)
	//@org.hibernate.annotations.Generated(org.hibernate.annotations.GenerationTime.ALWAYS)
	public Instant getModified() {
		return modified;
	}
	public void setModified(Instant modified) {
		this.modified = modified;
	}
	
	@JsonView({View.Admin.class,View.System.class})
	@Column(name = "created", updatable = false, insertable = false)
	public Instant getCreated() {
		return created;
	}
	public void setCreated(Instant created) {
		this.created = created;
	}
	
	@JsonIgnore
	@Version 
	@Column(name = "version")
	public Long getVersion() {
		return version;
	}
	public void setVersion(Long version) {
		this.version = version;
	}

}
