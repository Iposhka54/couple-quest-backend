package ru.iposhka.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.iposhka.model.EmailJob;

public interface EmailJobRepository extends JpaRepository<EmailJob, UUID> {

    @Query(value = """
            SELECT *
            FROM email_jobs
            WHERE (
                status = 'PENDING'
                AND (next_attempt_at IS NULL OR next_attempt_at <= now())
            )
            OR (
                status = 'PROCESSING'
                AND updated_at <= now() - (:processingTimeoutSeconds * interval '1 second')
            )
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EmailJob> lockNextBatch(
            @Param("limit") int limit,
            @Param("processingTimeoutSeconds") long processingTimeoutSeconds
    );
}