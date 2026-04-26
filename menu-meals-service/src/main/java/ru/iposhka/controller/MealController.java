package ru.iposhka.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.iposhka.dto.request.MealUpsertRequestDto;
import ru.iposhka.dto.response.MealResponseDto;
import ru.iposhka.service.MealService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/meals")
public class MealController {

    private final MealService mealService;

    @GetMapping
    public ResponseEntity<List<MealResponseDto>> getMeals(@RequestParam Long coupleId) {
        return ResponseEntity.ok(mealService.getMeals(coupleId));
    }

    @GetMapping("/{mealId}")
    public ResponseEntity<MealResponseDto> getMeal(@PathVariable Long mealId, @RequestParam Long coupleId) {
        return ResponseEntity.ok(mealService.getMeal(mealId, coupleId));
    }

    @PostMapping
    public ResponseEntity<MealResponseDto> createMeal(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @Valid @RequestBody MealUpsertRequestDto requestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealService.createMeal(userId, requestDto));
    }

    @PutMapping("/{mealId}")
    public ResponseEntity<MealResponseDto> updateMeal(
            @PathVariable Long mealId,
            @RequestHeader("X-Auth-User-Id") Long userId,
            @Valid @RequestBody MealUpsertRequestDto requestDto
    ) {
        return ResponseEntity.ok(mealService.updateMeal(mealId, userId, requestDto));
    }

    @DeleteMapping("/{mealId}")
    public ResponseEntity<Void> deleteMeal(@PathVariable Long mealId, @RequestParam Long coupleId) {
        mealService.deleteMeal(mealId, coupleId);
        return ResponseEntity.noContent().build();
    }
}