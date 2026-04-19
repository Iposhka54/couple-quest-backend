package ru.iposhka.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.iposhka.model.AuthStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private AuthStatus status;
    private String accessToken;
    private String email;
    private Boolean emailVerified;
    private Long resendAvailableInSeconds;
}