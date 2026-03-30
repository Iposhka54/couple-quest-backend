package ru.iposhka.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.iposhka.model.Couple;
import ru.iposhka.model.CoupleStatus;

public interface CoupleRepository extends JpaRepository<Couple, Long> {

    @Query("""
        select
            case
                when count(c) > 0 then true
                else false
            end
        from Couple c
        where c.status = :status
          and (c.boy.id = :userId or c.girlfriend.id = :userId)
    """)
    boolean existsActiveByUserId(Long userId, CoupleStatus status);
}