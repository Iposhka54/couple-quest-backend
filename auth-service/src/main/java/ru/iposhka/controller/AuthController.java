package ru.iposhka.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.iposhka.dto.request.ResendEmailCodeRequestDto;
import ru.iposhka.dto.request.UserSignInRequestDto;
import ru.iposhka.dto.request.UserSignUpRequestDto;
import ru.iposhka.dto.request.VerifyEmailCodeRequestDto;
import ru.iposhka.dto.response.AuthResponseDto;
import ru.iposhka.dto.response.JwtResponseDto;
import ru.iposhka.dto.response.UserResponseDto;
import ru.iposhka.model.AuthStatus;
import ru.iposhka.service.AuthService;
import ru.iposhka.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final long refreshExpirationSeconds;

    public AuthController(AuthService authService,
            UserService userService,
            @Value("${jwt.refresh.expiration_minutes}") long refreshExpirationMinutes) {
        this.authService = authService;
        this.userService = userService;
        refreshExpirationSeconds = refreshExpirationMinutes * 60;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> me(@RequestHeader("X-Auth-User-Id") Long userId) {
        return ResponseEntity.ok(userService.getInfoAboutUser(userId));
    }
    @PostMapping("/signUp")
    public ResponseEntity<AuthResponseDto> signUp(@Validated @RequestBody UserSignUpRequestDto userSignUpRequestDto) {
        AuthResponseDto authResponseDto = authService.signUp(userSignUpRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authResponseDto);
    }

    @PostMapping("/signIn")
    public ResponseEntity<AuthResponseDto> signIn(@Validated @RequestBody UserSignInRequestDto userSignInRequestDto,
            HttpServletResponse response) {
        AuthResponseDto authResponseDto = authService.signIn(userSignInRequestDto);
        if (authResponseDto.getStatus() == AuthStatus.AUTHENTICATED) {
            JwtResponseDto jwtResponseDto = authService.issueTokens(authResponseDto.getEmail());
            setRefreshTokenCookie(response, jwtResponseDto.getRefreshToken());
            authResponseDto.setAccessToken(jwtResponseDto.getAccessToken());
        }
        return ResponseEntity.ok(authResponseDto);
    }

    @PostMapping("/verify-email-code")
    public ResponseEntity<AuthResponseDto> verifyEmailCode(
            @Validated @RequestBody VerifyEmailCodeRequestDto verifyEmailCodeRequestDto,
            HttpServletResponse response) {
        AuthResponseDto authResponseDto = authService.verifyEmailCode(verifyEmailCodeRequestDto);
        JwtResponseDto jwtResponseDto = authService.issueTokens(authResponseDto.getEmail());
        setRefreshTokenCookie(response, jwtResponseDto.getRefreshToken());
        authResponseDto.setAccessToken(jwtResponseDto.getAccessToken());
        return ResponseEntity.ok(authResponseDto);
    }

    @PostMapping("/resend-email-code")
    public ResponseEntity<AuthResponseDto> resendEmailCode(
            @Validated @RequestBody ResendEmailCodeRequestDto resendEmailCodeRequestDto) {
        return ResponseEntity.ok(authService.resendEmailCode(resendEmailCodeRequestDto));
    }

    @PostMapping("/refresh")
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