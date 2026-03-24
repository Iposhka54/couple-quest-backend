package ru.iposhka.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.iposhka.model.Couple;
import ru.iposhka.model.CoupleStatus;

public interface CoupleRepository extends JpaRepository<Couple, Long> {

    @Query("""
            select c from Couple c
            where c.status = :status
              and ((c.boy.id = :userId) or (c.girlfriend.id = :userId))
            """)
    Optional<Couple> findActiveByUserId(@Param("userId") Long userId, @Param("status") CoupleStatus status);
}