package ru.iposhka.service;

import io.jsonwebtoken.Claims;
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
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public JwtResponseDto signUp(UserSignUpRequestDto userSignUpRequestDto) {
        if (userRepository.existsUserByEmail(userSignUpRequestDto.getEmail())) {
            throw new UserAlreadyExistsException("Пользователь с %s уже существует!".formatted(userSignUpRequestDto.getEmail()));
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
                .orElseThrow(() -> new UserNotFoundException("Пользователь с %s не существует.".formatted(
                        userSignInRequestDto.getEmail())));
        return authenticate(user);
    }

    public AuthResponseDto refreshToken(String refreshToken) {
        Claims claims = jwtService.validateRefreshToken(refreshToken);

        Long userId = claims.get("user_id", Long.class);
        Gender gender = claims.get("gender", Gender.class);
        String name = claims.get("name", String.class);
        return new AuthResponseDto(jwtService.generateAccessToken(userId, name, gender));
    }

    private JwtResponseDto authenticate(User user) {
        return JwtResponseDto.builder()
                .accessToken(jwtService.generateAccessToken(user.getId(), user.getName(), user.getGender()))
                .refreshToken(jwtService.generateRefreshToken(user.getId(), user.getName(), user.getGender()))
                .build();
    }
}