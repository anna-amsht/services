package com.innowise.apigateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey key;
    private final JwtParser jwtParser;
    private final String issuer;

    public JwtUtil(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
        this.issuer = issuer;
    }

    public String extractUsername(String token) {
        return jwtParser.parseClaimsJws(token).getBody().getSubject();
    }

    public String getUserNameFromJwtToken(String token) {
        return extractUsername(token);
    }

    public Long getUserIdFromJwtToken(String token) {
        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        return claims.get("userId", Long.class);
    }

    public Claims extractClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = extractClaims(token);
        return claims.get("roles", List.class);
    }

    public boolean validateToken(String authToken) {
        try {
            logger.debug("Validating JWT token");
            Jws<Claims> claims = jwtParser.parseClaimsJws(authToken);

            if (!issuer.equals(claims.getBody().getIssuer())) {
                logger.error("Invalid JWT issuer: expected '{}', but got '{}'", issuer, claims.getBody().getIssuer());
                return false;
            }

            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        }
        return false;
    }

    public boolean validateJwtToken(String authToken) {
        return validateToken(authToken);
    }
}