package com.company.GymVerse.models.tenant;

import com.company.GymVerse.models.tenant.enums.PlanTheme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Persisted plan tier definition (default ELITE/LEGENDARY or admin-created custom tier)
 * mapped to a frontend theme key.
 */
@Entity
@Table(name = "plan_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_key", nullable = false, length = 50)
    private PlanTheme themeKey;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean defaultTier = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanTier planTier = (PlanTier) o;
        return Objects.equals(id, planTier.id) || (id == null && Objects.equals(name, planTier.name));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : name);
    }
}
