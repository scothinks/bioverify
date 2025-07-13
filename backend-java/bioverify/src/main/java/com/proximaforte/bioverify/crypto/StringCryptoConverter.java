package com.proximaforte.bioverify.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.beans.factory.annotation.Value;

/**
 * A JPA AttributeConverter that automatically encrypts and decrypts entity string fields.
 * This ensures that sensitive data is stored encrypted in the database ("at rest").
 */
@Converter
public class StringCryptoConverter implements AttributeConverter<String, String> {

    private final AES256TextEncryptor textEncryptor;

    // We inject the encryption secret from our properties file.
    public StringCryptoConverter(@Value("${application.security.encryption.secret}") String encryptionSecret) {
        textEncryptor = new AES256TextEncryptor();
        textEncryptor.setPassword(encryptionSecret);
    }

    /**
     * Called when writing data TO the database.
     * Takes the plain text string and returns the encrypted version.
     * @param plainText The original string (e.g., "Bello Adekunle").
     * @return The encrypted string.
     */
    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null) {
            return null;
        }
        return textEncryptor.encrypt(plainText);
    }

    /**
     * Called when reading data FROM the database.
     * Takes the encrypted string and returns the decrypted, plain text version.
     * @param encryptedText The encrypted string from the database column.
     * @return The decrypted, original string.
     */
    @Override
    public String convertToEntityAttribute(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        try {
            return textEncryptor.decrypt(encryptedText);
        } catch (Exception e) {
            // Handle cases where data might not be encrypted (e.g., old data)
            // Or if the decryption key is wrong.
            // For this app, we'll return the raw value, but in production you might log an error.
            return encryptedText;
        }
    }
}
