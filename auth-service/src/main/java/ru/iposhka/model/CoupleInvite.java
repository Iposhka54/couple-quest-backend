package ru.iposhka.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "couple_invite")
public class CoupleInvite {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "inviter_id")
    private User inviter;

    @Column(unique = true)
    private String token;

    private LocalDateTime expiresAt;

    private Gender expectedGender;

    @Enumerated
    @Builder.Default
    private InviteStatus status = InviteStatus.ACTIVE;

    private LocalDateTime createdAt;
}