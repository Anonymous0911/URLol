package com.example.url_shortener.config;

import com.example.url_shortener.service.CustomUserDetailsService;
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
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler; // <-- NEW BOUNCER INJECTED

    // Inject the required services/repositories
    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          ClientRegistrationRepository clientRegistrationRepository,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Single, clean authorizeHttpRequests block
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/register", "/error", "/api/users/check-username", "/complete-signup", "/{shortCode:[a-zA-Z0-9_-]+}").permitAll()
                        .anyRequest().authenticated()
                )

                // 2. Standard Form Login
                .formLogin(form -> form
                        .loginPage("/") // Use our custom index page
                        .loginProcessingUrl("/login") // Spring handles this automatically
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/?error=bad_credentials")
                        .permitAll()
                )

                // 3. OAuth2 Login (Google & GitHub)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/")
                        // Force Google to show the account selection screen
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(customAuthorizationRequestResolver())
                        )
                        // Use our custom Bouncer to handle the signup vs login flow!
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                // 4. Remember Me Configuration
                .rememberMe(rememberMe -> rememberMe
                        .key("mySuperSecretKeyForUrlShortener") // Used to hash the cookie
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days expiration
                        .rememberMeParameter("remember-me") // Checkbox name in HTML
                )

                // 5. Logout Configuration
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )

                // 6. Security (Disabled CSRF for simpler API testing during dev)
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