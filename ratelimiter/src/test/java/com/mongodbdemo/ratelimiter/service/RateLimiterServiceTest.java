package com.mongodbdemo.ratelimiter.service;

import com.mongodbdemo.ratelimiter.dto.RateLimiterResponse;
import com.mongodbdemo.ratelimiter.entity.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class RateLimiterServiceTest {

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @Mock
    private MongoTemplate mongoTemplate;


    private int threshold = 10; // Set a sample threshold for tests


    private int expirationMinutes = 1; // Set a sample expiration time for tests

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCheckRateLimitAllowsRequest() {
        // Given
        String userId = "testUser";
        Instant expirationTime = Instant.now().plusSeconds(60); // Set expiration time for 1 minute later

        // Mock MongoTemplate behavior
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(RateLimiter.class)))
                .thenReturn(new RateLimiter(userId, 1, expirationTime)); // Simulate success

        // When
        RateLimiterResponse response = rateLimiterService.checkRateLimit(userId);

        // Then
        assertEquals(true, response.isAllowed());
        assertEquals(null, response.getRetryAfter());
    }

    @Test
    public void testCheckRateLimitExceededThreshold() {
        // Given
        String userId = "testUser";
        Instant expirationTime = Instant.now().plusSeconds(60); // Set expiration time for 1 minute later

        // Mock MongoTemplate behavior
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(RateLimiter.class)))
                .thenThrow(new DuplicateKeyException("Duplicate Key"));

        when(mongoTemplate.findById(eq(userId), eq(RateLimiter.class)))
                .thenReturn(new RateLimiter(userId, threshold, expirationTime)); // Simulate threshold exceeded

        // When
        RateLimiterResponse response = rateLimiterService.checkRateLimit(userId);

        // Then
        assertEquals(false, response.isAllowed());
        assertEquals(expirationTime, response.getRetryAfter());
    }

    @Test
    public void testCheckRateLimitDuplicateKeyExceptionDocumentExists() {
        // Given
        String userId = "testUser";
        Instant expirationTime = Instant.now().plusSeconds(60); // Set expiration time for 1 minute later

        // Mock MongoTemplate behavior
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(RateLimiter.class)))
                .thenThrow(new DuplicateKeyException("Duplicate Key"));

        when(mongoTemplate.findById(eq(userId), eq(RateLimiter.class)))
                .thenReturn(new RateLimiter(userId, 1, expirationTime)); // Simulate existing document

        // When
        RateLimiterResponse response = rateLimiterService.checkRateLimit(userId);

        // Then
        assertEquals(false, response.isAllowed());
        assertEquals(expirationTime, response.getRetryAfter());
    }

    @Test
    public void testCheckRateLimitDuplicateKeyExceptionDocumentDoesNotExist() {
        // Given
        String userId = "testUser";

        // Mock MongoTemplate behavior
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(RateLimiter.class)))
                .thenThrow(new DuplicateKeyException("Duplicate Key"));

        when(mongoTemplate.findById(eq(userId), eq(RateLimiter.class)))
                .thenReturn(null); // Simulate non-existing document

        // When
        RateLimiterResponse response = rateLimiterService.checkRateLimit(userId);

        // Then
        assertEquals(true, response.isAllowed());
        assertEquals(null, response.getRetryAfter());
    }
}
