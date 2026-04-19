package ru.iposhka.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.iposhka.model.Gender;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private String email;
    private String name;
    private Gender gender;
    private Boolean emailVerified;
    private CoupleStateResponseDto couple;
}