package ru.iposhka.service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.iposhka.dto.response.CoupleInviteResponseDto;
import ru.iposhka.dto.response.CoupleStateResponseDto;
import ru.iposhka.dto.response.PartnerShortDto;
import ru.iposhka.exception.CoupleOperationException;
import ru.iposhka.exception.InviteNotFoundException;
import ru.iposhka.exception.UserNotFoundException;
import ru.iposhka.model.Couple;
import ru.iposhka.model.CoupleInvite;
import ru.iposhka.model.CoupleStatus;
import ru.iposhka.model.Gender;
import ru.iposhka.model.InviteStatus;
import ru.iposhka.model.User;
import ru.iposhka.repository.CoupleInviteRepository;
import ru.iposhka.repository.CoupleRepository;
import ru.iposhka.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CoupleService {

    private final UserRepository userRepository;
    private final CoupleRepository coupleRepository;
    private final CoupleInviteRepository coupleInviteRepository;
    private final InviteTokenService inviteTokenService;

    @Value("${couple.invite.expiration-hours:24}")
    private long inviteExpirationHours;

    @Transactional
    public CoupleInviteResponseDto createOrGetInvite(Long userId) {
        LocalDateTime now = now();

        User inviter = getUserForUpdate(userId);
        ensureUserHasNoActiveCouple(inviter.getId());

        List<CoupleInvite> activeInvites = coupleInviteRepository.findAllActiveByInviterIdForUpdate(
                inviter.getId(),
                InviteStatus.ACTIVE,
                now
        );

        for (CoupleInvite invite : activeInvites) {
            if (isExpired(invite.getExpiresAt(), now)) {
                invite.setStatus(InviteStatus.EXPIRED);
            } else {
                invite.setStatus(InviteStatus.REVOKED);
            }
        }

        if (!activeInvites.isEmpty()) {
            coupleInviteRepository.saveAll(activeInvites);
        }

        GeneratedInviteToken generatedToken = inviteTokenService.generate();

        CoupleInvite invite = CoupleInvite.builder()
                .id(generatedToken.inviteId())
                .inviter(inviter)
                .tokenHash(generatedToken.secretHash())
                .expiresAt(now.plusHours(inviteExpirationHours))
                .expectedGender(oppositeGender(inviter.getGender()))
                .status(InviteStatus.ACTIVE)
                .createdAt(now)
                .build();

        coupleInviteRepository.save(invite);

        return toInviteResponse(invite, generatedToken.rawToken());
    }

    @Transactional
    public CoupleInviteResponseDto acceptInvite(Long userId, String token) {
        LocalDateTime now = now();

        ParsedInviteToken parsedToken = inviteTokenService.parse(token);

        CoupleInvite invite = coupleInviteRepository.findByIdForUpdate(parsedToken.inviteId())
                .orElseThrow(() -> new InviteNotFoundException("Приглашение не найдено"));

        User inviter = invite.getInviter();
        User invitedUser = getUser(userId);

        lockUsersInStableOrder(inviter.getId(), invitedUser.getId());

        inviter = getUser(inviter.getId());
        invitedUser = getUser(invitedUser.getId());

        ensureUserHasNoActiveCouple(inviter.getId());
        ensureUserHasNoActiveCouple(invitedUser.getId());

        validateInviteForAccept(invite, inviter, invitedUser, parsedToken.secret(), now);

        Couple couple = createCouple(inviter, invitedUser, now);
        coupleRepository.save(couple);

        invite.setCouple(couple);
        invite.setStatus(InviteStatus.USED);
        coupleInviteRepository.save(invite);

        revokeOtherActiveInvites(inviter.getId(), invite.getId(), now);
        revokeOtherActiveInvites(invitedUser.getId(), null, now);

        return toInviteResponse(invite, null);
    }

    @Transactional(readOnly = true)
    public CoupleStateResponseDto getMyState(Long userId) {
        LocalDateTime now = now();

        User user = getUser(userId);

        Couple couple = coupleRepository.findActiveByUserId(user.getId(), CoupleStatus.ACTIVE).orElse(null);
        if (couple != null && isCompleteCouple(couple)) {
            User partner = resolvePartner(couple, user.getId());

            return new CoupleStateResponseDto(
                    true,
                    couple.getId(),
                    couple.getStatus().name(),
                    new PartnerShortDto(partner.getId(), partner.getName(), partner.getGender().name()),
                    null
            );
        }

        CoupleInvite invite = coupleInviteRepository
                .findTopActiveByInviterId(user.getId(), InviteStatus.ACTIVE, now)
                .orElse(null);

        CoupleInviteResponseDto inviteResponse = null;
        if (invite != null && !isExpired(invite.getExpiresAt(), now)) {
            inviteResponse = toInviteResponse(invite, null);
        }

        return new CoupleStateResponseDto(
                false,
                null,
                null,
                null,
                inviteResponse
        );
    }

    @Transactional
    public void revokeCurrentInvite(Long userId) {
        LocalDateTime now = now();

        ensureUserHasNoActiveCouple(userId);

        CoupleInvite invite = coupleInviteRepository.findTopActiveByInviterIdForUpdate(
                        userId,
                        InviteStatus.ACTIVE,
                        now
                )
                .orElseThrow(() -> new InviteNotFoundException("Активное приглашение не найдено"));

        if (isExpired(invite.getExpiresAt(), now)) {
            invite.setStatus(InviteStatus.EXPIRED);
            coupleInviteRepository.save(invite);
            throw new InviteNotFoundException("Активное приглашение не найдено");
        }

        invite.setStatus(InviteStatus.REVOKED);
        coupleInviteRepository.save(invite);
    }

    private void validateInviteForAccept(
            CoupleInvite invite,
            User inviter,
            User invitedUser,
            String rawSecret,
            LocalDateTime now
    ) {
        if (invite.getStatus() != InviteStatus.ACTIVE) {
            throw new CoupleOperationException("Приглашение уже не активно");
        }

        if (isExpired(invite.getExpiresAt(), now)) {
            invite.setStatus(InviteStatus.EXPIRED);
            coupleInviteRepository.save(invite);
            throw new CoupleOperationException("Срок действия приглашения истек");
        }

        if (inviter.getId().equals(invitedUser.getId())) {
            throw new CoupleOperationException("Нельзя принять собственное приглашение");
        }

        if (invite.getExpectedGender() != invitedUser.getGender()) {
            throw new CoupleOperationException("Приглашение предназначено для другого пола");
        }

        if (!inviteTokenService.matches(rawSecret, invite.getTokenHash())) {
            throw new CoupleOperationException("Некорректный токен приглашения");
        }
    }

    private Couple createCouple(User inviter, User invitedUser, LocalDateTime now) {
        Couple couple = new Couple();
        couple.setStatus(CoupleStatus.ACTIVE);
        couple.setCreatedAt(now);

        if (inviter.getGender() == Gender.MALE) {
            couple.setBoy(inviter);
            couple.setGirlfriend(invitedUser);
        } else {
            couple.setBoy(invitedUser);
            couple.setGirlfriend(inviter);
        }

        return couple;
    }

    private void revokeOtherActiveInvites(Long inviterId, UUID excludeInviteId, LocalDateTime now) {
        List<CoupleInvite> invites = coupleInviteRepository.findAllActiveByInviterIdForUpdate(
                inviterId,
                InviteStatus.ACTIVE,
                now
        );

        for (CoupleInvite invite : invites) {
            if (excludeInviteId != null && excludeInviteId.equals(invite.getId())) {
                continue;
            }

            if (isExpired(invite.getExpiresAt(), now)) {
                invite.setStatus(InviteStatus.EXPIRED);
            } else {
                invite.setStatus(InviteStatus.REVOKED);
            }
        }

        if (!invites.isEmpty()) {
            coupleInviteRepository.saveAll(invites);
        }
    }

    private void ensureUserHasNoActiveCouple(Long userId) {
        coupleRepository.findActiveByUserId(userId, CoupleStatus.ACTIVE)
                .filter(this::isCompleteCouple)
                .ifPresent(couple -> {
                    throw new CoupleOperationException("Пользователь уже состоит в паре");
                });
    }

    private boolean isCompleteCouple(Couple couple) {
        return couple.getBoy() != null && couple.getGirlfriend() != null;
    }

    private User resolvePartner(Couple couple, Long currentUserId) {
        if (couple.getBoy() != null && couple.getBoy().getId().equals(currentUserId)) {
            return couple.getGirlfriend();
        }
        return couple.getBoy();
    }

    private void lockUsersInStableOrder(Long firstUserId, Long secondUserId) {
        long minId = Math.min(firstUserId, secondUserId);
        long maxId = Math.max(firstUserId, secondUserId);

        getUserForUpdate(minId);
        if (minId != maxId) {
            getUserForUpdate(maxId);
        }
    }

    private boolean isExpired(LocalDateTime expiresAt, LocalDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id=%s не найден".formatted(userId)));
    }

    private User getUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id=%s не найден".formatted(userId)));
    }

    private Gender oppositeGender(Gender gender) {
        return switch (gender) {
            case MALE -> Gender.FEMALE;
            case FEMALE -> Gender.MALE;
        };
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private CoupleInviteResponseDto toInviteResponse(CoupleInvite invite, String rawTokenOrNull) {
        return new CoupleInviteResponseDto(
                invite.getId().toString(),
                invite.getCouple() != null ? invite.getCouple().getId() : null,
                rawTokenOrNull,
                invite.getExpectedGender().name(),
                invite.getStatus().name(),
                invite.getExpiresAt()
        );
    }

    private record GeneratedInviteToken(UUID inviteId, String rawToken, String secretHash) {}
    private record ParsedInviteToken(UUID inviteId, String secret) {}

    @Service
    @RequiredArgsConstructor
    static class InviteTokenService {

        private static final int SECRET_BYTES = 32;
        private final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

        GeneratedInviteToken generate() {
            UUID inviteId = UUID.randomUUID();

            byte[] secretBytes = new byte[SECRET_BYTES];
            secureRandom.nextBytes(secretBytes);

            String secret = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(secretBytes);

            String rawToken = inviteId + "." + secret;
            String secretHash = sha256Hex(secret);

            return new GeneratedInviteToken(inviteId, rawToken, secretHash);
        }

        ParsedInviteToken parse(String token) {
            if (token == null || token.isBlank()) {
                throw new InviteNotFoundException("Некорректный формат токена приглашения");
            }

            String[] parts = token.split("\\.", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new InviteNotFoundException("Некорректный формат токена приглашения");
            }

            try {
                UUID inviteId = UUID.fromString(parts[0]);
                return new ParsedInviteToken(inviteId, parts[1]);
            } catch (IllegalArgumentException e) {
                throw new InviteNotFoundException("Некорректный формат токена приглашения");
            }
        }

        boolean matches(String rawSecret, String storedHash) {
            return sha256Hex(rawSecret).equals(storedHash);
        }

        private String sha256Hex(String value) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return java.util.HexFormat.of().formatHex(hash);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 недоступен", e);
            }
        }
    }
}