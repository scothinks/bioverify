package com.proximaforte.bioverify.service.impl;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.service.LivenessCheckService;
// import com.proximaforte.bioverify.service.LivenessDetectionService; // For future real implementation
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
public class LivenessCheckServiceImpl implements LivenessCheckService {

    private final MasterListRecordRepository recordRepository;
    // In a real implementation, you would inject a LivenessDetectionService here.
    // private final LivenessDetectionService livenessDetectionService;

    public LivenessCheckServiceImpl(MasterListRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Override
    @Transactional
    public void processLivenessCheck(User user, MultipartFile livenessVideo) {
        MasterListRecord record = recordRepository.findByUserId(user.getId())
            .orElseThrow(() -> new IllegalStateException("No master record associated with this user."));

        if (record.getStatus() != RecordStatus.ACTIVE) {
            throw new IllegalStateException("User is not in an ACTIVE state and cannot perform a liveness check.");
        }

        // --- Liveness Detection Logic ---
        // In a real application, you would send the livenessVideo to a third-party service here.
        // boolean isLive = livenessDetectionService.isUserLive(livenessVideo);
        
        // For our current e2e testing, we will assume the check always passes.
        boolean isLive = true; 

        if (isLive) {
            // If the check passes, update the record's liveness dates.
            record.setLastLivenessCheckDate(LocalDate.now());
            record.setNextLivenessCheckDate(LocalDate.now().plusMonths(3)); // Schedule next check for 3 months
            recordRepository.save(record);
        } else {
            // If the check fails, the user can try again until their due date.
            // The nightly scheduler will handle moving them to SUSPENDED if the deadline is missed.
            throw new RuntimeException("Liveness check failed. Please try again.");
        }
    }
}