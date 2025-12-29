package com.innowise.paymentservice.client;

import com.innowise.paymentservice.dto.models.OrderDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceClient.class);
    
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${order-service.url:http://localhost:8084}")
    private String orderServiceUrl;

    @Value("${internal.service.token:internal-service-secret}")
    private String internalToken;

    public OrderDto getOrderById(Long orderId) {
        String url = orderServiceUrl + "/api/v1/orders/internal/" + orderId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", internalToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        return restTemplate.exchange(url, HttpMethod.GET, entity, OrderDto.class).getBody();
    }
}
