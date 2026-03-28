package com.example.url_shortener.controller;

import com.example.url_shortener.model.AppUser;
import com.example.url_shortener.model.UrlLink;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.example.url_shortener.service.QrCodeService;
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
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

@Controller
public class WebController {

    private final UrlService urlService;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QrCodeService qrCodeService;

    public WebController(UrlService urlService, AppUserRepository userRepository,
                         PasswordEncoder passwordEncoder, QrCodeService qrCodeService) {
        this.urlService = urlService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.qrCodeService = qrCodeService;
    }

    // --- FRONTEND VIEWS ---

    @GetMapping("/")
    public String index(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            // User is logged in! Fetch their details and their most recent link
            String userId = extractUserId(authentication);
            model.addAttribute("name", extractUserName(authentication));

            // Fetch the latest link
            Optional<UrlLink> latestLink = urlService.getLatestUserLink(userId);
            latestLink.ifPresent(link -> model.addAttribute("latestLink", link));
        }
        return "index";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/";
        }

        String userId = extractUserId(authentication);
        List<UrlLink> userLinks = urlService.getUserLinks(userId);

        int totalClicks = userLinks.stream().mapToInt(UrlLink::getClickCount).sum();

        model.addAttribute("name", extractUserName(authentication));
        model.addAttribute("links", userLinks);
        model.addAttribute("totalClicks", totalClicks);

        return "profile";
    }
    @PostMapping("/update-password")
    public String updatePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        String identifier = extractUserId(authentication);
        Optional<AppUser> userOpt = userRepository.findByEmailOrUsername(identifier, identifier);

        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();

            // Safety check: If they registered via OAuth2, they won't have a password in the DB
            if (user.getPassword() == null) {
                redirectAttributes.addFlashAttribute("error", "You logged in via Google/GitHub. You cannot change your password here.");
                return "redirect:/profile";
            }

            // Verify current password is correct
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Incorrect current password.");
                return "redirect:/profile";
            }

            // Update to new password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("message", "Password updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        }
        return "redirect:/profile";
    }

    // NEW: This serves the actual HTML page when someone clicks "Sign up"
    @GetMapping("/register")
    public String registerPage(Authentication authentication) {
        // If they are already logged in, send them to the home page
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            return "redirect:/";
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String name, @RequestParam String username,
                               @RequestParam String email, @RequestParam String password,
                               RedirectAttributes redirectAttributes) {
        if (userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email already in use!");
            return "redirect:/register";
        }
        if (userRepository.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("error", "Username already taken!");
            return "redirect:/register";
        }

        AppUser newUser = new AppUser();
        newUser.setName(name);
        newUser.setUsername(username); // Save username
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        userRepository.save(newUser);

        redirectAttributes.addFlashAttribute("message", "Registration successful! Please login.");
        return "redirect:/";
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
        return "redirect:/";
    }

    // --- REDIRECT LOGIC ---
    @GetMapping("/{shortCode}")
    public String redirect(@PathVariable String shortCode) {
        // 1. Fetch from Redis Cache (or MySQL if not cached yet)
        String originalUrl = urlService.getCachedUrl(shortCode);

        if (originalUrl != null) {
            // 2. Fire and forget the click analytics in the background
            urlService.incrementClickCountAsync(shortCode);

            // 3. Format and Redirect
            if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
                originalUrl = "https://" + originalUrl;
            }
            return "redirect:" + originalUrl;
        }
        return "redirect:/?error=notfound";
    }

    // --- HELPER METHODS ---
    private String extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("login") != null ? oauth2User.getAttribute("login") : oauth2User.getAttribute("email");
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername(); // We use email/username as the identifier
        }
        return "anonymous";
    }

    private String extractUserName(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("name");
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            // Fetch name from DB using the new method we created!
            String identifier = userDetails.getUsername();
            return userRepository.findByEmailOrUsername(identifier, identifier)
                    .map(AppUser::getName).orElse("User");
        }
        return "User";
    }

    // --- QR GENERATION ---
//    @GetMapping(value = "/qr/{shortCode}", produces = MediaType.IMAGE_PNG_VALUE)
//    @ResponseBody
//    public ResponseEntity<byte[]> getQrCode(@PathVariable String shortCode) {
//        String fullUrl = "http://localhost:8080/" + shortCode;
//
//        // Generate a 250x250 pixel QR code
//        byte[] imageBytes = qrCodeService.generateQrCodeImage(fullUrl, 250, 250);
//
//        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes);
//    }


    @GetMapping(value = "/qr/{shortCode}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getQrCode(@PathVariable String shortCode, jakarta.servlet.http.HttpServletRequest request) {

        // Dynamically get the base URL (e.g., http://localhost:8080 OR https://your-app.onrender.com)
        String scheme = request.getHeader("X-Forwarded-Proto"); // Catches Render's HTTPS proxy
        if (scheme == null) {
            scheme = request.getScheme(); // Falls back to local HTTP
        }
        String serverName = request.getServerName();
        String fullUrl = scheme + "://" + serverName + "/" + shortCode;

        byte[] imageBytes = qrCodeService.generateQrCodeImage(fullUrl, 250, 250);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes);
    }
}