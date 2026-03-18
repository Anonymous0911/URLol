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
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    public UrlService(UrlLinkRepository repository) {
        this.repository = repository;
    }

    public UrlLink createShortLink(String originalUrl, String customAlias, String userEmail) {
        String shortCode = (customAlias != null && !customAlias.isBlank()) ? customAlias : generateRandomCode();

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

    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder code;
        do {
            code = new StringBuilder();
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
        } while (repository.existsByShortCode(code.toString()));
        return code.toString();
    }
}