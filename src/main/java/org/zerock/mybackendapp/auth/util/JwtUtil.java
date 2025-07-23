package org.zerock.mybackendapp.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(@Value("${jwt.secret:myVerySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong}") String secret,
                   @Value("${jwt.access-token-expiration:3600000}") long accessTokenExpiration,
                   @Value("${jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;

        log.info("=== JWT 유틸리티 초기화 ===");
        log.info("액세스 토큰 만료기간: {}ms {}분", accessTokenExpiration, accessTokenExpiration / 60000);
        log.info("리프레시 토큰 만료시간: {}ms ({}시간)", refreshTokenExpiration, refreshTokenExpiration / 3600000);
    }

    public String generateAccessToken(String username, Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        log.info("=== 액세스 토큰 생성 시작 ===");
        log.info("사용자: username={}, userId={}", username, userId);
        log.info("발급시간: {}", now);
        log.info("만료시간: {}", expiry);

        String token = Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();

        log.info("액세스 토큰 생성 완료: 길이={}", token.length());
        return token;
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        log.info("=== 리프레시 토큰 생성 시작 ===");
        log.info("사용자: username={}", username);
        log.info("발급시간: {}", now);
        log.info("만료시간: {}", expiry);

        String token = Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();

        log.info("리프레시 토큰 생성 완료: 길이={}", token.length());
        return token;
    }

    public String extractUsername(String token) {
        log.info("=== 토큰에서 사용자명 추출 시작 ===");
        try {
            Claims claims = extractAllClaims(token);
            String username = claims.getSubject();
            log.info("사용자명 추출 성공: username={}", username);
            return username;
        } catch (Exception e) {
            log.error("사용자명 추출 실패: {}", e.getMessage());
            throw e;
        }
    }

    public Long extractUserId(String token) {
        log.info("=== 토큰에서 사용자 ID 추출 시작 ===");
        try {
            Claims claims = extractAllClaims(token);
            Long userId = claims.get("userId", Long.class);
            log.info("사용자 ID 추출 성공: userId={}", userId);
            return userId;
        } catch (Exception e) {
            log.error("사용자 ID 추출 실패: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isTokenValid(String token) {
        log.info("=== 토큰 유효성 검증 시작 ===");
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();

            boolean isValid = expiration.after(now);
            log.info("토큰 유효성 검증 결과: valid={}, 만료시간={}, 현재시간={}",
                    isValid, expiration, now);

            return isValid;
        } catch (ExpiredJwtException e) {
            log.warn("토큰 만료: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("토큰 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        log.info("=== 토큰 타입 확인 시작 ===");
        try {
            Claims claims = extractAllClaims(token);
            String type = claims.get("type", String.class);
            boolean isAccess = "access".equals(type);
            log.info("토큰 타입 확인: type={}, isAccessToken={}", type, isAccess);
            return isAccess;
        } catch (Exception e) {
            log.error("토큰 타입 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        log.info("토큰 파싱 시작: 토큰 길이={}", token.length());
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.info("토큰 파싱 성공: subject={}, issuedAt={}, expiration={}",
                    claims.getSubject(), claims.getIssuedAt(), claims.getExpiration());
            return claims;
        } catch (Exception e) {
            log.error("토큰 파싱 실패: {}", e.getMessage());
            throw e;
        }
    }
}
