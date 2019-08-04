package org.toasthub.security.model;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonView;

public class MyUserPrincipal implements UserDetails {

	private static final long serialVersionUID = 1L;
	private User user;
	
	public MyUserPrincipal(User user) {
		this.setUser(user);
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPassword() {
		return this.user.getPassword();
	}

	@Override
	public String getUsername() {
		return this.user.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		return !this.user.isLocked();
	}

	@Override
	public boolean isAccountNonLocked() {
		return !this.user.isLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return !this.user.isLocked();
	}

	@Override
	public boolean isEnabled() {
		return this.user.isActive();
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}
