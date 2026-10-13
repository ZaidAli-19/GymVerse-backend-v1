package com.company.GymVerse.models.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Facility geofencing and operational hours configuration.
 */
@Entity
@Table(name = "gym_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 10, scale = 8, nullable = false)
    private Double latitude;

    @Column(precision = 11, scale = 8, nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double radius;

    @Builder.Default
    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone = "UTC";

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @Column(name = "auto_checkout_time")
    private LocalTime autoCheckoutTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GymConfig gymConfig = (GymConfig) o;
        return Objects.equals(id, gymConfig.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
