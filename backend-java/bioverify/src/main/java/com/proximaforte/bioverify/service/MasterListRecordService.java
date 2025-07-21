package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MasterListRecordService {

    private final MasterListRecordRepository recordRepository;

    @Transactional
    public MasterListRecord updatePsnForRecord(UUID recordId, String newPsn) {
        MasterListRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Record not found with ID: " + recordId));

        // Set the plaintext PSN (the converter will handle encryption)
        record.setPsn(newPsn);
        // Set the hashed PSN
        record.setPsnHash(toSha256(newPsn));

        return recordRepository.save(record);
    }

    private String toSha256(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hash);
            StringBuilder hexString = new StringBuilder(number.toString(16));
            while (hexString.length() < 64) {
                hexString.insert(0, '0');
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find SHA-256 algorithm", e);
        }
    }
}