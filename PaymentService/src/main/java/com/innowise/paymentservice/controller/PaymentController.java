package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.models.PaymentDto;
import com.innowise.paymentservice.service.interfaces.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDto> create(@Valid @RequestBody PaymentDto paymentDto) {
        log.info("Creating payment for orderId: {}, userId: {}", paymentDto.getOrderId(), paymentDto.getUserId());
        PaymentDto createdPayment = paymentService.create(paymentDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<List<PaymentDto>> getByOrderId(@PathVariable Long orderId) {
        log.debug("Getting payments by orderId: {}", orderId);
        List<PaymentDto> payments = paymentService.getByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<PaymentDto>> getByUserId(@PathVariable Long userId) {
        log.debug("Getting payments by userId: {}", userId);
        List<PaymentDto> payments = paymentService.getByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/by-statuses")
    public ResponseEntity<List<PaymentDto>> getByStatuses(@RequestParam List<String> statuses) {
        log.debug("Getting payments by statuses: {}", statuses);
        List<PaymentDto> payments = paymentService.getByStatuses(statuses);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/total-sum")
    public ResponseEntity<BigDecimal> getTotalSumByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting total payment sum between {} and {}", startDate, endDate);
        BigDecimal totalSum = paymentService.getTotalSumByPeriod(startDate, endDate);
        return ResponseEntity.ok(totalSum != null ? totalSum : BigDecimal.ZERO);
    }
}
