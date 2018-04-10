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
import java.util.Map;


import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserContext implements Serializable {

   private static final long serialVersionUID = 7965455427888195913L;
   
   private User currentUser;
   private Long userRefId;
   
   public User getCurrentUser() {
      return currentUser;
   }

   public void setCurrentUser(User user){
	   this.currentUser = user;
   }

   public Long getUserRefId() {
	   return userRefId;
   }
	
   public void setUserRefId(Long userRefId) {
	   this.userRefId = userRefId;
   }
	
}