package ru.iposhka.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.iposhka.dto.request.UserSignInRequestDto;
import ru.iposhka.dto.request.UserSignUpRequestDto;
import ru.iposhka.dto.response.AuthResponseDto;
import ru.iposhka.dto.response.JwtResponseDto;
import ru.iposhka.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    @Value("${jwt.refresh.expiration_minutes}")
    private final long refreshExpirationSeconds;

    public AuthController(AuthService authService,
            @Value("${jwt.refresh.expiration_minutes}") long refreshExpirationMinutes) {
        this.authService = authService;
        refreshExpirationSeconds = refreshExpirationMinutes * 60;
    }

    @PostMapping("/signUp")
    public ResponseEntity<AuthResponseDto> signUp(@Validated @RequestBody UserSignUpRequestDto userSignUpRequestDto,
            HttpServletResponse response) {
        JwtResponseDto jwtResponseDto = authService.signUp(userSignUpRequestDto);

        setRefreshTokenCookie(response, jwtResponseDto.getRefreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponseDto(jwtResponseDto.getAccessToken()));
    }

    @PostMapping("/signIn")
    public ResponseEntity<AuthResponseDto> signIn(@Validated @RequestBody UserSignInRequestDto userSignInRequestDto,
            HttpServletResponse response) {
        JwtResponseDto jwtResponseDto = authService.signIn(userSignInRequestDto);

        setRefreshTokenCookie(response, jwtResponseDto.getRefreshToken());

        return ResponseEntity.ok(new AuthResponseDto(jwtResponseDto.getAccessToken()));
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<AuthResponseDto> refreshToken(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        AuthResponseDto authResponseDto = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(authResponseDto);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .path("/api/auth/refresh")
                .maxAge(refreshExpirationSeconds)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
}