package com.innowise.orderservice.service.implementation;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.models.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @Mock
    private UserClient userClient;

    private UserServiceClient userServiceClient;

    private UserDto expectedUserDto;

    @BeforeEach
    void setUp() {
        userServiceClient = new UserServiceClient(userClient);
        ReflectionTestUtils.setField(userServiceClient, "internalToken", "test-token");
        
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
        when(userClient.getUserById(eq(100L), anyString())).thenReturn(expectedUserDto);

        UserDto result = userServiceClient.getUserById(100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Test", result.getName());
        assertEquals("User", result.getSurname());
        verify(userClient).getUserById(eq(100L), anyString());
    }

    @Test
    void testGetUserById_ReturnsTokenWithClient() {
        when(userClient.getUserById(eq(100L), eq("test-token"))).thenReturn(expectedUserDto);

        UserDto result = userServiceClient.getUserById(100L);

        assertNotNull(result);
        verify(userClient).getUserById(eq(100L), eq("test-token"));
    }
}
