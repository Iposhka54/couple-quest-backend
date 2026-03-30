package ru.iposhka.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.iposhka.dto.response.CoupleInviteResponseDto;
import ru.iposhka.dto.response.CoupleStateResponseDto;
import ru.iposhka.dto.response.PartnerShortDto;
import ru.iposhka.exception.ConflictException;
import ru.iposhka.exception.NotFoundException;
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
    private static final int TOKEN_SIZE_BYTES = 32;

    private final UserRepository userRepository;
    private final CoupleRepository coupleRepository;
    private final CoupleInviteRepository coupleInviteRepository;
    private final SecureRandom SECURE_RANDOM;
    private final Clock clock;

    @Value("${couple.invite.expiration-hours:24}")
    private long inviteExpirationHours;

    @Transactional(readOnly = true)
    public CoupleInviteResponseDto getInvite(Long userId) {
        CoupleInvite invite = findActiveNotExpiredInvite(userId, now());
        return toInviteDto(invite);
    }

    @Transactional
    public CoupleInviteResponseDto createOrGetInvite(Long userId) {
        User inviter = getUserOrThrow(userId);
        ensureUserNotInActiveCouple(userId);

        LocalDateTime now = now();

        CoupleInvite activeInvite = coupleInviteRepository
                .findActiveNotExpiredByInviterId(userId, now)
                .orElse(null);

        if (activeInvite != null) {
            return toInviteDto(activeInvite);
        }

        expireCurrentActiveInviteIfNeeded(userId, now);

        CoupleInvite newInvite = buildInvite(inviter, now);

        try {
            coupleInviteRepository.saveAndFlush(newInvite);
            return toInviteDto(newInvite);
        } catch (DataIntegrityViolationException ex) {
            CoupleInvite existingInvite = coupleInviteRepository
                    .findActiveNotExpiredByInviterId(userId, now)
                    .orElseThrow(() -> ex);

            return toInviteDto(existingInvite);
        }
    }

    @Transactional
    public CoupleStateResponseDto acceptInvite(Long accepterUserId, String token) {
        LocalDateTime now = now();

        CoupleInvite invite = getInviteByTokenOrThrow(token);
        validateInviteBeforeLock(invite, accepterUserId, now);

        Long inviterUserId = invite.getInviter().getId();

        List<User> lockedUsers = lockUsers(inviterUserId, accepterUserId);
        User inviter = findUserById(lockedUsers, inviterUserId);
        User accepter = findUserById(lockedUsers, accepterUserId);

        CoupleInvite lockedInvite = lockInvite(invite.getId());
        validateInviteAfterLock(lockedInvite, accepter);
        ensureUsersNotInActiveCouple(inviter.getId(), accepter.getId());

        Couple couple = buildCouple(inviter, accepter, now);
        coupleRepository.save(couple);

        lockedInvite.setStatus(InviteStatus.ACCEPTED);
        coupleInviteRepository.save(lockedInvite);

        revokeOwnActiveInviteIfExists(accepterUserId);

        return toCoupleStateDto(inviter, accepter, accepterUserId);
    }

    @Transactional
    public void revokeCurrentInvite(Long userId) {
        CoupleInvite invite = coupleInviteRepository
                .findByInviterIdAndStatusForUpdate(userId, InviteStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Нет активного приглашения!"));

        invite.setStatus(InviteStatus.REVOKED);
        coupleInviteRepository.save(invite);
    }

    private CoupleInvite findActiveNotExpiredInvite(Long userId, LocalDateTime now) {
        return coupleInviteRepository.findActiveNotExpiredByInviterId(userId, now)
                .orElseThrow(() -> new NotFoundException("У вас нет приглашения!"));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не существует!"));
    }

    private CoupleInvite getInviteByTokenOrThrow(String token) {
        return coupleInviteRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Приглашение не найдено!"));
    }

    private void ensureUserNotInActiveCouple(Long userId) {
        if (coupleRepository.existsActiveByUserId(userId, CoupleStatus.ACTIVE)) {
            throw new ConflictException("Пользователь уже в паре");
        }
    }

    private void ensureUsersNotInActiveCouple(Long firstUserId, Long secondUserId) {
        if (coupleRepository.existsActiveByUserId(firstUserId, CoupleStatus.ACTIVE)
                || coupleRepository.existsActiveByUserId(secondUserId, CoupleStatus.ACTIVE)) {
            throw new ConflictException("Кто-то уже в паре");
        }
    }

    private void expireCurrentActiveInviteIfNeeded(Long userId, LocalDateTime now) {
        CoupleInvite currentInvite = coupleInviteRepository
                .findByInviterIdAndStatusForUpdate(userId, InviteStatus.ACTIVE)
                .orElse(null);

        if (currentInvite == null) {
            return;
        }

        if (!isExpired(currentInvite, now)) {
            return;
        }

        currentInvite.setStatus(InviteStatus.EXPIRED);
        coupleInviteRepository.save(currentInvite);
    }

    private CoupleInvite buildInvite(User inviter, LocalDateTime now) {
        CoupleInvite invite = new CoupleInvite();
        invite.setId(UUID.randomUUID());
        invite.setInviter(inviter);
        invite.setToken(generateToken());
        invite.setExpectedGender(opposite(inviter.getGender()));
        invite.setStatus(InviteStatus.ACTIVE);
        invite.setCreatedAt(now);
        invite.setExpiresAt(now.plusHours(inviteExpirationHours));
        return invite;
    }

    private void validateInviteBeforeLock(CoupleInvite invite, Long accepterUserId, LocalDateTime now) {
        Long inviterUserId = invite.getInviter().getId();

        if (inviterUserId.equals(accepterUserId)) {
            throw new ConflictException("Вы не можете принять своё же приглашение!");
        }

        if (invite.getStatus() != InviteStatus.ACTIVE) {
            throw new ConflictException("Ссылка не активна!");
        }

        if (isExpired(invite, now)) {
            throw new ConflictException("Ссылка истекла!");
        }
    }

    private void validateInviteAfterLock(CoupleInvite invite, User accepter) {
        if (accepter.getGender() != invite.getExpectedGender()) {
            throw new ConflictException("Приглашение предназначено для другого пола");
        }
    }

    private List<User> lockUsers(Long firstUserId, Long secondUserId) {
        List<Long> sortedIds = Stream.of(firstUserId, secondUserId)
                .sorted(Comparator.naturalOrder())
                .toList();

        List<User> lockedUsers = userRepository.findAllByIdInForUpdate(sortedIds);

        if (lockedUsers.size() != 2) {
            throw new NotFoundException("Пользователь не найден!");
        }

        return lockedUsers;
    }

    private User findUserById(List<User> users, Long userId) {
        return users.stream()
                .filter(user -> user.getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Пользователь не найден!"));
    }

    private CoupleInvite lockInvite(UUID inviteId) {
        return coupleInviteRepository.findByIdForUpdate(inviteId)
                .orElseThrow(() -> new NotFoundException("Приглашение не найдено!"));
    }

    private void revokeOwnActiveInviteIfExists(Long userId) {
        coupleInviteRepository.findByInviterIdAndStatusForUpdate(userId, InviteStatus.ACTIVE)
                .ifPresent(invite -> {
                    invite.setStatus(InviteStatus.REVOKED);
                    coupleInviteRepository.save(invite);
                });
    }

    private Couple buildCouple(User inviter, User accepter, LocalDateTime now) {
        Couple couple = new Couple();
        couple.setStatus(CoupleStatus.ACTIVE);
        couple.setCreatedAt(now);

        if (inviter.getGender() == Gender.MALE) {
            couple.setBoy(inviter);
            couple.setGirlfriend(accepter);
        } else {
            couple.setBoy(accepter);
            couple.setGirlfriend(inviter);
        }

        return couple;
    }

    private CoupleInviteResponseDto toInviteDto(CoupleInvite invite) {
        return new CoupleInviteResponseDto(
                invite.getToken(),
                invite.getExpiresAt(),
                invite.getStatus().name()
        );
    }

    private CoupleStateResponseDto toCoupleStateDto(
            User inviter,
            User accepter,
            Long currentUserId
    ) {
        User partner = inviter.getId().equals(currentUserId) ? accepter : inviter;

        return new CoupleStateResponseDto(
                true,
                new PartnerShortDto(partner.getName(), partner.getGender().name())
        );
    }

    private boolean isExpired(CoupleInvite invite, LocalDateTime now) {
        return invite.getExpiresAt() != null && !invite.getExpiresAt().isAfter(now);
    }

    private Gender opposite(Gender gender) {
        return switch (gender) {
            case MALE -> Gender.FEMALE;
            case FEMALE -> Gender.MALE;
        };
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}