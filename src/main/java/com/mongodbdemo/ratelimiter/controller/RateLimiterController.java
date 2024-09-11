package com.mongodbdemo.ratelimiter.controller;

import com.mongodbdemo.ratelimiter.dto.ApiResponse;
import com.mongodbdemo.ratelimiter.dto.RateLimiterResponse;
import com.mongodbdemo.ratelimiter.service.RateLimiterService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.mongodbdemo.ratelimiter.RateLimiterConstants.BASE_PATH;
import static com.mongodbdemo.ratelimiter.RateLimiterConstants.USER_ID_PATH;

@RestController
@RequestMapping(BASE_PATH)
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @PutMapping(USER_ID_PATH)
    public ResponseEntity<?> checkRateLimit(@PathVariable("userId") String userId) {
        RateLimiterResponse response = rateLimiterService.checkRateLimit(userId);

        if (response.isAllowed()) {
            return ResponseEntity.ok(new ApiResponse("Request allowed"));
        } else {
            HttpHeaders httpHeaders = new HttpHeaders();
            Instant docExpirationTime = response.getRetryAfter().plusSeconds(60);
            Instant currTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant();
            httpHeaders.add("retry-after", String.valueOf(Duration.between(currTime,
                    docExpirationTime).toSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(httpHeaders)
                    .body(new ApiResponse("Too many requests please try again"));
        }
    }
}

