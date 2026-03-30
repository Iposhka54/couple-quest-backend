package ru.iposhka.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.iposhka.dto.response.CoupleInviteResponseDto;
import ru.iposhka.dto.response.CoupleStateResponseDto;
import ru.iposhka.service.CoupleService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/couple")
public class CoupleController {

    private final CoupleService coupleService;

    @GetMapping("/invite")
    public ResponseEntity<CoupleInviteResponseDto> getInvite(
            @RequestHeader("X-Auth-User-Id") Long userId) {
        return ResponseEntity.ok(coupleService.getInvite(userId));
    }

    @PostMapping("/invite")
    public ResponseEntity<CoupleInviteResponseDto> createOrGetInvite(
            @RequestHeader("X-Auth-User-Id") Long userId) {
        return ResponseEntity.ok(coupleService.createOrGetInvite(userId));
    }

    @PostMapping("/invite/accept")
    public ResponseEntity<CoupleStateResponseDto> acceptInvite(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @RequestParam("invite") String token) {
        return ResponseEntity.ok(coupleService.acceptInvite(userId, token));
    }

    @DeleteMapping("/invite")
    public ResponseEntity<Void> revokeCurrentInvite(@RequestHeader("X-Auth-User-Id") Long userId) {
        coupleService.revokeCurrentInvite(userId);
        return ResponseEntity.noContent().build();
    }
}