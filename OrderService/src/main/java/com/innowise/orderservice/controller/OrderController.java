package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.models.OrderDto;
import com.innowise.orderservice.exceptions.NotFoundException;
import com.innowise.orderservice.service.interfaces.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> create(@Valid @RequestBody OrderDto orderDto) {
        logger.info("Creating new order for userId: {}", orderDto.getUserId());
        OrderDto createdOrder = orderService.create(orderDto);
        logger.info("Successfully created order with ID: {}", createdOrder.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getById(@PathVariable Long id) {
        logger.debug("Getting order by id: {}", id);
        Optional<OrderDto> order = orderService.getById(id);
        return order.map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
    }

    @GetMapping(params = "ids")
    public ResponseEntity<List<OrderDto>> getByIds(@RequestParam List<Long> ids) {
        logger.debug("Getting orders by ids: {}", ids);
        List<OrderDto> orders = orderService.getByIds(ids);
        return ResponseEntity.ok(orders);
    }

    @GetMapping(params = "statuses")
    public ResponseEntity<List<OrderDto>> getByStatuses(@RequestParam List<String> statuses) {
        logger.debug("Getting orders by statuses: {}", statuses);
        List<OrderDto> orders = orderService.getByStatuses(statuses);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> update(@PathVariable Long id, @Valid @RequestBody OrderDto orderDto) {
        logger.info("Updating order with id: {}", id);
        OrderDto updatedOrder = orderService.update(id, orderDto);
        logger.info("Successfully updated order with ID: {}", id);
        return ResponseEntity.ok(updatedOrder);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        logger.info("Deleting order with id: {}", id);
        orderService.delete(id);
        logger.info("Successfully deleted order with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}

