package com.proximaforte.bioverify.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface ProofOfLifeService {

    /**
     * Completes the Proof of Life process for a given record.
     * This includes validating documents, saving files, creating a user account,
     * and activating or flagging the record.
     *
     * @param recordId The UUID of the master list record.
     * @param email The agent-verified email address for the user.
     * @param photo The captured live photo of the user.
     * @param letterOfEmployment The uploaded Letter of Employment file.
     * @param workId The uploaded Work ID file.
     */
    void completePoL(UUID recordId, String email, MultipartFile photo, MultipartFile letterOfEmployment, MultipartFile workId);
}