package ru.iposhka.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.iposhka.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsUserByEmail(String email);

    Optional<User> findByEmail(String email);
}