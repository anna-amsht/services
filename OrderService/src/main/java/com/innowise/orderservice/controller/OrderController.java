package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.models.OrderDto;
import com.innowise.orderservice.dto.models.OrderWithUserDto;
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
    public ResponseEntity<OrderWithUserDto> create(@Valid @RequestBody OrderDto orderDto) {
        logger.info("Creating new order for userId: {}", orderDto.getUserId());
        OrderWithUserDto createdOrder = orderService.create(orderDto);
        logger.info("Successfully created order with ID: {}", createdOrder.getOrder().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> getById(@PathVariable Long id) {
        logger.debug("Getting order by id: {}", id);
        Optional<OrderWithUserDto> order = orderService.getById(id);
        return order.map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<OrderDto> getByIdInternal(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
        if (!"internal-service-secret".equals(internalToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        logger.debug("Getting order by id: {}", id);
        Optional<OrderDto> order = orderService.getOrderOnly(id);
        return order.map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
    }

    @GetMapping(params = "ids")
    public ResponseEntity<List<OrderWithUserDto>> getByIds(@RequestParam List<Long> ids) {
        logger.debug("Getting orders by ids: {}", ids);
        List<OrderWithUserDto> orders = orderService.getByIds(ids);
        return ResponseEntity.ok(orders);
    }

    @GetMapping(params = "statuses")
    public ResponseEntity<List<OrderWithUserDto>> getByStatuses(@RequestParam List<String> statuses) {
        logger.debug("Getting orders by statuses: {}", statuses);
        List<OrderWithUserDto> orders = orderService.getByStatuses(statuses);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderWithUserDto> update(@PathVariable Long id, @Valid @RequestBody OrderDto orderDto) {
        logger.info("Updating order with id: {}", id);
        OrderWithUserDto updatedOrder = orderService.update(id, orderDto);
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

