package ru.iposhka.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
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

    @Value("${couple.invite.expiration-hours:24}")
    private long inviteExpirationHours;

    @Transactional
    public CoupleInviteResponseDto createOrGetInvite(Long userId) {
        User inviter = getUser(userId);
        ensureUserDoesNotHaveFullCouple(inviter.getId());

        LocalDateTime now = LocalDateTime.now();
        return coupleInviteRepository.findTopActiveByInviterId(inviter.getId(), InviteStatus.ACTIVE, now)
                .map(this::toInviteResponse)
                .orElseGet(() -> createInvite(inviter, now));
    }

    @Transactional
    public CoupleInviteResponseDto acceptInvite(Long userId, String token) {
        User invitedUser = getUser(userId);
        ensureUserDoesNotHaveFullCouple(invitedUser.getId());

        UUID inviteId = parseInviteId(token);
        CoupleInvite invite = coupleInviteRepository.findById(inviteId)
                .orElseThrow(() -> new InviteNotFoundException("Приглашение не найдено"));

        validateInviteForAccept(invite, invitedUser, token);

        Couple couple = invite.getCouple();
        if (couple == null) {
            throw new CoupleOperationException("У приглашения отсутствует связанная пара");
        }

        if (invitedUser.getGender() == Gender.MALE) {
            if (couple.getBoy() != null) {
                throw new CoupleOperationException("Место парня в паре уже занято");
            }
            couple.setBoy(invitedUser);
        } else {
            if (couple.getGirlfriend() != null) {
                throw new CoupleOperationException("Место девушки в паре уже занято");
            }
            couple.setGirlfriend(invitedUser);
        }

        coupleRepository.save(couple);
        invite.setStatus(InviteStatus.USED);
        coupleInviteRepository.save(invite);

        return toInviteResponse(invite);
    }

    @Transactional(readOnly = true)
    public CoupleStateResponseDto getMyState(Long userId) {
        User user = getUser(userId);

        Couple couple = coupleRepository.findActiveByUserId(user.getId(), CoupleStatus.ACTIVE).orElse(null);
        if (couple != null && couple.getBoy() != null && couple.getGirlfriend() != null) {
            User partner = couple.getBoy().getId().equals(user.getId()) ? couple.getGirlfriend() : couple.getBoy();
            return new CoupleStateResponseDto(
                    true,
                    couple.getId(),
                    couple.getStatus().name(),
                    new PartnerShortDto(partner.getId(), partner.getName(), partner.getGender().name()),
                    null
            );
        }

        CoupleInvite invite = coupleInviteRepository.findTopActiveByInviterId(user.getId(), InviteStatus.ACTIVE, LocalDateTime.now())
                .orElse(null);

        return new CoupleStateResponseDto(false, null, null, null, invite != null ? toInviteResponse(invite) : null);
    }

    @Transactional
    public void revokeCurrentInvite(Long userId) {
        CoupleInvite invite = coupleInviteRepository.findTopActiveByInviterId(userId, InviteStatus.ACTIVE, LocalDateTime.now())
                .orElseThrow(() -> new InviteNotFoundException("Активное приглашение не найдено"));
        invite.setStatus(InviteStatus.REVOKED);
        coupleInviteRepository.save(invite);
    }

    private CoupleInviteResponseDto createInvite(User inviter, LocalDateTime now) {
        Couple couple = new Couple();
        couple.setStatus(CoupleStatus.ACTIVE);
        couple.setCreatedAt(now);
        if (inviter.getGender() == Gender.MALE) {
            couple.setBoy(inviter);
        } else {
            couple.setGirlfriend(inviter);
        }
        couple = coupleRepository.save(couple);

        UUID inviteId = UUID.randomUUID();
        CoupleInvite invite = CoupleInvite.builder()
                .id(inviteId)
                .couple(couple)
                .inviter(inviter)
                .tokenHash(hashToken(inviteId.toString()))
                .expiresAt(now.plusHours(inviteExpirationHours))
                .expectedGender(oppositeGender(inviter.getGender()))
                .status(InviteStatus.ACTIVE)
                .createdAt(now)
                .build();

        return toInviteResponse(coupleInviteRepository.save(invite));
    }

    private void validateInviteForAccept(CoupleInvite invite, User invitedUser, String rawToken) {
        if (invite.getStatus() != InviteStatus.ACTIVE) {
            throw new CoupleOperationException("Приглашение уже не активно");
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(InviteStatus.EXPIRED);
            coupleInviteRepository.save(invite);
            throw new CoupleOperationException("Срок действия приглашения истек");
        }
        if (invite.getInviter().getId().equals(invitedUser.getId())) {
            throw new CoupleOperationException("Нельзя принять собственное приглашение");
        }
        if (invite.getExpectedGender() != invitedUser.getGender()) {
            throw new CoupleOperationException("Приглашение предназначено для другого пола");
        }
        if (!invite.getTokenHash().equals(hashToken(rawToken))) {
            throw new CoupleOperationException("Некорректный токен приглашения");
        }
    }

    private void ensureUserDoesNotHaveFullCouple(Long userId) {
        coupleRepository.findActiveByUserId(userId, CoupleStatus.ACTIVE)
                .filter(couple -> couple.getBoy() != null && couple.getGirlfriend() != null)
                .ifPresent(couple -> {
                    throw new CoupleOperationException("Пользователь уже состоит в паре");
                });
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id=%s не найден".formatted(userId)));
    }

    private Gender oppositeGender(Gender gender) {
        return gender == Gender.MALE ? Gender.FEMALE : Gender.MALE;
    }

    private UUID parseInviteId(String token) {
        try {
            return UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new InviteNotFoundException("Некорректный формат токена приглашения");
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    private CoupleInviteResponseDto toInviteResponse(CoupleInvite invite) {
        return new CoupleInviteResponseDto(
                invite.getId().toString(),
                invite.getCouple() != null ? invite.getCouple().getId() : null,
                invite.getId().toString(),
                invite.getExpectedGender().name(),
                invite.getStatus().name(),
                invite.getExpiresAt()
        );
    }
}