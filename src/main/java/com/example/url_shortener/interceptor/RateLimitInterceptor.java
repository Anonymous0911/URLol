package com.example.url_shortener.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!request.getRequestURI().equals("/shorten") || !request.getMethod().equalsIgnoreCase("POST")) {
            return true;
        }

        String clientIdentifier = getClientIdentifier(request);
        String redisKey = "rate_limit:" + clientIdentifier;

        // Increment the counter for this user/IP in Redis
        Long requests = redisTemplate.opsForValue().increment(redisKey);

        // If this is the first request, set the key to expire in 60 seconds
        if (requests != null && requests == 1) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(1));
        }

        if (requests != null && requests > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded. Please wait a minute before creating more links.");
            return false; // Block the request
        }

        return true; // Allow the request
    }

    private String getClientIdentifier(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getName(); // Use their logged-in username/email
        }
        // Fallback to IP address if not logged in
        return request.getRemoteAddr();
    }
}