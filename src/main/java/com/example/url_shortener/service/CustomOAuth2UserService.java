package com.example.url_shortener.service;

import com.example.url_shortener.model.AppUser; // Replace with your actual User entity import
import com.example.url_shortener.repository.AppUserRepository; // Replace with your actual repo import
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AppUserRepository userRepository;

    public CustomOAuth2UserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Let Spring Security do the heavy lifting of fetching the user's data from Google/GitHub
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Find out which provider they used (google or github)
        String provider = userRequest.getClientRegistration().getRegistrationId();

        String email = "";
        String name = "";

        // Extract the data based on the provider
        if (provider.equals("google")) {
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        } else if (provider.equals("github")) {
            // GitHub users can hide their public email. If so, we use their username as a fallback.
            email = oAuth2User.getAttribute("email");
            if (email == null) {
                email = oAuth2User.getAttribute("login") + "@github.com";
            }
            name = oAuth2User.getAttribute("name");
            if (name == null) {
                name = oAuth2User.getAttribute("login");
            }
        }

        // Automatic Sign-Up Logic!
        // Check if the user exists in our MySQL database
        Optional<AppUser> existingUser = userRepository.findByEmailOrUsername(email,name);

        if (existingUser.isEmpty()) {
            AppUser newUser = new AppUser();
            newUser.setEmail(email);
            newUser.setName(name);


            userRepository.save(newUser);
            System.out.println("New user signed up automatically: " + email);
        } else {
            System.out.println("Existing user logged in: " + email);
        }


        return oAuth2User;
    }
}