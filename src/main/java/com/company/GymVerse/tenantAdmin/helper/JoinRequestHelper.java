package com.company.GymVerse.tenantAdmin.helper;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ForbiddenException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantMembershipStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.repositories.central.UserTenantRoleRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

/**
 * Coordinates cross-database validation, SaaS member quota checks, unique member ID generation,
 * and central role provisioning during join request approval.
 */
@Component
public class JoinRequestHelper {

    private final GymTenantRepository gymTenantRepository;
    private final GymSubscriptionRepository gymSubscriptionRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final MemberRepository memberRepository;
    private final PlatformTransactionManager centralTransactionManager;

    public JoinRequestHelper(
            final GymTenantRepository gymTenantRepository,
            final GymSubscriptionRepository gymSubscriptionRepository,
            final UserAccountRepository userAccountRepository,
            final UserTenantRoleRepository userTenantRoleRepository,
            final MemberRepository memberRepository,
            @Qualifier("centralTransactionManager") final PlatformTransactionManager centralTransactionManager
    ) {
        this.gymTenantRepository = gymTenantRepository;
        this.gymSubscriptionRepository = gymSubscriptionRepository;
        this.userAccountRepository = userAccountRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
        this.memberRepository = memberRepository;
        this.centralTransactionManager = centralTransactionManager;
    }

    public record CentralApprovalContext(UserAccount userAccount, GymTenant gymTenant) {
    }

    /**
     * Verifies central gym subscription health, max_members quota, applicant existence,
     * and absence of an existing ACTIVE role in this gym.
     */
    public CentralApprovalContext validateAndResolveCentralContext(
            final Long tenantId,
            final Long centralUserId,
            final long currentMemberCount
    ) {
        TransactionTemplate readTx = new TransactionTemplate(centralTransactionManager);
        readTx.setReadOnly(true);

        return readTx.execute(status -> {
            GymTenant gymTenant = gymTenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Gym tenant not found with ID: " + tenantId));

            GymSubscription subscription = gymSubscriptionRepository
                    .findFirstByGymTenantIdOrderBySubscribedAtDesc(tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("No SaaS subscription found for tenant ID: " + tenantId));

            if (subscription.getStatus() == GymSubscriptionStatus.EXPIRED
                    || subscription.getStatus() == GymSubscriptionStatus.CANCELLED) {
                throw new ForbiddenException("Cannot approve join requests while gym SaaS subscription is " + subscription.getStatus());
            }

            if (subscription.getPlan() != null && currentMemberCount >= subscription.getPlan().getMaxMembers()) {
                throw new ForbiddenException("Gym has reached its SaaS plan member limit (" + subscription.getPlan().getMaxMembers() + ")");
            }

            UserAccount applicant = userAccountRepository.findById(centralUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Central user account not found for ID: " + centralUserId));

            List<UserTenantRole> existingRoles = userTenantRoleRepository
                    .findAllByUserAccountIdAndGymTenantId(centralUserId, tenantId);
            boolean alreadyActive = existingRoles.stream()
                    .anyMatch(r -> r.getStatus() == TenantMembershipStatus.ACTIVE);
            if (alreadyActive) {
                throw new BadRequestException("User already holds an active role in this gym");
            }

            return new CentralApprovalContext(applicant, gymTenant);
        });
    }

    /**
     * Grants or activates the MEMBER role in central user_tenant_roles.
     */
    public void persistCentralMemberRole(final CentralApprovalContext context) {
        TransactionTemplate writeTx = new TransactionTemplate(centralTransactionManager);
        writeTx.executeWithoutResult(status -> {
            UserTenantRole role = userTenantRoleRepository
                    .findByUserAccountIdAndGymTenantIdAndRole(
                            context.userAccount().getId(),
                            context.gymTenant().getId(),
                            TenantRole.MEMBER
                    )
                    .orElseGet(() -> UserTenantRole.builder()
                            .userAccount(context.userAccount())
                            .gymTenant(context.gymTenant())
                            .role(TenantRole.MEMBER)
                            .adminAuthority(AdminAuthority.NONE)
                            .build());

            role.setStatus(TenantMembershipStatus.ACTIVE);
            userTenantRoleRepository.save(role);
        });
    }

    /**
     * Generates a unique tenant-scoped member badge ID (MEM-XXXXXXXX).
     */
    public String generateUniqueMemberGymId() {
        for (int i = 0; i < 20; i++) {
            String candidate = "MEM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            if (!memberRepository.existsByGymId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate unique member gymId");
    }
}
