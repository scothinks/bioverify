package com.proximaforte.bioverify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkJobStatusDto { // <-- Renamed class
    private String jobId;
    private String status;
    private String message;
    private String fileUrl;
    private String fileHash;
}