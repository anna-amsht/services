package com.innowise.paymentservice.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class PaymentIdGenerator {
    
    private final AtomicLong counter = new AtomicLong(System.currentTimeMillis());
    
    public Long generateId() {
        return counter.incrementAndGet();
    }
}
