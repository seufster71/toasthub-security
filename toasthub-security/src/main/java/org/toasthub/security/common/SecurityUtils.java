package org.toasthub.security.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.toasthub.security.model.RolePermission;
import org.toasthub.security.model.User;
import org.toasthub.security.model.UserRole;

public class SecurityUtils {

	public static Map<String,RolePermission> effectivePermissions(List<UserRole> roles) throws Exception {
		Map<String,RolePermission> perms = new HashMap<String,RolePermission>();
	
		for(UserRole r : roles) {
			Set<RolePermission> permissions = r.getRole().getPermissions();
			for(RolePermission p : permissions){
				perms.put(p.getCode(), p);
			}
		}
		return perms;
	}
	
	public static boolean containsPermission(User user, String code, String rights) throws Exception {
		boolean result = false;
		if (user != null && user.getPermissions().containsKey(code)) {
			String r = user.getPermissions().get(code).getRights();
			if ("W".equals(rights)) {
				if ("W".equals(r)){
					result = true;
				}
			} else if ("R".equals(rights)) {
				if ("R".equals(r) || "W".equals(r)) {
					result = true;
				}
			}
		}
		
		
		return result;
	}
}
