package com.proximaforte.bioverify.service;

import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

    private final BasicTextEncryptor textEncryptor;

    // This is the only line that changes.
    public EncryptionService(@Value("${application.security.encryption.secret}") String encryptionPassword) {
        this.textEncryptor = new BasicTextEncryptor();
        this.textEncryptor.setPassword(encryptionPassword);
    }

    public String encrypt(String data) {
        return textEncryptor.encrypt(data);
    }

    public String decrypt(String encryptedData) {
        return textEncryptor.decrypt(encryptedData);
    }
}