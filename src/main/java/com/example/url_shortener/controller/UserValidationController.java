package com.example.url_shortener.controller;

import com.example.url_shortener.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/users")
public class UserValidationController {

    private final AppUserRepository userRepository;

    public UserValidationController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();

        // 1. Check if it exists in MySQL
        boolean isTaken = userRepository.existsByUsername(username);

        if (isTaken) {
            response.put("available", false);
            // 2. Generate 3 unique suggestions
            response.put("suggestions", generateSuggestions(username));
        } else {
            response.put("available", true);
        }

        return ResponseEntity.ok(response);
    }

    // Helper method to generate similar usernames
    private List<String> generateSuggestions(String baseName) {
        List<String> suggestions = new ArrayList<>();
        Random random = new Random();

        // Keep generating until we have 3 available options
        while (suggestions.size() < 3) {
            // Append a random number between 10 and 999
            String suggestion = baseName + (random.nextInt(990) + 10);

            // Make sure our suggestion isn't ALSO taken!
            if (!userRepository.existsByUsername(suggestion) && !suggestions.contains(suggestion)) {
                suggestions.add(suggestion);
            }
        }
        return suggestions;
    }
}