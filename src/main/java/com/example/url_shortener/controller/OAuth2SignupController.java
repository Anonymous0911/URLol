package com.example.url_shortener.controller;

import com.example.url_shortener.model.AppUser;
import com.example.url_shortener.repository.AppUserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OAuth2SignupController {

    private final AppUserRepository userRepository;

    public OAuth2SignupController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/complete-signup")
    public String showCompleteSignup(HttpSession session) {
        // Security check: ensure they actually came from Google/GitHub
        if (session.getAttribute("temp_oauth_email") == null) {
            return "redirect:/";
        }
        return "complete-signup";
    }

    @PostMapping("/complete-signup")
    public String processSignup(@RequestParam String username, HttpSession session) {
        String email = (String) session.getAttribute("temp_oauth_email");

        if (email == null) {
            return "redirect:/";
        }

        // PERFECT MATCH: Using your new existsByUsername method!
        // This is a backend safety net in case they bypass the frontend JavaScript
        if (userRepository.existsByUsername(username)) {
            return "redirect:/complete-signup?error=taken";
        }

        // Save the brand new user to MySQL!
        AppUser newUser = new AppUser();
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setPassword("OAUTH_USER_NO_PASSWORD");

        userRepository.save(newUser);

        // Clean up the temporary session data
        session.removeAttribute("temp_oauth_email");

        return "redirect:/";
    }
}