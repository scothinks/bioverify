package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.Department;
import lombok.Data;
import java.util.UUID;

@Data
public class DepartmentDto {
    private UUID id;
    private String name;

    public DepartmentDto(Department department) {
        this.id = department.getId();
        this.name = department.getName();
    }
}