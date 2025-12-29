package com.innowise.orderservice.client;

import com.innowise.orderservice.dto.models.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserClient {

    @GetMapping("/api/v1/users/internal/{id}")
    UserDto getUserById(@PathVariable("id") Long id, @RequestHeader("X-Internal-Token") String internalToken);
}
