package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.Ministry;
import lombok.Data;
import java.util.UUID;

@Data
public class MinistryDto {
    private UUID id;
    private String name;

    public MinistryDto(Ministry ministry) {
        this.id = ministry.getId();
        this.name = ministry.getName();
    }
}