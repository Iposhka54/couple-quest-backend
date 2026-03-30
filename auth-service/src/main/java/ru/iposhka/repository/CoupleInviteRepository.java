package ru.iposhka.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.iposhka.model.CoupleInvite;
import ru.iposhka.model.InviteStatus;

public interface CoupleInviteRepository extends JpaRepository<CoupleInvite, UUID> {

    Optional<CoupleInvite> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ci
        from CoupleInvite ci
        where ci.id = :id
    """)
    Optional<CoupleInvite> findByIdForUpdate(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ci
        from CoupleInvite ci
        where ci.inviter.id = :inviterId
          and ci.status = :status
    """)
    Optional<CoupleInvite> findByInviterIdAndStatusForUpdate(Long inviterId, InviteStatus status);

    @Query(value = """
        select *
        from couple_invite ci
        where ci.inviter_id = :inviterId
          and ci.status = ?#{T(ru.iposhka.model.InviteStatus).ACTIVE.ordinal()}
          and (ci.expires_at is null or ci.expires_at > :now)
    """, nativeQuery = true)
    Optional<CoupleInvite> findActiveNotExpiredByInviterId(Long inviterId, LocalDateTime now);


}