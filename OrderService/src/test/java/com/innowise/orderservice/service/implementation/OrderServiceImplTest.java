package com.innowise.orderservice.service.implementation;

import com.innowise.orderservice.dao.interfaces.OrderDao;
import com.innowise.orderservice.dto.mappers.OrderItemMapper;
import com.innowise.orderservice.dto.mappers.OrderMapper;
import com.innowise.orderservice.dto.models.*;
import com.innowise.orderservice.entities.ItemEntity;
import com.innowise.orderservice.entities.OrderEntity;
import com.innowise.orderservice.entities.OrderItemEntity;
import com.innowise.orderservice.exceptions.BadRequestException;
import com.innowise.orderservice.exceptions.NotFoundException;
import com.innowise.orderservice.kafka.OrderEventProducer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private EntityManager entityManager;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderEntity orderEntity;
    private OrderDto orderDto;
    private ItemEntity itemEntity;
    private ItemDto itemDto;
    private OrderItemEntity orderItemEntity;
    private OrderItemDto orderItemDto;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setName("Test Item");
        itemEntity.setPrice(new BigDecimal("99.99"));

        itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setName("Test Item");
        itemDto.setPrice(new BigDecimal("99.99"));

        orderItemEntity = new OrderItemEntity();
        orderItemEntity.setId(1L);
        orderItemEntity.setItem(itemEntity);
        orderItemEntity.setQuantity(2);

        orderItemDto = new OrderItemDto();
        orderItemDto.setId(1L);
        orderItemDto.setItemId(1L);
        orderItemDto.setItem(itemDto);
        orderItemDto.setQuantity(2);
        orderItemDto.setPrice(new BigDecimal("99.99"));

        orderEntity = new OrderEntity();
        orderEntity.setId(1L);
        orderEntity.setUserId(100L);
        orderEntity.setStatus("PENDING");
        orderEntity.setCreationDate(LocalDateTime.now());
        orderEntity.setOrderItems(new ArrayList<>(List.of(orderItemEntity)));

        orderDto = new OrderDto();
        orderDto.setId(1L);
        orderDto.setUserId(100L);
        orderDto.setStatus("PENDING");
        orderDto.setCreationDate(LocalDateTime.now());
        orderDto.setOrderItems(new ArrayList<>(List.of(orderItemDto)));

        userDto = UserDto.builder()
                .id(100L)
                .name("Test")
                .surname("User")
                .email("test@example.com")
                .birthdate(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void testCreate() {
        when(orderMapper.toEntity(orderDto)).thenReturn(orderEntity);
        when(orderItemMapper.toEntity(any(OrderItemDto.class))).thenReturn(orderItemEntity);
        when(entityManager.find(ItemEntity.class, 1L)).thenReturn(itemEntity);
        when(orderMapper.toDto(any(OrderEntity.class))).thenReturn(orderDto);
        when(userServiceClient.getUserById(100L)).thenReturn(userDto);

        OrderWithUserDto result = orderService.create(orderDto);

        assertNotNull(result);
        assertNotNull(result.getOrder());
        assertNotNull(result.getUser());
        assertEquals(orderDto.getUserId(), result.getOrder().getUserId());
        assertEquals(userDto.getId(), result.getUser().getId());
        verify(orderDao).create(any(OrderEntity.class));
        verify(entityManager).find(ItemEntity.class, 1L);
        verify(userServiceClient).getUserById(100L);
        verify(orderEventProducer).sendCreateOrderEvent(any(CreateOrderEventDto.class));
    }

    @Test
    void testCreateWhenItemNotFound() {
        when(orderMapper.toEntity(orderDto)).thenReturn(orderEntity);
        when(orderItemMapper.toEntity(any(OrderItemDto.class))).thenReturn(orderItemEntity);
        when(entityManager.find(ItemEntity.class, 1L)).thenReturn(null);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.create(orderDto)
        );
        assertEquals("Item with id 1 not found", exception.getMessage());
        verify(orderDao, never()).create(any());
        verify(orderEventProducer, never()).sendCreateOrderEvent(any(CreateOrderEventDto.class));
    }

    @Test
    void testGetById() {
        when(orderDao.getById(1L)).thenReturn(Optional.of(orderEntity));
        when(orderMapper.toDto(orderEntity)).thenReturn(orderDto);
        when(userServiceClient.getUserById(100L)).thenReturn(userDto);

        Optional<OrderWithUserDto> result = orderService.getById(1L);

        assertTrue(result.isPresent());
        assertEquals(orderDto.getId(), result.get().getOrder().getId());
        assertEquals(userDto.getId(), result.get().getUser().getId());
        verify(userServiceClient).getUserById(100L);
    }

    @Test
    void testGetByIdNotFound() {
        when(orderDao.getById(999L)).thenReturn(Optional.empty());

        Optional<OrderWithUserDto> result = orderService.getById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetByIds() {
        List<Long> ids = List.of(1L, 2L);
        List<OrderEntity> entities = List.of(orderEntity);
        when(orderDao.getByIds(ids)).thenReturn(entities);
        when(orderMapper.toDto(any(OrderEntity.class))).thenReturn(orderDto);
        when(userServiceClient.getUserById(100L)).thenReturn(userDto);

        List<OrderWithUserDto> result = orderService.getByIds(ids);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getOrder());
        assertNotNull(result.get(0).getUser());
        verify(orderDao).getByIds(ids);
        verify(userServiceClient).getUserById(100L);
    }

    @Test
    void testGetByStatuses() {
        List<String> statuses = List.of("PENDING", "PROCESSING");
        List<OrderEntity> entities = List.of(orderEntity);
        when(orderDao.getByStatuses(statuses)).thenReturn(entities);
        when(orderMapper.toDto(any(OrderEntity.class))).thenReturn(orderDto);
        when(userServiceClient.getUserById(100L)).thenReturn(userDto);

        List<OrderWithUserDto> result = orderService.getByStatuses(statuses);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getOrder());
        assertNotNull(result.get(0).getUser());
        verify(orderDao).getByStatuses(statuses);
        verify(userServiceClient).getUserById(100L);
    }

    @Test
    void testUpdate() {
        OrderDto updatedDto = new OrderDto();
        updatedDto.setStatus("PROCESSING");
        updatedDto.setOrderItems(new ArrayList<>(List.of(orderItemDto)));

        OrderEntity updatedEntity = new OrderEntity();
        updatedEntity.setStatus("PROCESSING");

        when(orderDao.getById(1L)).thenReturn(Optional.of(orderEntity));
        when(orderMapper.toEntity(updatedDto)).thenReturn(updatedEntity);
        when(orderItemMapper.toEntity(any(OrderItemDto.class))).thenReturn(orderItemEntity);
        when(entityManager.find(ItemEntity.class, 1L)).thenReturn(itemEntity);
        when(orderMapper.toDto(any(OrderEntity.class))).thenReturn(orderDto);
        when(userServiceClient.getUserById(100L)).thenReturn(userDto);

        OrderWithUserDto result = orderService.update(1L, updatedDto);

        assertNotNull(result);
        assertNotNull(result.getOrder());
        assertNotNull(result.getUser());
        verify(orderDao).update(eq(1L), any(OrderEntity.class));
        verify(userServiceClient).getUserById(100L);
    }

    @Test
    void testUpdateWhenOrderNotFound() {
        when(orderDao.getById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> orderService.update(999L, orderDto)
        );
        assertEquals("Order not found with id: 999", exception.getMessage());
        verify(orderDao, never()).update(any(), any());
    }

    @Test
    void testUpdateWhenItemNotFound() {
        when(orderDao.getById(1L)).thenReturn(Optional.of(orderEntity));
        when(orderMapper.toEntity(orderDto)).thenReturn(orderEntity);
        when(orderItemMapper.toEntity(any(OrderItemDto.class))).thenReturn(orderItemEntity);
        when(entityManager.find(ItemEntity.class, 1L)).thenReturn(null);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.update(1L, orderDto)
        );
        assertEquals("Item with id 1 not found", exception.getMessage());
    }

    @Test
    void testDelete() {
        when(orderDao.getById(1L)).thenReturn(Optional.of(orderEntity));

        orderService.delete(1L);

        verify(orderDao).delete(1L);
    }

    @Test
    void testDeleteWhenOrderNotFound() {
        when(orderDao.getById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> orderService.delete(999L)
        );
        assertEquals("Order not found with id: 999", exception.getMessage());
        verify(orderDao, never()).delete(any());
    }

    @Test
    void testUpdateOrderStatus() {
        when(orderDao.getById(1L)).thenReturn(Optional.of(orderEntity));
        doAnswer(invocation -> {
            String status = invocation.getArgument(1);
            orderEntity.setStatus(status);
            return null;
        }).when(orderDao).updateStatus(eq(1L), anyString());

        orderService.updateOrderStatus(1L, "PAID");

        verify(orderDao).updateStatus(eq(1L), eq("PAID"));
        assertEquals("PAID", orderEntity.getStatus());
    }

    @Test
    void testUpdateOrderStatusWhenOrderNotFound() {
        when(orderDao.getById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> orderService.updateOrderStatus(999L, "PAID")
        );
        assertEquals("Order not found with id: 999", exception.getMessage());
        verify(orderDao, never()).updateStatus(any(), any());
    }

    @Test
    void testGetOrderOnly() {
        when(orderDao.getById(1L)).thenReturn(Optional.of(orderEntity));
        when(orderMapper.toDto(orderEntity)).thenReturn(orderDto);

        Optional<OrderDto> result = orderService.getOrderOnly(1L);

        assertTrue(result.isPresent());
        assertEquals(orderDto.getId(), result.get().getId());
        verify(orderDao).getById(1L);
        verify(orderMapper).toDto(orderEntity);
        verifyNoInteractions(userServiceClient);
    }

    @Test
    void testGetOrderOnlyNotFound() {
        when(orderDao.getById(999L)).thenReturn(Optional.empty());

        Optional<OrderDto> result = orderService.getOrderOnly(999L);

        assertTrue(result.isEmpty());
        verifyNoInteractions(userServiceClient);
    }
}




