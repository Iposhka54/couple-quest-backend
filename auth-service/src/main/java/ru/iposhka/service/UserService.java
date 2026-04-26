package ru.iposhka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.iposhka.dto.response.CoupleStateResponseDto;
import ru.iposhka.dto.response.PartnerShortDto;
import ru.iposhka.dto.response.UserResponseDto;
import ru.iposhka.exception.NotFoundException;
import ru.iposhka.model.Couple;
import ru.iposhka.model.CoupleStatus;
import ru.iposhka.model.User;
import ru.iposhka.repository.CoupleRepository;
import ru.iposhka.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CoupleRepository coupleRepository;

    @Transactional(readOnly = true)
    public UserResponseDto getInfoAboutUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователя не существует!"));

        CoupleStateResponseDto coupleDto = getCoupleState(user);

        return UserResponseDto.builder()
                .email(user.getEmail())
                .name(user.getName())
                .gender(user.getGender())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .couple(coupleDto)
                .build();
    }

    private CoupleStateResponseDto getCoupleState(User user) {
        return coupleRepository.findActiveByUserId(user.getId(), CoupleStatus.ACTIVE)
                .map(couple -> {
                    User partner = resolvePartner(couple, user.getId());

                    return new CoupleStateResponseDto(
                            couple.getId(),
                            true,
                            new PartnerShortDto(
                                    partner.getName(),
                                    partner.getGender().name()
                            )
                    );
                })
                .orElseGet(() -> new CoupleStateResponseDto(null, false, null));
    }

    private User resolvePartner(Couple couple, Long currentUserId) {
        if (couple.getBoy().getId().equals(currentUserId)) {
            return couple.getGirlfriend();
        }
        return couple.getBoy();
    }
}