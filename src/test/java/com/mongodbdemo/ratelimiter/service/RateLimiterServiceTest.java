package com.mongodbdemo.ratelimiter.service;

import com.mongodbdemo.ratelimiter.dto.RateLimiterResponse;
import com.mongodbdemo.ratelimiter.entity.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RateLimiterServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCheckRateLimitSuccess() {
        String userId = "user123";
        RateLimiter rateLimiter = new RateLimiter(userId, 0, Instant.now().plusSeconds(60), 1L);

        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(RateLimiter.class))
        ).thenReturn(rateLimiter);

        RateLimiterResponse response = rateLimiterService.checkRateLimit(userId);

        assertTrue(response.isAllowed());
        assertNull(response.getRetryAfter());
    }

    @Test
    void testCheckRateLimitExceedsLimit() {
        String userId = "user123";
        RateLimiter rateLimiter = new RateLimiter(userId, 10, Instant.now().plusSeconds(60), 1L);

        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(RateLimiter.class))
        ).thenReturn(null);

        IllegalStateException thrownException = assertThrows(IllegalStateException.class, () -> {
            rateLimiterService.checkRateLimit(userId);
        });

        // Optionally, assert the exception message
        assertEquals("Failed to insert or update rate limiter document.", thrownException.getMessage());
    }

    @Test
    void testCheckRateLimitOptimisticLockingFailureException() {
        String userId = "user123";
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(RateLimiter.class))
        ).thenThrow(new OptimisticLockingFailureException("Optimistic locking failed"));

        RateLimiterResponse response = rateLimiterService.checkRateLimit(userId);

        assertFalse(response.isAllowed());
        assertNotNull(response.getRetryAfter());
    }

    @Test
    void testCheckRateLimitDuplicateKeyException() {
        String userId = "user123";
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(RateLimiter.class))
        ).thenThrow(new DuplicateKeyException("Duplicate key"));

        RateLimiterResponse response = rateLimiterService.checkRateLimit(userId);

        assertFalse(response.isAllowed());
        assertNotNull(response.getRetryAfter());
    }

    @Test
    void testRateLimitUpdateWithVersioning() {
        String userId = "user123";
        RateLimiter rateLimiter = new RateLimiter(userId, 0, Instant.now().plusSeconds(60), 1L);

        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(RateLimiter.class))
        ).thenReturn(rateLimiter);

        RateLimiterResponse response = rateLimiterService.checkRateLimit(userId);

        verify(mongoTemplate).findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(RateLimiter.class)
        );
    }

    @Test
    void testAttemptRateLimitIncrementSuccess() {
        String userId = "user123";
        Instant expirationTime = Instant.now().plusSeconds(60);
        RateLimiter rateLimiter = new RateLimiter(userId, 10, expirationTime, 1L);

        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update().inc("counter", 1).setOnInsert("userId", userId).setOnInsert("expirationTime", expirationTime);

        when(mongoTemplate.findAndModify(
                eq(query),
                eq(update),
                any(FindAndModifyOptions.class),
                eq(RateLimiter.class))
        ).thenReturn(rateLimiter);

        RateLimiterResponse response = rateLimiterService.attemptRateLimitIncrement(query, update);

        assertTrue(response.isAllowed());
        assertNull(response.getRetryAfter());
    }

    @Test
    void testAttemptRateLimitIncrementDuplicateKeyException_DocumentExists() {
        String userId = "user123";
        Instant expirationTime = Instant.now().plusSeconds(60);
        RateLimiter existingRateLimiter = new RateLimiter(userId, 10, expirationTime, 1L);

        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update().inc("counter", 1).setOnInsert("userId", userId).setOnInsert("expirationTime", expirationTime);

        when(mongoTemplate.findAndModify(
                eq(query),
                eq(update),
                any(FindAndModifyOptions.class),
                eq(RateLimiter.class))
        ).thenThrow(new DuplicateKeyException("Duplicate key error"));

        when(mongoTemplate.findById(eq(userId), eq(RateLimiter.class)))
                .thenReturn(existingRateLimiter);

        RateLimiterResponse response = rateLimiterService.attemptRateLimitIncrement(query, update);

        assertFalse(response.isAllowed());
        assertEquals(expirationTime, response.getRetryAfter());
    }

    @Test
    void testAttemptRateLimitIncrementDuplicateKeyException_NoDocument() {
        String userId = "user123";
        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update().inc("counter", 1).setOnInsert("userId", userId).setOnInsert("expirationTime", Instant.now().plusSeconds(60));

        when(mongoTemplate.findAndModify(
                eq(query),
                eq(update),
                any(FindAndModifyOptions.class),
                eq(RateLimiter.class))
        ).thenThrow(new DuplicateKeyException("Duplicate key error"));

        when(mongoTemplate.findById(eq(userId), eq(RateLimiter.class)))
                .thenReturn(null); // Simulate no document found

        // Ensure that the DuplicateKeyException is thrown so retry logic can handle it
        DuplicateKeyException thrownException = assertThrows(DuplicateKeyException.class, () -> {
            rateLimiterService.attemptRateLimitIncrement(query, update);
        });

    }

    @Test
    void testAttemptRateLimitIncrementOptimisticLockingFailureException() {
        String userId = "user123";
        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update().inc("counter", 1).setOnInsert("userId", userId).setOnInsert("expirationTime", Instant.now().plusSeconds(60));

        when(mongoTemplate.findAndModify(
                eq(query),
                eq(update),
                any(FindAndModifyOptions.class),
                eq(RateLimiter.class))
        ).thenThrow(new OptimisticLockingFailureException("Optimistic locking error"));

        // Ensure that the OptimisticLockingFailureException is thrown and handled by the retry logic
        OptimisticLockingFailureException thrownException = assertThrows(OptimisticLockingFailureException.class, () -> {
            rateLimiterService.attemptRateLimitIncrement(query, update);
        });

        assertEquals("Optimistic locking error", thrownException.getMessage());
    }

    @Test
    void testSleepWithBackoffSuccess() {
        // Testing the sleepWithBackoff method without interruption
        long startTime = System.currentTimeMillis();
        int backoff = 100; // 100 milliseconds

        rateLimiterService.sleepWithBackoff(backoff);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Allowing a small margin for delay
        assertTrue(duration >= backoff && duration < backoff + 50, "Sleep duration should be approximately equal to backoff time");
    }

    @Test
    void testSleepWithBackoffInterrupted() {
        // Testing the sleepWithBackoff method when interrupted
        int backoff = 100; // 100 milliseconds

        // Create a thread to interrupt the sleep
        Thread interruptingThread = new Thread(() -> {
            try {
                Thread.sleep(50); // Sleep briefly before interrupting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
            rateLimiterService.sleepWithBackoff(backoff);
        });

        interruptingThread.start();
        interruptingThread.interrupt(); // Interrupt the thread

        try {
            interruptingThread.join(); // Wait for the thread to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
        }

        // No specific assertion for this test as it verifies proper handling of interruption
        // Ensuring no exceptions are thrown is sufficient
    }
}
