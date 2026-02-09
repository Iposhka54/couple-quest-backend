package ru.iposhka.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.List;
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
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = "boy_id"),
    @UniqueConstraint(columnNames = "girlfriend_id")
})
public class Couple {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "boy_id", nullable = false)
    private User boy;

    @OneToOne
    @JoinColumn(name = "girlfriend_id", nullable = false)
    private User girlfriend;

    @Enumerated
    @Builder.Default
    private CoupleStatus status = CoupleStatus.ACTIVE;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "couple", fetch = FetchType.LAZY)
    private List<CoupleInvite> invites;
}