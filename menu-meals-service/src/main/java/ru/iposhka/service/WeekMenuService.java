package ru.iposhka.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.iposhka.dto.request.ReplaceWeekMenuRequestDto;
import ru.iposhka.dto.request.UpsertWeekMenuEntryRequestDto;
import ru.iposhka.dto.request.WeekMenuEntryRequestDto;
import ru.iposhka.dto.response.MealIngredientResponseDto;
import ru.iposhka.dto.response.MealResponseDto;
import ru.iposhka.dto.response.ShoppingListItemResponseDto;
import ru.iposhka.dto.response.WeekMenuResponseDto;
import ru.iposhka.dto.response.WeeklyMenuEntryResponseDto;
import ru.iposhka.exception.BadRequestException;
import ru.iposhka.model.Meal;
import ru.iposhka.model.MealIngredient;
import ru.iposhka.model.WeeklyMenuEntry;
import ru.iposhka.repository.WeeklyMenuEntryRepository;

@Service
@RequiredArgsConstructor
public class WeekMenuService {

    private final WeeklyMenuEntryRepository weeklyMenuEntryRepository;
    private final MealService mealService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public WeekMenuResponseDto getWeek(Long coupleId, LocalDate requestedDate) {
        LocalDate weekStart = resolveWeekStart(requestedDate);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<WeeklyMenuEntry> entries = weeklyMenuEntryRepository
                .findAllByCoupleIdAndPlannedDateBetweenOrderByPlannedDateAscMealTypeAsc(coupleId, weekStart, weekEnd);

        return new WeekMenuResponseDto(
                coupleId,
                weekStart,
                weekEnd,
                entries.stream().map(this::toDto).toList(),
                buildShoppingList(entries)
        );
    }

    @Transactional
    public WeekMenuResponseDto replaceWeek(Long userId, LocalDate requestedDate, ReplaceWeekMenuRequestDto requestDto) {
        LocalDate weekStart = resolveWeekStart(requestedDate);
        LocalDate weekEnd = weekStart.plusDays(6);

        validateEntriesBelongToWeek(weekStart, requestDto.entries());
        weeklyMenuEntryRepository.deleteAllByCoupleIdAndPlannedDateBetween(requestDto.coupleId(), weekStart, weekEnd);

        List<WeeklyMenuEntry> saved = saveEntries(userId, requestDto.coupleId(), requestDto.entries());
        return new WeekMenuResponseDto(
                requestDto.coupleId(),
                weekStart,
                weekEnd,
                saved.stream().sorted(entryComparator()).map(this::toDto).toList(),
                buildShoppingList(saved)
        );
    }

    @Transactional
    public WeeklyMenuEntryResponseDto upsertEntry(Long userId, LocalDate requestedDate, UpsertWeekMenuEntryRequestDto requestDto) {
        LocalDate weekStart = resolveWeekStart(requestedDate);
        WeekMenuEntryRequestDto entryDto = requestDto.entry();
        ensureDateInsideWeek(weekStart, entryDto.plannedDate());

        Meal meal = mealService.getMealEntity(entryDto.mealId(), requestDto.coupleId());

        WeeklyMenuEntry entry = weeklyMenuEntryRepository
                .findByCoupleIdAndPlannedDateAndMealType(requestDto.coupleId(), entryDto.plannedDate(), entryDto.mealType())
                .orElseGet(WeeklyMenuEntry::new);

        LocalDateTime now = now();
        if (entry.getId() == null) {
            entry.setCreatedAt(now);
        }

        entry.setCoupleId(requestDto.coupleId());
        entry.setMeal(meal);
        entry.setPlannedDate(entryDto.plannedDate());
        entry.setMealType(entryDto.mealType());
        entry.setServings(entryDto.servings());
        entry.setNote(trimToNull(entryDto.note()));
        entry.setUpdatedByUserId(userId);
        entry.setUpdatedAt(now);

        return toDto(weeklyMenuEntryRepository.save(entry));
    }

