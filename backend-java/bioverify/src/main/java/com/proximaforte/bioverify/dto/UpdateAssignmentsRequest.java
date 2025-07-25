package com.proximaforte.bioverify.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateAssignmentsRequest {
    private List<UUID> ministryIds;
    private List<UUID> departmentIds;
}