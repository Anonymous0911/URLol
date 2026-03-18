package com.example.url_shortener.config;

import com.example.url_shortener.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/register", "/error", "/{shortCode:[a-zA-Z0-9_-]+}").permitAll()
                        .anyRequest().authenticated()
                )
                // Standard Form Login
                .formLogin(form -> form
                        .loginPage("/") // Use our custom index page
                        .loginProcessingUrl("/login") // Spring handles this automatically
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/?error=bad_credentials")
                        .permitAll()
                )
                // OAuth2 Login
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/")
                        .defaultSuccessUrl("/dashboard", true)
                )
                // Remember Me Configuration
                .rememberMe(rememberMe -> rememberMe
                        .key("mySuperSecretKeyForUrlShortener") // Used to hash the cookie
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days expiration
                        .rememberMeParameter("remember-me") // Checkbox name in HTML
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}