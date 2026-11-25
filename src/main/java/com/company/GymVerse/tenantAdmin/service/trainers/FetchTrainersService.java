package com.company.GymVerse.tenantAdmin.service.trainers;

import com.company.GymVerse.models.tenant.Trainer;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.TrainerRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.trainers.FetchTrainersRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.trainers.TrainerItemResponse;
import com.company.GymVerse.tenantAdmin.specifications.trainers.TrainerSpecification;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates paginated, sorted, and JPA Specification-filtered retrieval of the trainer address book.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchTrainersService implements TenantAdminOperation {

    private final TrainerRepository trainerRepository;
    private final MemberRepository memberRepository;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_TRAINERS;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_TRAINERS operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        FetchTrainersRequest filter = request.getFetchTrainersRequest();
        Pageable pageable = validations.validateFetchTrainers(filter);

        Specification<Trainer> spec = TrainerSpecification.withFilters(
                filter != null ? filter.getActive() : null,
                filter != null ? filter.getSpecialization() : null,
                filter != null ? filter.getSearch() : null
        );

        Page<Trainer> trainerPage = trainerRepository.findAll(spec, pageable);
        Map<Long, Long> assignedMemberCountsByUserId = resolveAssignedMemberCounts(trainerPage.getContent());

        List<TrainerItemResponse> items = trainerPage.getContent().stream()
                .map(trainer -> {
                    Long userId = trainer.getUser() != null ? trainer.getUser().getId() : null;
                    long assignedCount = userId != null ? assignedMemberCountsByUserId.getOrDefault(userId, 0L) : 0L;
                    return mapper.toTrainerItem(trainer, assignedCount);
                })
                .toList();

        return mapper.toPagedTrainersResponse("Trainers retrieved successfully", trainerPage, items);
    }

    private Map<Long, Long> resolveAssignedMemberCounts(final List<Trainer> trainers) {
        if (trainers == null || trainers.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> trainerUserIds = trainers.stream()
                .map(Trainer::getUser)
                .filter(u -> u != null && u.getId() != null)
                .map(u -> u.getId())
                .distinct()
                .toList();
        if (trainerUserIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> rows = memberRepository.countAssignedMembersByTrainerUserIds(trainerUserIds);
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] instanceof Number userIdNum && row[1] instanceof Number countNum) {
                counts.put(userIdNum.longValue(), countNum.longValue());
            }
        }
        return counts;
    }
}
