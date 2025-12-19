package com.innowise.orderservice.service.implementation;

import com.innowise.orderservice.dao.interfaces.OrderDao;
import com.innowise.orderservice.dto.mappers.OrderItemMapper;
import com.innowise.orderservice.dto.mappers.OrderMapper;
import com.innowise.orderservice.dto.models.CreateOrderEventDto;
import com.innowise.orderservice.dto.models.OrderDto;
import com.innowise.orderservice.dto.models.OrderWithUserDto;
import com.innowise.orderservice.dto.models.UserDto;
import com.innowise.orderservice.entities.ItemEntity;
import com.innowise.orderservice.entities.OrderEntity;
import com.innowise.orderservice.entities.OrderItemEntity;
import com.innowise.orderservice.exceptions.BadRequestException;
import com.innowise.orderservice.exceptions.NotFoundException;
import com.innowise.orderservice.kafka.OrderEventProducer;
import com.innowise.orderservice.service.interfaces.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Validated
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderDao orderDao;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final EntityManager entityManager;
    private final UserServiceClient userServiceClient;
    private final OrderEventProducer orderEventProducer;

    @Override
    public OrderWithUserDto create(OrderDto orderDto) {
        logger.info("Creating order for userId: {}", orderDto.getUserId());
        
        OrderEntity orderEntity = orderMapper.toEntity(orderDto);
        orderEntity.setCreationDate(LocalDateTime.now());
        
        if (orderDto.getOrderItems() != null && !orderDto.getOrderItems().isEmpty()) {
            List<OrderItemEntity> orderItemEntities = orderDto.getOrderItems().stream()
                    .map(orderItemDto -> {
                        OrderItemEntity orderItemEntity = orderItemMapper.toEntity(orderItemDto);
                        
                        ItemEntity itemEntity = entityManager.find(ItemEntity.class, orderItemDto.getItemId());
                        if (itemEntity == null) {
                            throw new BadRequestException("Item with id " + orderItemDto.getItemId() + " not found");
                        }
                        
                        orderItemEntity.setItem(itemEntity);
                        orderItemEntity.setOrder(orderEntity);
                        return orderItemEntity;
                    })
                    .collect(Collectors.toList());
            
            orderEntity.setOrderItems(orderItemEntities);
        }
        
        orderDao.create(orderEntity);
        logger.info("Successfully created order with ID: {}", orderEntity.getId());
        
        CreateOrderEventDto event = new CreateOrderEventDto(
                orderEntity.getId(),
                orderEntity.getUserId(),
                orderEntity.getStatus(),
                orderEntity.getCreationDate()
        );
        orderEventProducer.sendCreateOrderEvent(event);
        
        OrderDto createdOrderDto = orderMapper.toDto(orderEntity);
        UserDto userDto = userServiceClient.getUserById(orderDto.getUserId());
        
        return OrderWithUserDto.builder()
                .order(createdOrderDto)
                .user(userDto)
                .build();
    }

    @Override
    public Optional<OrderWithUserDto> getById(Long id) {
        logger.debug("Getting order by id: {}", id);
        return orderDao.getById(id)
                .map(orderEntity -> {
                    OrderDto orderDto = orderMapper.toDto(orderEntity);
                    UserDto userDto = userServiceClient.getUserById(orderEntity.getUserId());
                    return OrderWithUserDto.builder()
                            .order(orderDto)
                            .user(userDto)
                            .build();
                });
    }

    @Override
    public List<OrderWithUserDto> getByIds(List<Long> ids) {
        logger.debug("Getting orders by ids: {}", ids);
        return orderDao.getByIds(ids).stream()
                .map(orderEntity -> {
                    OrderDto orderDto = orderMapper.toDto(orderEntity);
                    UserDto userDto = userServiceClient.getUserById(orderEntity.getUserId());
                    return OrderWithUserDto.builder()
                            .order(orderDto)
                            .user(userDto)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderWithUserDto> getByStatuses(List<String> statuses) {
        logger.debug("Getting orders by statuses: {}", statuses);
        return orderDao.getByStatuses(statuses).stream()
                .map(orderEntity -> {
                    OrderDto orderDto = orderMapper.toDto(orderEntity);
                    UserDto userDto = userServiceClient.getUserById(orderEntity.getUserId());
                    return OrderWithUserDto.builder()
                            .order(orderDto)
                            .user(userDto)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public OrderWithUserDto update(Long id, OrderDto updatedOrderDto) {
        logger.info("Updating order with id: {}", id);
        
        OrderEntity existingOrder = orderDao.getById(id)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + id));
        
        OrderEntity updatedOrderEntity = orderMapper.toEntity(updatedOrderDto);
        
        existingOrder.setStatus(updatedOrderEntity.getStatus());
        
        existingOrder.getOrderItems().clear();
        
        if (updatedOrderDto.getOrderItems() != null && !updatedOrderDto.getOrderItems().isEmpty()) {
            List<OrderItemEntity> orderItemEntities = updatedOrderDto.getOrderItems().stream()
                    .map(orderItemDto -> {
                        OrderItemEntity orderItemEntity = orderItemMapper.toEntity(orderItemDto);
                        
                        ItemEntity itemEntity = entityManager.find(ItemEntity.class, orderItemDto.getItemId());
                        if (itemEntity == null) {
                            throw new BadRequestException("Item with id " + orderItemDto.getItemId() + " not found");
                        }
                        
                        orderItemEntity.setItem(itemEntity);
                        orderItemEntity.setOrder(existingOrder);
                        return orderItemEntity;
                    })
                    .collect(Collectors.toList());
            
            existingOrder.getOrderItems().addAll(orderItemEntities);
        }
        
        orderDao.update(id, existingOrder);
        logger.info("Successfully updated order with ID: {}", id);
        
        OrderDto orderDto = orderMapper.toDto(existingOrder);
        UserDto userDto = userServiceClient.getUserById(existingOrder.getUserId());
        
        return OrderWithUserDto.builder()
                .order(orderDto)
                .user(userDto)
                .build();
    }

    @Override
    public void updateOrderStatus(Long orderId, String status) {
        logger.info("Updating order status for orderId: {} to status: {}", orderId, status);
        
        OrderEntity orderEntity = orderDao.getById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));
        
        orderEntity.setStatus(status);
        orderDao.update(orderId, orderEntity);
        
        logger.info("Successfully updated order status for orderId: {}", orderId);
    }

    @Override
    public void delete(Long id) {
        logger.info("Deleting order with id: {}", id);
        if (orderDao.getById(id).isEmpty()) {
            throw new NotFoundException("Order not found with id: " + id);
        }
        orderDao.delete(id);
        logger.info("Successfully deleted order with ID: {}", id);
    }
}

