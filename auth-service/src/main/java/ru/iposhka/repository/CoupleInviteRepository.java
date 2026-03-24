package ru.iposhka.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.iposhka.model.CoupleInvite;
import ru.iposhka.model.InviteStatus;

public interface CoupleInviteRepository extends JpaRepository<CoupleInvite, UUID> {

    @Query("""
            select ci from CoupleInvite ci
            where ci.inviter.id = :inviterId
              and ci.status = :status
              and (ci.expiresAt is null or ci.expiresAt > :now)
            order by ci.createdAt desc
            """)
    Optional<CoupleInvite> findTopActiveByInviterId(@Param("inviterId") Long inviterId,
            @Param("status") InviteStatus status,
            @Param("now") LocalDateTime now);
}
