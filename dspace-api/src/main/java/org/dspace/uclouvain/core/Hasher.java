package org.dspace.uclouvain.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Basic class that can be used to hash data using a specific algorithm.
 */
public class Hasher {
    private MessageDigest digest;
    private String encryptionKey;

    public Hasher(String algorithm, String encryptionKey) throws NoSuchAlgorithmException {
        this(algorithm);
        this.encryptionKey = encryptionKey;
    }

    public Hasher(String algorithm) throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance(algorithm);
    }

    /**
     * Process the hash of a given string.
     * The result depends on the configured algorithm.
     * @param data: The data to hash.
     * @return: The hash of the data.
     */
    public byte[] processHash(String data){
        if (this.encryptionKey != null) {
            this.digest.update(this.encryptionKey.getBytes());
        }
        return digest.digest(data.getBytes());
    }

    /**
     * Process the hash of a given string and return it as a string.
     * The result depends on the configured algorithm.
     * @param data: The data to hash.
     * @return: The hash of the data as a string.
     */
    public String processHashAsString(String data){
        byte[] hash =  this.processHash(data);
        return this.bytesToHexString(hash);

    }

    /**
     * Converts a given byte array to an Hexadecimal string notation.
     * For each byte in the array, append the hexadecimal representation.
     * @param bytes: An array of bytes that need to be converted.
     * @return: A string containing the full hexadecimal notation for the given array.
     */
    private String bytesToHexString(byte[] bytes){
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
