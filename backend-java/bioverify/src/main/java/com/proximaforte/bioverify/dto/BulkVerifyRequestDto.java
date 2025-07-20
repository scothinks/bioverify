package com.proximaforte.bioverify.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class BulkVerifyRequestDto {
    private UUID uploadId;
}