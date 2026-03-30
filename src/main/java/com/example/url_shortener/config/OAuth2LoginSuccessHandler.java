package com.example.url_shortener.config;

import com.example.url_shortener.repository.AppUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AppUserRepository userRepository;

    public OAuth2LoginSuccessHandler(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();

        String provider = token.getAuthorizedClientRegistrationId();
        String email = oAuth2User.getAttribute("email");

        // GitHub fallback if email is hidden
        if (email == null && provider.equals("github")) {
            email = oAuth2User.getAttribute("login") + "@github.com";
        }

        // PERFECT MATCH: Using your new existsByEmail method!
        if (!userRepository.existsByEmail(email)) {
            // NEW USER! Save email in session and route to username selection
            request.getSession().setAttribute("temp_oauth_email", email);
            getRedirectStrategy().sendRedirect(request, response, "/complete-signup");
        } else {
            // EXISTING USER! Send straight to the dashboard
            super.setDefaultTargetUrl("/");
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}