package com.company.GymVerse.tenantAdmin.service.trainers;

import com.company.GymVerse.attendance.dto.TrainerAttendanceSummary;
import com.company.GymVerse.attendance.service.AttendanceQueryPort;
import com.company.GymVerse.repositories.tenant.TrainerRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates Trainers mini-dashboard summary retrieval: total/active trainer counts
 * plus stubbed live presence and average attendance rate via AttendanceQueryPort.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchTrainersSummaryService implements TenantAdminOperation {

    private final TrainerRepository trainerRepository;
    private final AttendanceQueryPort attendanceQueryPort;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_TRAINERS_SUMMARY;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_TRAINERS_SUMMARY operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        long totalTrainersCount = trainerRepository.count();
        long activeTrainersCount = trainerRepository.countByActiveTrue();

        // TODO [ATTENDANCE-MODULE]: Live trainer count and avg attendance rate delegate to AttendanceQueryPort.
        TrainerAttendanceSummary attendanceSummary = attendanceQueryPort.getTrainerAttendanceSummary();
        long activeLiveCount = attendanceSummary != null ? attendanceSummary.getActiveLiveTrainersCount() : 0L;
        double avgAttendanceRate = attendanceSummary != null ? attendanceSummary.getAvgAttendanceRate() : 0.0;

        return mapper.toTrainersSummaryResponse(
                "Trainers summary retrieved successfully",
                totalTrainersCount,
                activeTrainersCount,
                activeLiveCount,
                avgAttendanceRate
        );
    }
}
