package com.example.contact_service.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EncryptionUtilTest {

    @Test
    void testEncryptionAndDecryption() {
        // Given
        String originalText = "Secret Address 123";

        // When
        String encrypted = EncryptionUtil.encrypt(originalText);
        String decrypted = EncryptionUtil.decrypt(encrypted);

        // Then
        Assertions.assertNotEquals(originalText, encrypted, "Encrypted text should not match original");
        Assertions.assertEquals(originalText, decrypted, "Decrypted text should match original");
    }
}