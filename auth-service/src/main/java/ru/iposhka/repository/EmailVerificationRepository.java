package ru.iposhka.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.iposhka.model.EmailVerification;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    @Query(value = """
        select *
        from email_verification ev
        where ev.user_id = :userId
          and ev.used_at is null
        order by ev.created_at desc
        fetch first 1 row only
    """, nativeQuery = true)
    Optional<EmailVerification> findLatestActiveByUserId(Long userId);
}