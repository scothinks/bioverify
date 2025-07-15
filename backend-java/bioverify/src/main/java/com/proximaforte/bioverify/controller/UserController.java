package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.MasterListRecordDto;
import com.proximaforte.bioverify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me/record")
    public ResponseEntity<MasterListRecordDto> getCurrentUserRecord(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        MasterListRecord record = user.getMasterListRecord();

        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        // Map the entity to a DTO to avoid serialization issues
        MasterListRecordDto recordDto = new MasterListRecordDto(
                record.getId(),
                record.getFullName(),
                record.getBusinessUnit(),
                record.getGradeLevel(),
                record.getStatus(),
                record.getCreatedAt()
        );

        return ResponseEntity.ok(recordDto);
    }
}