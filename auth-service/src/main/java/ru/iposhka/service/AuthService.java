package ru.iposhka.service;

import io.jsonwebtoken.Claims;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.iposhka.dto.event.EmailVerificationRequestedEvent;
import ru.iposhka.dto.request.ResendEmailCodeRequestDto;
import ru.iposhka.dto.request.UserSignInRequestDto;
import ru.iposhka.dto.request.UserSignUpRequestDto;
import ru.iposhka.dto.request.VerifyEmailCodeRequestDto;
import ru.iposhka.dto.response.AuthResponseDto;
import ru.iposhka.dto.response.JwtResponseDto;
import ru.iposhka.exception.BadRequestException;
import ru.iposhka.exception.ConflictException;
import ru.iposhka.exception.NotFoundException;
import ru.iposhka.model.AuthStatus;
import ru.iposhka.model.EmailVerification;
import ru.iposhka.model.Gender;
import ru.iposhka.model.User;
import ru.iposhka.repository.EmailVerificationRepository;
import ru.iposhka.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    public static final String USER_ID = "user_id";
    public static final String NAME = "name";
    public static final String GENDER = "gender";
    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email_verified";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final java.security.SecureRandom secureRandom;
    private final Clock clock;
    private final EmailVerificationEventPublisher emailVerificationEventPublisher;

    @Value("${email-verification.code.length}")
    private int codeLength;

    @Value("${email-verification.code.ttl-minutes}")
    private long codeTtlMinutes;

    @Value("${email-verification.code.max-attempts}")
    private int maxAttempts;

    @Value("${email-verification.code.resend-cooldown-seconds}")
    private long resendCooldownSeconds;

    @Transactional
    public AuthResponseDto signUp(UserSignUpRequestDto userSignUpRequestDto) {
        if (userRepository.existsUserByEmail(userSignUpRequestDto.getEmail())) {
            throw new ConflictException(
                    "Пользователь с %s уже существует!".formatted(userSignUpRequestDto.getEmail()));
        }

        User user = User.builder()
                .email(userSignUpRequestDto.getEmail())
                .name(userSignUpRequestDto.getName())
                .password(passwordEncoder.encode(userSignUpRequestDto.getPassword()))
                .gender(Gender.valueOf(userSignUpRequestDto.getGender()))
                .emailVerified(false)
                .build();

        userRepository.save(user);
        EmailVerification verification = createAndSendVerification(user);
        return buildPendingResponse(user, verification);
    }

    @Transactional
    public AuthResponseDto signIn(UserSignInRequestDto userSignInRequestDto) {
        User user = userRepository.findByEmail(userSignInRequestDto.getEmail())
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с %s не существует.".formatted(userSignInRequestDto.getEmail())
                ));

        if (!passwordEncoder.matches(userSignInRequestDto.getPassword(), user.getPassword())) {
            throw new NotFoundException("Неверный email или пароль.");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            EmailVerification verification = emailVerificationRepository.findLatestActiveByUserId(user.getId())
                    .map(existing -> isVerificationReusable(existing)
                            ? existing
                            : refreshVerification(existing, user))
                    .orElseGet(() -> createAndSendVerification(user));
            return buildPendingResponse(user, verification);
        }

        JwtResponseDto jwtResponseDto = authenticate(user);
        return buildAuthenticatedResponse(user, jwtResponseDto.getAccessToken());
    }

    @Transactional
    public AuthResponseDto verifyEmailCode(VerifyEmailCodeRequestDto requestDto) {
        User user = userRepository.findByEmail(requestDto.email())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            JwtResponseDto jwtResponseDto = authenticate(user);
            return buildAuthenticatedResponse(user, jwtResponseDto.getAccessToken());
        }

        EmailVerification verification = emailVerificationRepository.findLatestActiveByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Код подтверждения не найден. Запросите новый код."));

        validateVerificationCode(verification, requestDto.code());

        LocalDateTime now = now();
        verification.setUsedAt(now);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(now);

        emailVerificationRepository.save(verification);
        userRepository.save(user);

        JwtResponseDto jwtResponseDto = authenticate(user);
        return buildAuthenticatedResponse(user, jwtResponseDto.getAccessToken());
    }

    @Transactional
    public AuthResponseDto resendEmailCode(ResendEmailCodeRequestDto requestDto) {
        User user = userRepository.findByEmail(requestDto.email())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email уже подтвержден.");
        }

        EmailVerification verification = emailVerificationRepository.findLatestActiveByUserId(user.getId())
                .map(existing -> {
                    if (existing.getResendAvailableAt().isAfter(now())) {
                        throw new BadRequestException("Повторная отправка пока недоступна.");
                    }
                    return refreshVerification(existing, user);
                })
                .orElseGet(() -> createAndSendVerification(user));
        return buildPendingResponse(user, verification);
    }

    public AuthResponseDto refreshToken(String refreshToken) {
        Claims claims = jwtService.validateRefreshToken(refreshToken);
        return AuthResponseDto.builder()
                .status(AuthStatus.AUTHENTICATED)
                .accessToken(jwtService.generateAccessToken(claims))
                .email((String) claims.get(EMAIL))
                .emailVerified(Boolean.TRUE.equals(claims.get(EMAIL_VERIFIED)))
                .build();
    }

    public JwtResponseDto issueTokens(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден."));
        return authenticate(user);
    }

    private void validateVerificationCode(EmailVerification verification, String code) {
        LocalDateTime now = now();
        if (verification.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("Срок действия кода истек. Запросите новый код.");
        }
        if (verification.getAttemptsCount() >= verification.getMaxAttempts()) {
            throw new BadRequestException("Превышено количество попыток. Запросите новый код.");
        }
        if (!hash(code).equals(verification.getCodeHash())) {
            verification.setAttemptsCount(verification.getAttemptsCount() + 1);
            emailVerificationRepository.save(verification);
            throw new BadRequestException("Неверный код подтверждения.");
        }
    }

    private EmailVerification createAndSendVerification(User user) {
        VerificationPayload payload = buildNewVerification(user);
        EmailVerification verification = payload.verification();
        emailVerificationRepository.save(verification);
        publishVerificationRequestedEvent(user, verification, payload.code());
        return verification;
    }

    private EmailVerification refreshVerification(EmailVerification verification, User user) {
        VerificationPayload payload = rebuildVerification(user, verification);
        EmailVerification refreshedVerification = payload.verification();
        emailVerificationRepository.save(refreshedVerification);
        publishVerificationRequestedEvent(user, refreshedVerification, payload.code());
        return refreshedVerification;
    }

    private VerificationPayload buildNewVerification(User user) {
        LocalDateTime now = now();
        String code = generateNumericCode();
        EmailVerification verification = EmailVerification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .codeHash(hash(code))
                .expiresAt(now.plusMinutes(codeTtlMinutes))
                .resendAvailableAt(now.plusSeconds(resendCooldownSeconds))
                .attemptsCount(0)
                .maxAttempts(maxAttempts)
                .createdAt(now)
                .build();
        return new VerificationPayload(verification, code);
    }

    private VerificationPayload rebuildVerification(User user, EmailVerification verification) {
        LocalDateTime now = now();
        String code = generateNumericCode();
        verification.setUser(user);
        verification.setCodeHash(hash(code));
        verification.setExpiresAt(now.plusMinutes(codeTtlMinutes));
        verification.setResendAvailableAt(now.plusSeconds(resendCooldownSeconds));
        verification.setAttemptsCount(0);
        verification.setMaxAttempts(maxAttempts);
        verification.setCreatedAt(now);
        verification.setUsedAt(null);
        return new VerificationPayload(verification, code);
    }

    private void publishVerificationRequestedEvent(User user, EmailVerification verification, String code) {
        LocalDateTime now = now();
        emailVerificationEventPublisher.publish(new EmailVerificationRequestedEvent(
                verification.getId(),
                user.getId(),
                user.getEmail(),
                user.getName(),
                code,
                verification.getExpiresAt(),
                now
        ));
    }

    private record VerificationPayload(EmailVerification verification, String code) {
    }

    private boolean isVerificationReusable(EmailVerification verification) {
        LocalDateTime now = now();
        return verification.getExpiresAt().isAfter(now)
                && verification.getAttemptsCount() < verification.getMaxAttempts();
    }

    private JwtResponseDto authenticate(User user) {
        Map<String, Object> tokenClaims = buildTokenClaims(user);

        return JwtResponseDto.builder()
                .accessToken(jwtService.generateAccessToken(tokenClaims))
                .refreshToken(jwtService.generateRefreshToken(tokenClaims))
                .build();
    }

    private AuthResponseDto buildAuthenticatedResponse(User user, String accessToken) {
        return AuthResponseDto.builder()
                .status(AuthStatus.AUTHENTICATED)
                .accessToken(accessToken)
                .email(user.getEmail())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .resendAvailableInSeconds(0L)
                .build();
    }

    private AuthResponseDto buildPendingResponse(User user, EmailVerification verification) {
        long seconds = Math.max(0, java.time.Duration.between(now(), verification.getResendAvailableAt()).getSeconds());
        return AuthResponseDto.builder()
                .status(AuthStatus.EMAIL_VERIFICATION_REQUIRED)
                .email(user.getEmail())
                .emailVerified(false)
                .resendAvailableInSeconds(seconds)
                .build();
    }

    private Map<String, Object> buildTokenClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID, user.getId());
        claims.put(NAME, user.getName());
        claims.put(GENDER, user.getGender().name());
        claims.put(EMAIL, user.getEmail());
        claims.put(EMAIL_VERIFIED, Boolean.TRUE.equals(user.getEmailVerified()));
        return claims;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String generateNumericCode() {
        int bound = (int) Math.pow(10, codeLength);
        return String.format("%0" + codeLength + "d", secureRandom.nextInt(bound));
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}