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

/**
 * Controller class for handling rate limiting requests.
 * Provides endpoints to check and enforce rate limits based on user ID.
 */
@RestController
@RequestMapping(BASE_PATH)
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    /**
     * Constructs a {@code RateLimiterController} with the given {@code RateLimiterService}.
     *
     * @param rateLimiterService the service used to check and manage rate limits
     */
    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Checks the rate limit for a given user ID and returns an appropriate response.
     * If the request is allowed, it returns a 200 OK status with a message indicating the request is allowed.
     * If the request is not allowed, it returns a 429 Too Many Requests status with a retry-after header and a message indicating
     * that the rate limit has been exceeded.
     *
     * @param userId the ID of the user for whom the rate limit is being checked
     * @return a {@code ResponseEntity} containing the status and message of the rate limit check
     */
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
