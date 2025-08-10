package com.codingr.nfclib.schema;

/**
 * Interface that command ID enums must implement to provide
 * APDU command identification and basic command structure.
 */
public interface CommandId {
    /**
     * @return The unique identifier for this command
     */
    String getId();
    
    /**
     * @return The CLA (Class) byte for this command
     */
    byte getCLA();
    
    /**
     * @return The INS (Instruction) byte for this command
     */
    byte getINS();
}