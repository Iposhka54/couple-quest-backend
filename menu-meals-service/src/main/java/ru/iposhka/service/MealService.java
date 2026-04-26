package ru.iposhka.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.iposhka.dto.request.MealIngredientRequestDto;
import ru.iposhka.dto.request.MealUpsertRequestDto;
import ru.iposhka.dto.response.MealIngredientResponseDto;
import ru.iposhka.dto.response.MealResponseDto;
import ru.iposhka.exception.NotFoundException;
import ru.iposhka.model.Meal;
import ru.iposhka.model.MealIngredient;
import ru.iposhka.repository.MealRepository;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealRepository mealRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<MealResponseDto> getMeals(Long coupleId) {
        return mealRepository.findAllByCoupleIdOrderByNameAsc(coupleId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MealResponseDto getMeal(Long mealId, Long coupleId) {
        return toDto(getMealEntity(mealId, coupleId));
    }

    @Transactional
    public MealResponseDto createMeal(Long userId, MealUpsertRequestDto requestDto) {
        LocalDateTime now = now();
        Meal meal = Meal.builder()
                .coupleId(requestDto.coupleId())
                .createdByUserId(userId)
                .name(requestDto.name().trim())
                .description(trimToNull(requestDto.description()))
                .portionSize(requestDto.portionSize().trim())
                .calories(requestDto.calories())
                .proteins(requestDto.proteins())
                .fats(requestDto.fats())
                .carbs(requestDto.carbs())
                .createdAt(now)
                .updatedAt(now)
                .build();

        meal.replaceIngredients(mapIngredients(requestDto.ingredients()));

        return toDto(mealRepository.save(meal));
    }

    @Transactional
    public MealResponseDto updateMeal(Long mealId, Long userId, MealUpsertRequestDto requestDto) {
        Meal meal = getMealEntity(mealId, requestDto.coupleId());
        meal.setName(requestDto.name().trim());
        meal.setDescription(trimToNull(requestDto.description()));
        meal.setPortionSize(requestDto.portionSize().trim());
        meal.setCalories(requestDto.calories());
        meal.setProteins(requestDto.proteins());
        meal.setFats(requestDto.fats());
        meal.setCarbs(requestDto.carbs());
        meal.setUpdatedAt(now());
        meal.setCreatedByUserId(meal.getCreatedByUserId() == null ? userId : meal.getCreatedByUserId());
        meal.replaceIngredients(mapIngredients(requestDto.ingredients()));
        return toDto(mealRepository.save(meal));
    }

    @Transactional
    public void deleteMeal(Long mealId, Long coupleId) {
        Meal meal = getMealEntity(mealId, coupleId);
        mealRepository.delete(meal);
    }

    public Meal getMealEntity(Long mealId, Long coupleId) {
        return mealRepository.findByIdAndCoupleId(mealId, coupleId)
                .orElseThrow(() -> new NotFoundException("Блюдо не найдено"));
    }

    private List<MealIngredient> mapIngredients(List<MealIngredientRequestDto> ingredients) {
        return ingredients.stream()
                .map(ingredient -> MealIngredient.builder()
                        .name(ingredient.name().trim())
                        .amount(ingredient.amount())
                        .unit(ingredient.unit().trim())
                        .build())
                .toList();
    }

    private MealResponseDto toDto(Meal meal) {
        return new MealResponseDto(
                meal.getId(),
                meal.getCoupleId(),
                meal.getCreatedByUserId(),
                meal.getName(),
                meal.getDescription(),
                meal.getPortionSize(),
                meal.getCalories(),
                meal.getProteins(),
                meal.getFats(),
                meal.getCarbs(),
                meal.getIngredients().stream()
                        .map(ingredient -> new MealIngredientResponseDto(
                                ingredient.getId(),
                                ingredient.getName(),
                                ingredient.getAmount(),
                                ingredient.getUnit()
                        ))
                        .toList(),
                meal.getCreatedAt(),
                meal.getUpdatedAt()
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}