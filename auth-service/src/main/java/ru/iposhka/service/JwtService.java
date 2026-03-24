package ru.iposhka.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.iposhka.exception.BadJwtException;

@Slf4j
@Service
public class JwtService {
    private final JwtParser jwtParser;
    private final SecretKey key;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access.expiration_minutes}") long accessExpirationMinutes,
            @Value("${jwt.refresh.expiration_minutes}") long refreshExpirationMinutes
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser()
                .verifyWith(key)
                .build();
        this.accessExpirationMillis = accessExpirationMinutes * 60 * 1000;
        this.refreshExpirationMillis = refreshExpirationMinutes * 60 * 1000;
    }

    public String generateAccessToken(Map<String, Object> claims) {
        return createToken(claims, accessExpirationMillis);
    }

    public String generateRefreshToken(Map<String, Object> claims) {
        return createToken(claims, refreshExpirationMillis);
    }

    public Claims validateRefreshToken(String refreshToken) {
        try {
            return jwtParser
                    .parseSignedClaims(refreshToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BadJwtException("Срок действия Refresh token истек");
        } catch (Exception e) {
            throw new BadJwtException("Некорректный refresh токен");
        }
    }

    private String createToken(Map<String, Object> claims, long expirationMillis) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiration = new Date(now + expirationMillis);

        Object userId = claims.get("user_id");

        return Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS384)
                .compact();
    }
}