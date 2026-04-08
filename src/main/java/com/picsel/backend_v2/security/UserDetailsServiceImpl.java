package com.picsel.backend_v2.security;

import com.picsel.backend_v2.domain.User;
import com.picsel.backend_v2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new PicselUserDetails(user);
    }

    public UserDetails loadUserByUuid(String uuid) throws UsernameNotFoundException {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with uuid: " + uuid));
        return new PicselUserDetails(user);
    }
}
