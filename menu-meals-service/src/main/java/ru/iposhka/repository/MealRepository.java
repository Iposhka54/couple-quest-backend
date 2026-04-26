package ru.iposhka.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.iposhka.model.Meal;

public interface MealRepository extends JpaRepository<Meal, Long> {

    @EntityGraph(attributePaths = "ingredients")
    List<Meal> findAllByCoupleIdOrderByNameAsc(Long coupleId);

    @EntityGraph(attributePaths = "ingredients")
    Optional<Meal> findByIdAndCoupleId(Long id, Long coupleId);

    boolean existsByIdAndCoupleId(Long id, Long coupleId);
}