package ru.iposhka.service;

import io.jsonwebtoken.Claims;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.iposhka.dto.request.UserSignInRequestDto;
import ru.iposhka.dto.request.UserSignUpRequestDto;
import ru.iposhka.dto.response.AuthResponseDto;
import ru.iposhka.dto.response.JwtResponseDto;
import ru.iposhka.exception.UserAlreadyExistsException;
import ru.iposhka.exception.UserNotFoundException;
import ru.iposhka.model.Gender;
import ru.iposhka.model.User;
import ru.iposhka.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    public static final String USER_ID = "user_id";
    public static final String NAME = "name";
    public static final String GENDER = "gender";
    public static final String EMAIL = "email";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public JwtResponseDto signUp(UserSignUpRequestDto userSignUpRequestDto) {
        if (userRepository.existsUserByEmail(userSignUpRequestDto.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Пользователь с %s уже существует!".formatted(userSignUpRequestDto.getEmail()));
        }

        User user = User.builder()
                .email(userSignUpRequestDto.getEmail())
                .name(userSignUpRequestDto.getName())
                .password(passwordEncoder.encode(userSignUpRequestDto.getPassword()))
                .gender(Gender.valueOf(userSignUpRequestDto.getGender()))
                .build();

        userRepository.save(user);

        return authenticate(user);
    }

    public JwtResponseDto signIn(UserSignInRequestDto userSignInRequestDto) {
        User user = userRepository.findByEmail(userSignInRequestDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException(
                        "Пользователь с %s не существует.".formatted(userSignInRequestDto.getEmail())
                ));

        if (!passwordEncoder.matches(userSignInRequestDto.getPassword(), user.getPassword())) {
            throw new UserNotFoundException("Неверный email или пароль.");
        }

        return authenticate(user);
    }

    public AuthResponseDto refreshToken(String refreshToken) {
        Claims claims = jwtService.validateRefreshToken(refreshToken);

        return new AuthResponseDto(jwtService.generateAccessToken(claims));
    }

    private JwtResponseDto authenticate(User user) {
        Map<String, Object> tokenClaims = buildTokenClaims(user);

        return JwtResponseDto.builder()
                .accessToken(jwtService.generateAccessToken(tokenClaims))
                .refreshToken(jwtService.generateRefreshToken(tokenClaims))
                .build();
    }

    private Map<String, Object> buildTokenClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID, user.getId());
        claims.put(NAME, user.getName());
        claims.put(GENDER, user.getGender().name());
        claims.put(EMAIL, user.getEmail());
        return claims;
    }
}