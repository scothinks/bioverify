package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LivenessCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LivenessCheckScheduler.class);
    private final MasterListRecordRepository recordRepository;
    // In a real application, you would also inject a NotificationService here.

    public LivenessCheckScheduler(MasterListRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    /**
     * Runs every night at 1:00 AM server time.
     * This job finds all active users whose liveness check is due in exactly 14 days
     * and triggers a notification for them.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void notifyUsersForUpcomingLivenessCheck() {
        LocalDate notificationDate = LocalDate.now().plusDays(14);
        logger.info("Scheduler running: Checking for liveness checks due on {}", notificationDate);

        List<MasterListRecord> recordsToNotify = recordRepository.findAllByStatusAndNextLivenessCheckDate(
            RecordStatus.ACTIVE,
            notificationDate
        );

        if (recordsToNotify.isEmpty()) {
            logger.info("No users to notify for upcoming liveness checks.");
            return;
        }

        logger.info("Found {} users to notify for upcoming liveness check.", recordsToNotify.size());
        for (MasterListRecord record : recordsToNotify) {
            // Future enhancement: Integrate with NotificationService to send email or in-app alerts
            logger.info("Notifying user {} for record {}", record.getUser().getEmail(), record.getId());
        }
    }

    /**
     * Runs every night at 2:00 AM server time.
     * This job finds all active users whose liveness check due date has passed
     * and moves their account to an INACTIVE state.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void suspendUsersWithOverdueLivenessCheck() {
        LocalDate today = LocalDate.now();
        logger.info("Scheduler running: Checking for overdue liveness checks before {}", today);

        List<MasterListRecord> recordsToSuspend = recordRepository.findAllByStatusAndNextLivenessCheckDateBefore(
            RecordStatus.ACTIVE,
            today
        );

        if (recordsToSuspend.isEmpty()) {
            logger.info("No users to suspend for overdue liveness checks.");
            return;
        }

        logger.warn("Found {} users to suspend for overdue liveness checks.", recordsToSuspend.size());
        for (MasterListRecord record : recordsToSuspend) {
            logger.warn("Setting account to INACTIVE for user {} (Record ID: {})", record.getUser().getEmail(), record.getId());
            record.setStatus(RecordStatus.INACTIVE);
        }

        recordRepository.saveAll(recordsToSuspend);
    }
}