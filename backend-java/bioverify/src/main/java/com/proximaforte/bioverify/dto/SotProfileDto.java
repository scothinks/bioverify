package com.proximaforte.bioverify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SotProfileDto {
    private String ssid;
    private String nin;
    // This would be expanded with nested classes to fully model the SoT response
}