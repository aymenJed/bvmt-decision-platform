package com.bvmt.decision.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service JWT : génération, parsing et validation des tokens.
 *
 * Algorithme : HS256 (HMAC + SHA-256).
 * Le secret doit faire ≥ 256 bits — configuré via `bvmt.security.jwt.secret`.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${bvmt.security.jwt.secret}") String secret,
            @Value("${bvmt.security.jwt.expiration-ms:86400000}") long accessExpirationMs,
            @Value("${bvmt.security.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        // Supporte secret encodé base64 OU clair UTF-8 (dev)
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32) throw new IllegalArgumentException();
        } catch (Exception e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationMs  = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(UserDetails user) {
        return buildToken(user, accessExpirationMs, Map.of("type", "access"));
    }

    public String generateRefreshToken(UserDetails user) {
        return buildToken(user, refreshExpirationMs, Map.of("type", "refresh"));
    }

    private String buildToken(UserDetails user, long expirationMs, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("roles", user.getAuthorities().stream()
                .map(a -> a.getAuthority()).toList());
        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isValid(String token, UserDetails user) {
        String username = extractUsername(token);
        return username.equals(user.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
