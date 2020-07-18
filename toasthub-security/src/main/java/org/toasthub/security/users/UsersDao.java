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

import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.security.common.BaseDao;
import org.toasthub.security.model.User;

public interface UsersDao extends BaseDao {

	public void saveUser(RestRequest request, RestResponse response) throws Exception;
	public void resetPassword(String username, String password, String salt, String sessionToken) throws Exception;
	public void changePassword(String username, String password, String salt, String sessionToken) throws Exception;
	public User findUser(String username) throws Exception;
	public User findUserByEmail(String email) throws Exception;
	public void updateUser(RestRequest request, RestResponse response) throws Exception;
	public void getMembers(RestRequest request, RestResponse response) throws Exception; 
	public void listByUsernameEmail(RestRequest request, RestResponse response) throws Exception;
}
