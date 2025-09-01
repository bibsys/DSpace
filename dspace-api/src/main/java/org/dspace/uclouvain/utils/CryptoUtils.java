/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.NotImplementedException;

/**
 * Utility class used to encrypt/decrypt data
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class CryptoUtils {

    protected CryptoUtils() {
        throw new NotImplementedException();
    }

    /**
     * Encrypts a plaintext string using AES encryption with a key derived from the given password and salt.
     * The result includes a randomly generated IV (initialization vector) prepended to the encrypted data,
     * and returns a hexadecimal string.
     *
     * @param data The plaintext string to encrypt.
     * @param password The password used to derive the encryption key.
     * @param salt A byte array used as salt for key derivation.
     * @return A hexadecimal string representing the IV + encrypted data.
     * @throws Exception If encryption fails due to invalid parameters or cryptographic errors.
     */
    public static String encrypt(String data, String password, String salt) throws Exception {
        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // concat IV + encrypted data
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return bytesToHex(combined);
    }

    /**
     * Decrypts a hexadecimal string encrypted using the encrypt method.
     * Extracts the IV from the input, derives the key using the password and salt, and decrypts the data.
     *
     * @param hexData The hexadecimal string containing the IV + encrypted data.
     * @param password The password used to derive the decryption key.
     * @param salt A byte array used as salt for key derivation.
     * @return The original plaintext string.
     * @throws Exception If decryption fails due to invalid parameters or cryptographic errors.
     */

    public static String decrypt(String hexData, String password, String salt) throws Exception {
        byte[] combined = hexToBytes(hexData);
        byte[] iv = new byte[16];
        byte[] encrypted = new byte[combined.length - 16];

        System.arraycopy(combined, 0, iv, 0, 16);
        System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }


    /**
     * Derives a SecretKey for AES encryption using PBKDF2 with HMAC-SHA256.
     *
     * @param password The password to derive the key from.
     * @param salt A byte array used as salt.
     * @return A SecretKey suitable for AES encryption.
     * @throws Exception If key derivation fails.
     */
    private static SecretKey deriveKey(String password, String salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }


    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes: The byte array to convert.
     * @return A hexadecimal string representation of the byte array.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    /**
     * Converts a hexadecimal string to a byte array.
     * The hexadecimal string must be an even number (2 characters = 1 byte)
     *
     * @param hex: The hexadecimal string to convert.
     * @return A byte array corresponding to the hexadecimal input.
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

}
