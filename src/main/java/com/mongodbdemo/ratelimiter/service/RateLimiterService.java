package com.mongodbdemo.ratelimiter.service;

import com.mongodbdemo.ratelimiter.dto.RateLimiterResponse;
import com.mongodbdemo.ratelimiter.entity.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service class for managing rate limiting functionality.
 * Provides methods to check and enforce rate limits based on user ID.
 */
@Service
public class RateLimiterService {

    private final MongoTemplate mongoTemplate;

    @Value("${rate.limiter.threshold}")
    private int threshold;

    @Value("${rate.limiter.expiration.minutes}")
    private int expirationMinutes;

    public RateLimiterService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public RateLimiterResponse checkRateLimit(String userId) {
        int maxRetries = 3;
        return tryRateLimitCheck(userId, maxRetries);
    }

    /**
     * Attempts to check and update the rate limit counter in MongoDB.
     * This is wrapped in retry logic for better concurrency handling.
     *
     * @param query the query to find the rate limiter document
     * @param update the update to apply to the rate limiter document
     * @return a {@code RateLimiterResponse} indicating whether the operation succeeded or rate limit is exceeded
     */
    RateLimiterResponse attemptRateLimitIncrement(Query query, Update update) {
        try {
            RateLimiter rateLimiter = mongoTemplate.findAndModify(
                    query,
                    update,
                    new FindAndModifyOptions().returnNew(true).upsert(true),
                    RateLimiter.class
            );
            if (rateLimiter == null) {
                throw new IllegalStateException("Failed to insert or update rate limiter document.");
            }
            return new RateLimiterResponse(true, null);

        } catch (DuplicateKeyException e) {
            Object userId = query.getQueryObject().get("userId");
            // Handle DuplicateKeyException based on the presence of the document
            // If document exists, return failure response
            RateLimiter existingRateLimiter = mongoTemplate.findById(userId, RateLimiter.class);
            if (existingRateLimiter != null) {
                return new RateLimiterResponse(false, existingRateLimiter.getExpirationTime());
            }
            throw e; // Retry otherwise
        } catch (OptimisticLockingFailureException e) {
            // Propagate exception to be handled in the retry loop
            throw e;
        }
    }

    /**
     * Handles the retry logic with exponential backoff when encountering duplicate key or optimistic locking exceptions.
     *
     * @param userId the user ID for whom the rate limit is being checked
     * @param maxRetries the maximum number of retries allowed
     * @return a {@code RateLimiterResponse} indicating success or failure
     */
    private RateLimiterResponse tryRateLimitCheck(String userId, int maxRetries) {
        Instant expirationTime = calculateExpirationTime();
        Query query = buildQuery(userId);
        Update update = buildUpdate(userId, expirationTime);

        int retryCount = 0;
        int backoff = 100; // Initial backoff in milliseconds

        while (true) {
            try {
                return attemptRateLimitIncrement(query, update);
            } catch (DuplicateKeyException | OptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    return new RateLimiterResponse(false, Instant.now().plusSeconds(expirationMinutes * 60L));
                }
                // Backoff before retrying
                sleepWithBackoff(backoff);
                backoff *= 2;  // Exponential backoff
            }
        }
    }

    /**
     * Introduces a delay with the specified backoff duration.
     *
     * @param backoff the backoff duration in milliseconds
     */
    void sleepWithBackoff(int backoff) {
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();  // Restore interrupted status
        }
    }

    private Query buildQuery(String userId) {
        return new Query(Criteria.where("userId").is(userId)
                .andOperator(Criteria.where("counter").lt(threshold)));
    }

    private Update buildUpdate(String userId, Instant expirationTime) {
        return new Update()
                .inc("counter", 1)
                .setOnInsert("userId", userId)
                .setOnInsert("expirationTime", expirationTime);
    }

    private Instant calculateExpirationTime() {
        Instant now = Instant.now();
        return now.plusSeconds(expirationMinutes * 60L - 60L);
    }
}
