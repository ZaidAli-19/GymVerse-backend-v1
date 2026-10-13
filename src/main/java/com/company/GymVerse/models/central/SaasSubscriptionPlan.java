package com.company.GymVerse.models.central;

import com.company.GymVerse.models.central.enums.BillingCycle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * SaaS subscription plan defining platform pricing and feature entitlements.
 */
@Entity
@Table(name = "saas_subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasSubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 30)
    private BillingCycle billingCycle;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @Column(name = "max_trainers", nullable = false)
    private Integer maxTrainers;

    @Builder.Default
    @Column(name = "analytics_enabled", nullable = false)
    private boolean analyticsEnabled = false;

    @Builder.Default
    @Column(name = "multi_trainer_enabled", nullable = false)
    private boolean multiTrainerEnabled = false;

    @Builder.Default
    @Column(name = "custom_branding_enabled", nullable = false)
    private boolean customBrandingEnabled = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaasSubscriptionPlan that = (SaasSubscriptionPlan) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
