package com.innowise.paymentservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class RandomNumberClient {

    private final RestTemplate restTemplate;

    @Value("${random-number.url:https://www.randomnumberapi.com/api/v1.0/random?min=1&max=100&count=1}")
    private String randomNumberUrl;

    public Integer fetchRandomNumber() {
        try {
            Integer[] response = restTemplate.getForObject(randomNumberUrl, Integer[].class);
            if (response != null && response.length > 0) {
                log.debug("Random number API response: {}", response[0]);
                return response[0];
            }
            return null;
        } catch (RestClientException ex) {
            log.error("Failed to fetch random number from external API: {}", ex.getMessage());
            return null;
        }
    }
}


