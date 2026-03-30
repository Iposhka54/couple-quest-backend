package ru.iposhka.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSignUpRequestDto {
    @Email(message = "Некорректный формат email")
    @Size(max = 64, message = "Емаил должен быть не больше 128 символов")
    private String email;
    @Size(min = 8, max = 128, message = "Пароль должен быть не менее 8 и не больше 128 символов")
    private String password;

    @NotNull(message = "Имя должно быть заполнено")
    @Size(min = 2, max = 64, message = "Имя должно быть не менее 2 и не больше 64 символов")
    private String name;
    @NotNull(message = "Гендер должен быть заполнен")
    @Pattern(regexp = "MALE|FEMALE", message = "Пол должен быть MALE или FEMALE")
    private String gender;
}