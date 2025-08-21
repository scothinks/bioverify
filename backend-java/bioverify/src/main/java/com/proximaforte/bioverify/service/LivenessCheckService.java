package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.User;
import org.springframework.web.multipart.MultipartFile;

public interface LivenessCheckService {

    /**
     * Processes a liveness check submission for a given user.
     *
     * @param user The currently authenticated user submitting the check.
     * @param livenessVideo The video or image data for the liveness check.
     */
    void processLivenessCheck(User user, MultipartFile livenessVideo);
}