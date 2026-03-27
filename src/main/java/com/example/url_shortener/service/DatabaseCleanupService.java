package com.example.url_shortener.service;

import com.example.url_shortener.repository.UrlLinkRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;

@Service
public class DatabaseCleanupService {

    private final UrlLinkRepository repository;

    public DatabaseCleanupService(UrlLinkRepository repository) {
        this.repository = repository;
    }

    // Runs automatically every day at midnight (00:00:00)
    // Cron format: Second, Minute, Hour, Day of Month, Month, Day of Week
    @Scheduled(cron = "0 0 0 * * *")
    @CacheEvict(value = "urls", allEntries = true) // Clears the Redis cache to remove dead links
    public void deleteExpiredLinks() {
        System.out.println("🧹 [CRON JOB] Starting cleanup of expired links...");

        repository.deleteByExpiresAtBefore(LocalDateTime.now());

        System.out.println("✅ [CRON JOB] Cleanup complete.");
    }
}