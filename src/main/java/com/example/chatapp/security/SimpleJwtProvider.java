package com.example.chatapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class SimpleJwtProvider {

    private final SecretKey key;
    private final long tokenValidityInMilliseconds;

    public SimpleJwtProvider(
            @Value("${jwt.secret:chatappsecretkey12345678901234567890}") String secret,
            @Value("${jwt.token-validity-in-seconds:86400}") long tokenValidityInSeconds) {
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
    }

    // 사용자 ID와 이름으로 토큰 생성
    public String createToken(Long userId, String username) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .signWith(key, SignatureAlgorithm.HS512)
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .compact();
    }

    // 토큰의 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.info("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // 토큰에서 사용자명 추출
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 토큰에서 사용자 ID 추출
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    // 토큰에서 만료 날짜 추출
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 토큰에서 클레임 추출
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // 토큰에서 모든 클레임 추출
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
