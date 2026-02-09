package ru.iposhka.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class JwtResponseDto {
    private String accessToken;
    private String refreshToken;
}
