package com.example.url_shortener.service;

import com.example.url_shortener.model.UrlLink;
import com.example.url_shortener.repository.UrlLinkRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UrlService {

    private final UrlLinkRepository repository;
    private final Random random = new Random();


    private static final String[] ADJECTIVES = {
            "Chunky", "Derpy", "Caffeinated", "Recursive", "Stochastic",
            "Sassy", "Grumpy", "Fuzzy", "Bayesian", "Quantum",
            "Jittery", "Lazy", "Hyper", "Iterative", "Spicy"
    };


    private static final String[] NOUNS = {
            "Potato", "Penguin", "Tensor", "Algorithm", "Dinosaur",
            "Pixel", "Waffle", "Unicorn", "Matrix", "Variable",
            "Toaster", "Ninja", "Gradient", "Muffin", "Bug"
    };

    public UrlService(UrlLinkRepository repository) {
        this.repository = repository;
    }

    public UrlLink createShortLink(String originalUrl, String customAlias, String userEmail) {
        // Use the custom alias if provided, otherwise generate a funny one
        String shortCode = (customAlias != null && !customAlias.isBlank()) ? customAlias : generateFunnyCode();

        if (repository.existsByShortCode(shortCode)) {
            throw new IllegalArgumentException("Alias already in use!");
        }

        UrlLink link = new UrlLink();
        link.setOriginalUrl(originalUrl);
        link.setShortCode(shortCode);
        link.setUserEmail(userEmail);
        return repository.save(link);
    }

    public Optional<UrlLink> getOriginalUrl(String shortCode) {
        Optional<UrlLink> linkOpt = repository.findByShortCode(shortCode);
        linkOpt.ifPresent(link -> {
            link.setClickCount(link.getClickCount() + 1);
            repository.save(link);
        });
        return linkOpt;
    }

    public List<UrlLink> getUserLinks(String email) {
        return repository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    private String generateFunnyCode() {
        String code;
        do {
            String adj = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
            String noun = NOUNS[random.nextInt(NOUNS.length)];
            // Adding a small random number (1-99) at the end to massively reduce collisions
            int num = random.nextInt(99) + 1;
            code = adj + noun + num;
        } while (repository.existsByShortCode(code));

        return code;
    }
}