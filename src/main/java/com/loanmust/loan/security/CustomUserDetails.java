package com.loanmust.loan.security;

import com.loanmust.loan.user.Role;
import com.loanmust.loan.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal. Exposes tenant id, role and display name so controllers
 * and templates can read the signed-in context without another database round trip.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() { return user; }

    public Long getId() { return user.getId(); }

    public Long getTenantId() { return user.getTenantId(); }

    public String getFullName() { return user.getFullName(); }

    public Role getRole() { return user.getRole(); }

    public String getRoleLabel() { return user.getRoleLabel(); }

    public boolean isMustChangePassword() { return user.isMustChangePassword(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().authority()));
    }

    @Override
    public String getPassword() { return user.getPassword(); }

    @Override
    public String getUsername() { return user.getEmail(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    // Sign-in requires both an active account and the "allow login" toggle.
    @Override
    public boolean isEnabled() { return user.isEnabled() && user.isLoginEnabled(); }
}
