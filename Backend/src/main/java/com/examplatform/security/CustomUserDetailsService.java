package com.examplatform.security;

import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(
                email.trim().toLowerCase(Locale.ROOT)
        ).orElseThrow(() ->
                new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash() == null
                        ? ""
                        : user.getPasswordHash())
                .authorities(new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name()))
                .build();
    }
}