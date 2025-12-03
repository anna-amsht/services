package com.innowise.orderservice.service.implementation;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.models.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @Mock
    private UserClient userClient;

    @InjectMocks
    private UserServiceClient userServiceClient;

    private UserDto expectedUserDto;

    @BeforeEach
    void setUp() {
        expectedUserDto = UserDto.builder()
                .id(100L)
                .name("Test")
                .surname("User")
                .email("test@example.com")
                .birthdate(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void testGetUserById_Success() {
        when(userClient.getUserById(100L)).thenReturn(expectedUserDto);

        UserDto result = userServiceClient.getUserById(100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Test", result.getName());
        assertEquals("User", result.getSurname());
        verify(userClient).getUserById(100L);
    }

    @Test
    void testGetUserById_WhenClientThrowsException() {
        when(userClient.getUserById(100L)).thenThrow(new RuntimeException("Connection failed"));

        assertThrows(RuntimeException.class, () -> userServiceClient.getUserById(100L));
        verify(userClient).getUserById(100L);
    }
}
