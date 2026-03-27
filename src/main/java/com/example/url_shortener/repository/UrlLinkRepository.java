package com.example.url_shortener.repository;

import com.example.url_shortener.model.UrlLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UrlLinkRepository extends JpaRepository<UrlLink, Long> {
    Optional<UrlLink> findByShortCode(String shortCode);
    List<UrlLink> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    boolean existsByShortCode(String shortCode);
    Optional<UrlLink> findFirstByUserEmailOrderByCreatedAtDesc(String userEmail);
    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime now);
}