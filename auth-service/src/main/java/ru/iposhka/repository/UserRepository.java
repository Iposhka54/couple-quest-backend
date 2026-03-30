package ru.iposhka.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.iposhka.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsUserByEmail(String email);

    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select u
        from User u
        where u.id in :ids
        order by u.id
    """)
    List<User> findAllByIdInForUpdate(List<Long> ids);

    Optional<User> findById(Long id);
}