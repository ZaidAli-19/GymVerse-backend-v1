package com.company.GymVerse.models.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Reusable gym facility entity (e.g., Weightlifting, Cardio, Sauna) created and assigned to plans.
 */
@Entity
@Table(name = "gym_facilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

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
        GymFacility that = (GymFacility) o;
        return Objects.equals(id, that.id) || (id == null && Objects.equals(name, that.name));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : name);
    }
}
