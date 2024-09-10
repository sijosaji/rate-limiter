package com.mongodbdemo.ratelimiter.repository;

import com.mongodbdemo.ratelimiter.entity.RateLimiter;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RateLimiterRepository extends MongoRepository<RateLimiter, String> {
    RateLimiter findByUserId(String userId);
}

