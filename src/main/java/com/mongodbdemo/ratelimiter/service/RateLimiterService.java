package com.mongodbdemo.ratelimiter.service;

import com.mongodbdemo.ratelimiter.dto.RateLimiterResponse;
import com.mongodbdemo.ratelimiter.entity.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
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

    /**
     * Constructs a {@code RateLimiterService} with the given {@code MongoTemplate}.
     *
     * @param mongoTemplate the MongoDB template used to interact with the database
     */
    public RateLimiterService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Checks the rate limit for a given user ID and returns a response indicating whether the rate limit is exceeded.
     * If the rate limit is exceeded, it returns a {@code RateLimiterResponse} with the expiration time of the rate limit.
     * Otherwise, it allows the request and increments the rate limit counter.
     *
     * @param userId the ID of the user for whom the rate limit is being checked
     * @return a {@code RateLimiterResponse} indicating whether the request is allowed or rate limit is exceeded,
     *         and the expiration time if the limit is exceeded
     */
    public RateLimiterResponse checkRateLimit(String userId) {
        Instant now = Instant.now();
        // Deduct 1 minute to account for MongoDB job delay in deleting expired documents
        Instant expirationTime = now.plusSeconds(expirationMinutes * 60L - 60L);

        Query query = new Query(Criteria.where("userId").is(userId));

        // Attempt to increment the counter if it's below the threshold, atomically
        Update update = new Update()
                .inc("counter", 1)
                .setOnInsert("userId", userId)
                .setOnInsert("expirationTime", expirationTime);
        RateLimiter rateLimiter = null;
        try {
            mongoTemplate.findAndModify(
                    query.addCriteria(Criteria.where("counter").lt(threshold)),
                    update,
                    new FindAndModifyOptions().returnNew(true).upsert(true),
                    RateLimiter.class
            );

        } catch (DuplicateKeyException ex) {
            rateLimiter = mongoTemplate.findById(userId, RateLimiter.class);
            if (rateLimiter != null) {
                return new RateLimiterResponse(false, rateLimiter.getExpirationTime());
            }
            return new RateLimiterResponse(true, null);
        }
        return new RateLimiterResponse(true, null);
    }
}
