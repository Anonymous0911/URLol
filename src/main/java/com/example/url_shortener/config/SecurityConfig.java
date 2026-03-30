package com.example.url_shortener.config;

import com.example.url_shortener.service.CustomUserDetailsService;
// Make sure to import your new CustomOAuth2UserService!
import com.example.url_shortener.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final ClientRegistrationRepository clientRegistrationRepository;

    // Inject all three required services/repositories
    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          CustomOAuth2UserService customOAuth2UserService,
                          ClientRegistrationRepository clientRegistrationRepository) {
        this.userDetailsService = userDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
        this.clientRegistrationRepository = clientRegistrationRepository;
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
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/?error=bad_credentials")
                        .permitAll()
                )
                // OAuth2 Login
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/")
                        // 1. Force Google to show the account selection screen
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(customAuthorizationRequestResolver())
                        )
                        // 2. Use our custom service to handle automatic sign-up/sign-in
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .defaultSuccessUrl("/", true)
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

    // Helper method to append "prompt=select_account" to OAuth requests (specifically for Google)
    private OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
                );

        defaultResolver.setAuthorizationRequestCustomizer(customizer -> {
            customizer.additionalParameters(params -> {
                params.put("prompt", "select_account");
            });
        });

        return defaultResolver;
    }
}