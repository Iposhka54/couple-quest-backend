package ru.iposhka.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.iposhka.model.MealType;
import ru.iposhka.model.WeeklyMenuEntry;

public interface WeeklyMenuEntryRepository extends JpaRepository<WeeklyMenuEntry, Long> {

    @EntityGraph(attributePaths = {"meal", "meal.ingredients"})
    List<WeeklyMenuEntry> findAllByCoupleIdAndPlannedDateBetweenOrderByPlannedDateAscMealTypeAsc(
            Long coupleId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<WeeklyMenuEntry> findByCoupleIdAndPlannedDateAndMealType(Long coupleId, LocalDate plannedDate, MealType mealType);

    void deleteAllByCoupleIdAndPlannedDateBetween(Long coupleId, LocalDate startDate, LocalDate endDate);
}