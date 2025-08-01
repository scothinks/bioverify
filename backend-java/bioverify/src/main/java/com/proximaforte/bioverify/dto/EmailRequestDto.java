package com.proximaforte.bioverify.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class EmailRequestDto {
    private String clientId;
    private String templateName;
    private Map<String, String> parameters;
    private String toEmail;
    private String toPhoneNumber;
    private boolean sendEmail;
    private boolean sendSms;
}