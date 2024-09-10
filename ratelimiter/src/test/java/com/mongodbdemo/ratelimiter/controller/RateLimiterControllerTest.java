package com.mongodbdemo.ratelimiter.controller;

import com.mongodbdemo.ratelimiter.dto.ApiResponse;
import com.mongodbdemo.ratelimiter.dto.RateLimiterResponse;
import com.mongodbdemo.ratelimiter.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class RateLimiterControllerTest {

    @InjectMocks
    private RateLimiterController rateLimiterController;

    @Mock
    private RateLimiterService rateLimiterService;

    public RateLimiterControllerTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCheckRateLimitAllowed() {
        // Given
        String userId = "testUser";
        RateLimiterResponse response = new RateLimiterResponse(true, Instant.now());
        when(rateLimiterService.checkRateLimit(userId)).thenReturn(response);

        // When
        ResponseEntity<?> result = rateLimiterController.checkRateLimit(userId);

        // Then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new ApiResponse("Request allowed"), result.getBody());
    }

    @Test
    public void testCheckRateLimitNotAllowed() {
        // Given
        String userId = "testUser";
        Instant retryAfter = Instant.now().plus(Duration.ofMinutes(1)); // Simulate rate limiting
        RateLimiterResponse response = new RateLimiterResponse(false, retryAfter);
        when(rateLimiterService.checkRateLimit(userId)).thenReturn(response);

        Instant docExpirationTime = retryAfter.plusSeconds(60);
        Instant currTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant();
        long retryAfterSeconds = Duration.between(currTime, docExpirationTime).toSeconds();

        // When
        ResponseEntity<?> result = rateLimiterController.checkRateLimit(userId);

        // Then
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, result.getStatusCode());
        assertEquals("Too many requests please try again", ((ApiResponse) result.getBody()).message());
        HttpHeaders headers = result.getHeaders();
        assertEquals(String.valueOf(retryAfterSeconds), headers.getFirst("retry-after"));
    }
}
