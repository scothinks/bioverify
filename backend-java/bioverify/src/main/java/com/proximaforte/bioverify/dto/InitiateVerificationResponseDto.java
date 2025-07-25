// FILE: src/main/java/com/proximaforte/bioverify/dto/InitiateVerificationResponseDto.java
package com.proximaforte.bioverify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateVerificationResponseDto {
    private String nextStep; // Will be "CHALLENGE_PSN" or "CREATE_ACCOUNT"
    private UUID recordId;
}