// FILE: src/main/java/com/proximaforte/bioverify/dto/PsnChallengeRequestDto.java
package com.proximaforte.bioverify.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PsnChallengeRequestDto {
    private UUID recordId;
    private String psn;
}