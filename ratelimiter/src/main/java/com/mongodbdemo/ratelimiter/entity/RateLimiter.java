package com.mongodbdemo.ratelimiter.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;


@Data
@AllArgsConstructor
@Document(collection = "rateLimiter")
public class RateLimiter {

    @Id
    private String userId;

    private int counter;

    @Indexed(name = "expirationTime", expireAfterSeconds = 0) // TTL index to delete the document after expirationTime
    private Instant expirationTime;
}

