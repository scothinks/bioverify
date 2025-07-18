package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.MasterListRecordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    @GetMapping("/me/record")
    public ResponseEntity<MasterListRecordDto> getCurrentUserRecord(@AuthenticationPrincipal User user) {
        MasterListRecord record = user.getMasterListRecord();
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new MasterListRecordDto(record));
    }
}