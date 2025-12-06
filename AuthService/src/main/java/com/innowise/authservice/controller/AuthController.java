package com.innowise.authservice.controller;

import com.innowise.authservice.dao.interfaces.RefreshTokenDao;
import com.innowise.authservice.dao.interfaces.RoleDao;
import com.innowise.authservice.dao.interfaces.UserDao;
import com.innowise.authservice.dto.JwtResponseDto;
import com.innowise.authservice.dto.LoginRequestDto;
import com.innowise.authservice.dto.RegisterRequestDto;
import com.innowise.authservice.dto.TokenRefreshRequestDto;
import com.innowise.authservice.entities.RefreshTokenEntity;
import com.innowise.authservice.entities.RoleEntity;
import com.innowise.authservice.entities.UserEntity;
import com.innowise.authservice.exceptions.InvalidCredentialsException;
import com.innowise.authservice.exceptions.TokenRefreshException;
import com.innowise.authservice.exceptions.UserAlreadyExistsException;
import com.innowise.authservice.exceptions.UserNotFoundException;
import com.innowise.authservice.exceptions.AdminRoleAssignmentException;
import com.innowise.authservice.service.UserDetailsServiceImpl;
import com.innowise.authservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Value("${internal.token:internal-secret-token}")
    private String internalToken;

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserDao userDao;
    private final RefreshTokenDao refreshTokenDao;
    private final RoleDao roleDao;


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDto registerRequest) {
        try {
            userDetailsService.loadUserByUsername(registerRequest.getEmail());
            throw new UserAlreadyExistsException("User with email " + registerRequest.getEmail() + " already exists");
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            logger.info("User with email {} does not exist, proceeding with registration", registerRequest.getEmail());
        }

        UserEntity user = new UserEntity();

        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        RoleEntity userRole = roleDao.getByName("user")
                .orElseThrow(() -> new IllegalStateException("Default user role not found in database"));

        if (user.getRoles().stream().noneMatch(r -> r.getId().equals(userRole.getId()))) {
            user.getRoles().add(userRole);
        }

        userDao.create(user);
        logger.info("User created in AuthService with ID: {}", user.getId());

        // Generate JWT token
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setExpiryDate(LocalDateTime.now().plusHours(24)); // 24 hours expiry
        refreshTokenDao.save(refreshTokenEntity);

        JwtResponseDto response = new JwtResponseDto(
                accessToken,
                refreshToken,
                "Bearer",
                user.getId(),
                user.getEmail()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
            
            UserEntity userEntity = userDao.getByEmailWithRoles(loginRequest.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginRequest.getEmail()));
            
            String accessToken = jwtUtil.generateAccessToken(userEntity);
            String refreshToken = jwtUtil.generateRefreshToken(userEntity);
            
            RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
            refreshTokenEntity.setUser(userEntity);
            refreshTokenEntity.setToken(refreshToken);
            refreshTokenEntity.setExpiryDate(LocalDateTime.now().plusHours(24)); // 24 hours expiry
            refreshTokenDao.save(refreshTokenEntity);
            
            JwtResponseDto response = new JwtResponseDto(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    userEntity.getId(),
                    userEntity.getEmail()
            );
            
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }


    @PostMapping("/admin/assign-admin/{userId}")
    public ResponseEntity<?> assignAdminRole(@PathVariable Long userId) {
        if (userId == null) {
            throw new AdminRoleAssignmentException("User ID is required");
        }
        
        UserEntity user = userDao.getById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        RoleEntity adminRole = roleDao.getByName("admin")
                .orElseThrow(() -> new AdminRoleAssignmentException("Admin role not found"));
        
        if (user.getRoles().stream().anyMatch(role -> role.getId().equals(adminRole.getId()))) {
            throw new AdminRoleAssignmentException("User already has admin role");
        }
        
        user.getRoles().add(adminRole);
        
        boolean updated = userDao.update(user);
        if (!updated) {
            throw new AdminRoleAssignmentException("Failed to update user roles");
        }
        
        return ResponseEntity.ok("Admin role assigned to user successfully!");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequestDto request) {
        String requestRefreshToken = request.getRefreshToken();
        
        return refreshTokenDao.getByToken(requestRefreshToken)
                .map(token -> {
                    if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
                        refreshTokenDao.delete(token);
                        throw new TokenRefreshException("Refresh token was expired. Please make a new signin request");
                    }
                    
                    String newAccessToken = jwtUtil.generateAccessToken(token.getUser());
                    
                    Map<String, String> response = new HashMap<>();
                    response.put("accessToken", newAccessToken);
                    response.put("refreshToken", requestRefreshToken);
                    response.put("tokenType", "Bearer");
                    
                    return ResponseEntity.ok(response);
                })
                .orElseThrow(() -> new TokenRefreshException("Refresh token is not in database"));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String token = jwtUtil.getJwtFromHeader(request);
        
        if (token == null || token.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", "No token provided");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            if (jwtUtil.validateJwtToken(token)) {
                String username = jwtUtil.getUserNameFromJwtToken(token);
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("username", username);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("error", "Invalid token");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", "Token validation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        logger.warn("Rollback initiated: deleting user credentials for userId: {}", userId);
        
        userDao.getById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        refreshTokenDao.deleteByUserId(userId);
        
        userDao.delete(userId);
        
        logger.info("Successfully deleted user credentials for userId: {}", userId);
        return ResponseEntity.ok("User credentials deleted successfully");
    }
}