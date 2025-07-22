package com.proximaforte.bioverify.service;

import java.io.IOException;

/**
 * An interface for abstracting file storage operations.
 * Implementations can be for local disk, cloud storage (S3), etc.
 */
public interface FileStorageService {

    /**
     * Saves file content to the storage system.
     *
     * @param content The byte array of the file content.
     * @param fileName The desired name for the file.
     * @return A unique identifier or path for the saved file.
     */
    String save(byte[] content, String fileName) throws IOException;

    /**
     * Loads a file's content from the storage system.
     *
     * @param fileIdentifier The unique identifier or path of the file to load.
     * @return The byte array of the file content.
     */
    byte[] load(String fileIdentifier) throws IOException;
}