    @Transactional
    public void deleteEntry(Long entryId, Long coupleId) {
        WeeklyMenuEntry entry = weeklyMenuEntryRepository.findById(entryId)
                .filter(found -> found.getCoupleId().equals(coupleId))
                .orElseThrow(() -> new ru.iposhka.exception.NotFoundException("Запись меню не найдена"));
        weeklyMenuEntryRepository.delete(entry);
    }

    private List<WeeklyMenuEntry> saveEntries(Long userId, Long coupleId, List<WeekMenuEntryRequestDto> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = now();
        List<WeeklyMenuEntry> entities = entries.stream()
                .map(entryDto -> WeeklyMenuEntry.builder()
                        .coupleId(coupleId)
                        .meal(mealService.getMealEntity(entryDto.mealId(), coupleId))
                        .plannedDate(entryDto.plannedDate())
                        .mealType(entryDto.mealType())
                        .servings(entryDto.servings())
                        .note(trimToNull(entryDto.note()))
                        .updatedByUserId(userId)
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();

        return weeklyMenuEntryRepository.saveAll(entities);
    }

    private void validateEntriesBelongToWeek(LocalDate weekStart, List<WeekMenuEntryRequestDto> entries) {
        if (entries == null) {
            return;
        }

        for (WeekMenuEntryRequestDto entry : entries) {
            ensureDateInsideWeek(weekStart, entry.plannedDate());
        }
    }

    private void ensureDateInsideWeek(LocalDate weekStart, LocalDate plannedDate) {
        LocalDate weekEnd = weekStart.plusDays(6);
        if (plannedDate.isBefore(weekStart) || plannedDate.isAfter(weekEnd)) {
            throw new BadRequestException("Дата блюда должна попадать в выбранную неделю");
        }
    }

    private LocalDate resolveWeekStart(LocalDate requestedDate) {
        return requestedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private List<ShoppingListItemResponseDto> buildShoppingList(List<WeeklyMenuEntry> entries) {
        Map<String, ShoppingAccumulator> items = new LinkedHashMap<>();

        for (WeeklyMenuEntry entry : entries) {
            for (MealIngredient ingredient : entry.getMeal().getIngredients()) {
                String key = ingredient.getName().trim().toLowerCase() + "|" + ingredient.getUnit().trim().toLowerCase();
                ShoppingAccumulator accumulator = items.computeIfAbsent(
                        key,
                        ignored -> new ShoppingAccumulator(ingredient.getName().trim(), ingredient.getUnit().trim(), BigDecimal.ZERO)
                );
                accumulator.totalAmount = accumulator.totalAmount.add(
                        ingredient.getAmount().multiply(BigDecimal.valueOf(entry.getServings()))
                );
            }
        }

        return items.values().stream()
                .sorted(Comparator.comparing(acc -> acc.name))
                .map(acc -> new ShoppingListItemResponseDto(acc.name, acc.totalAmount.stripTrailingZeros(), acc.unit))
                .toList();
    }

    private WeeklyMenuEntryResponseDto toDto(WeeklyMenuEntry entry) {
        Meal meal = entry.getMeal();
        MealResponseDto mealDto = new MealResponseDto(
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

        return new WeeklyMenuEntryResponseDto(
                entry.getId(),
                entry.getPlannedDate(),
                entry.getMealType(),
                entry.getServings(),
                entry.getNote(),
                entry.getUpdatedByUserId(),
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                mealDto
        );
    }

    private Comparator<WeeklyMenuEntry> entryComparator() {
        return Comparator.comparing(WeeklyMenuEntry::getPlannedDate)
                .thenComparing(WeeklyMenuEntry::getMealType);
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

    private static final class ShoppingAccumulator {
        private final String name;
        private final String unit;
        private BigDecimal totalAmount;

        private ShoppingAccumulator(String name, String unit, BigDecimal totalAmount) {
            this.name = name;
            this.unit = unit;
            this.totalAmount = totalAmount;
        }
    }
}