package com.picsel.backend_v2.security;

import com.picsel.backend_v2.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class PicselUserDetails implements UserDetails {

    private final Long seq;
    private final String uuid;
    private final String email;
    private final String passwordHash;

    public PicselUserDetails(User user) {
        this.seq = user.getSeq();
        this.uuid = user.getUuid();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return String.valueOf(seq);
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
