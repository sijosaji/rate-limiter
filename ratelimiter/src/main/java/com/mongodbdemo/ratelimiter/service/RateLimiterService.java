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
        Instant now = Instant.now();
        // Need to deduct 1 minute to factor in time taken for mongodb job to delete expired documents.
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


