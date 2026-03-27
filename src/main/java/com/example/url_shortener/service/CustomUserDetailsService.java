package com.example.url_shortener.service;


import com.example.url_shortener.model.AppUser;
import com.example.url_shortener.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public CustomUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        AppUser appUser = userRepository.findByEmailOrUsername(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new User(appUser.getUsername(), appUser.getPassword(), Collections.emptyList());
    }
}