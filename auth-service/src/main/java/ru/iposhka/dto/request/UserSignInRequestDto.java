package ru.iposhka.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSignInRequestDto {
    @Email(message = "Некорректный формат email")
    @Size(max = 64, message = "Емаил должен быть не больше 128 символов")
    private String email;
    @Size(min = 8, max = 128, message = "Пароль должен быть не менее 8 и не больше 128 символов")
    private String password;
}