package com.codingr.nfclib.schema;

import java.util.List;

/**
 * Defines a complete APDU protocol schema with commands and expected responses.
 * This interface provides structure on top of the existing annotation-based system,
 * allowing developers to define protocols declaratively while maintaining full control
 * over business logic in handlers.
 */
public interface ApduSchema {
    
    /**
     * @return The schema name/identifier
     */
    String getName();
    
    /**
     * @return The version of this schema
     */
    String getVersion();
    
    /**
     * @return List of AIDs this schema supports
     */
    String[] getSupportedAids();
    
    /**
     * @return All command definitions in this schema
     */
    List<ApduCommandSpec> getCommands();
    
    /**
     * Finds a command specification by its identifier
     * @param commandId The command identifier
     * @return The command specification or null if not found
     */
    ApduCommandSpec getCommand(String commandId);
    
    /**
     * Finds a command specification by matching APDU bytes
     * @param apduBytes The APDU command bytes
     * @return The matching command specification or null if not found
     */
    ApduCommandSpec findCommandByApdu(byte[] apduBytes);
}