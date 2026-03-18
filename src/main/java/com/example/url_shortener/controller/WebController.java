package com.example.url_shortener.controller;

import com.example.url_shortener.model.AppUser;
import com.example.url_shortener.model.UrlLink;
import com.example.url_shortener.repository.AppUserRepository;
import com.example.url_shortener.service.UrlService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class WebController {

    private final UrlService urlService;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public WebController(UrlService urlService, AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.urlService = urlService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- FRONTEND VIEWS ---

    @GetMapping("/")
    public String index(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    @GetMapping("/register")
    public String registerPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String name, @RequestParam String email,
                               @RequestParam String password, RedirectAttributes redirectAttributes) {
        if (userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email already in use!");
            return "redirect:/register";
        }

        AppUser newUser = new AppUser();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password)); // Hash password!
        userRepository.save(newUser);

        redirectAttributes.addFlashAttribute("message", "Registration successful! Please login.");
        return "redirect:/";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String userId = extractUserId(authentication);
        String name = extractUserName(authentication);

        model.addAttribute("name", name);
        model.addAttribute("links", urlService.getUserLinks(userId));
        return "dashboard";
    }

    @PostMapping("/shorten")
    public String shortenUrl(@RequestParam String originalUrl,
                             @RequestParam(required = false) String customAlias,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String userId = extractUserId(authentication);
            urlService.createShortLink(originalUrl, customAlias, userId);
            redirectAttributes.addFlashAttribute("message", "Link shortened successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    // --- REDIRECT LOGIC ---
    @GetMapping("/{shortCode}")
    public String redirect(@PathVariable String shortCode) {
        Optional<UrlLink> link = urlService.getOriginalUrl(shortCode);
        if (link.isPresent()) {
            String original = link.get().getOriginalUrl();
            if (!original.startsWith("http://") && !original.startsWith("https://")) {
                original = "https://" + original;
            }
            return "redirect:" + original;
        }
        return "redirect:/?error=notfound";
    }

    // --- HELPER METHODS ---
    private String extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("login") != null ? oauth2User.getAttribute("login") : oauth2User.getAttribute("email");
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername(); // We use email as username
        }
        return "anonymous";
    }

    private String extractUserName(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("name");
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            // Fetch name from DB
            return userRepository.findByEmail(userDetails.getUsername())
                    .map(AppUser::getName).orElse("User");
        }
        return "User";
    }
}