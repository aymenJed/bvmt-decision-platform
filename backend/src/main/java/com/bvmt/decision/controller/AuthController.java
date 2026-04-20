package com.bvmt.decision.controller;

import com.bvmt.decision.dto.AuthRequestDto;
import com.bvmt.decision.dto.AuthResponseDto;
import com.bvmt.decision.dto.RegisterRequestDto;
import com.bvmt.decision.entity.AppRole;
import com.bvmt.decision.entity.AppUser;
import com.bvmt.decision.repository.AppRoleRepository;
import com.bvmt.decision.repository.AppUserRepository;
import com.bvmt.decision.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Login / register / refresh JWT")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService            jwtService;
    private final UserDetailsService    userDetailsService;
    private final AppUserRepository     userRepo;
    private final AppRoleRepository     roleRepo;
    private final PasswordEncoder       passwordEncoder;

    @Operation(summary = "Connexion : retourne access + refresh token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        var userDetails = userDetailsService.loadUserByUsername(req.username());
        return ResponseEntity.ok(new AuthResponseDto(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails),
                req.username()));
    }

    @Operation(summary = "Création d'un compte utilisateur (rôle ROLE_USER par défaut)")
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto req) {
        if (userRepo.existsByUsername(req.username())) {
            return ResponseEntity.badRequest().build();
        }
        AppRole userRole = roleRepo.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER absent (seed Flyway ?)"));

        AppUser user = AppUser.builder()
                .username(req.username())
                .email(req.email())
                .fullName(req.fullName())
                .passwordHash(passwordEncoder.encode(req.password()))
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
        userRepo.save(user);

        var userDetails = userDetailsService.loadUserByUsername(req.username());
        return ResponseEntity.ok(new AuthResponseDto(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails),
                req.username()));
    }
}
