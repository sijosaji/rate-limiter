package com.mongodbdemo.ratelimiter.dto;

import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Getter
public class RateLimiterResponse {

    private final boolean allowed;
    private final Instant retryAfter;

    public RateLimiterResponse(boolean allowed, Instant retryAfter) {
        this.allowed = allowed;
        this.retryAfter = retryAfter;
    }

}

