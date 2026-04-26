package ru.iposhka.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
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
import ru.iposhka.dto.request.ReplaceWeekMenuRequestDto;
import ru.iposhka.dto.request.UpsertWeekMenuEntryRequestDto;
import ru.iposhka.dto.response.WeekMenuResponseDto;
import ru.iposhka.dto.response.WeeklyMenuEntryResponseDto;
import ru.iposhka.service.WeekMenuService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/menu")
public class WeekMenuController {

    private final WeekMenuService weekMenuService;

    @GetMapping("/weeks/{weekStart}")
    public ResponseEntity<WeekMenuResponseDto> getWeek(
            @PathVariable LocalDate weekStart,
            @RequestParam Long coupleId
    ) {
        return ResponseEntity.ok(weekMenuService.getWeek(coupleId, weekStart));
    }

    @PutMapping("/weeks/{weekStart}")
    public ResponseEntity<WeekMenuResponseDto> replaceWeek(
            @PathVariable LocalDate weekStart,
            @RequestHeader("X-Auth-User-Id") Long userId,
            @Valid @RequestBody ReplaceWeekMenuRequestDto requestDto
    ) {
        return ResponseEntity.ok(weekMenuService.replaceWeek(userId, weekStart, requestDto));
    }

    @PostMapping("/weeks/{weekStart}/entries")
    public ResponseEntity<WeeklyMenuEntryResponseDto> upsertEntry(
            @PathVariable LocalDate weekStart,
            @RequestHeader("X-Auth-User-Id") Long userId,
            @Valid @RequestBody UpsertWeekMenuEntryRequestDto requestDto
    ) {
        return ResponseEntity.ok(weekMenuService.upsertEntry(userId, weekStart, requestDto));
    }

    @DeleteMapping("/weeks/{weekStart}/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable LocalDate weekStart,
            @PathVariable Long entryId,
            @RequestParam Long coupleId
    ) {
        weekMenuService.deleteEntry(entryId, coupleId);
        return ResponseEntity.noContent().build();
    }
}