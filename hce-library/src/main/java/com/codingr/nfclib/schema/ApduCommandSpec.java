package com.codingr.nfclib.schema;

import java.util.List;

/**
 * Specification for an APDU command within a schema.
 * Defines the structure and constraints for a specific command type.
 */
public class ApduCommandSpec {
    
    private final String commandId;
    private final String name;
    private final String description;
    private final byte cla;
    private final byte ins;
    private final Byte p1; // null means any value allowed
    private final Byte p2; // null means any value allowed
    private final Integer minDataLength;
    private final Integer maxDataLength;
    private final Integer expectedLe; // null means Le is optional
    private final List<ApduResponseSpec> possibleResponses;
    
    private ApduCommandSpec(Builder builder) {
        this.commandId = builder.commandId;
        this.name = builder.name;
        this.description = builder.description;
        this.cla = builder.cla;
        this.ins = builder.ins;
        this.p1 = builder.p1;
        this.p2 = builder.p2;
        this.minDataLength = builder.minDataLength;
        this.maxDataLength = builder.maxDataLength;
        this.expectedLe = builder.expectedLe;
        this.possibleResponses = builder.possibleResponses;
    }
    
    public String getCommandId() { return commandId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public byte getCla() { return cla; }
    public byte getIns() { return ins; }
    public Byte getP1() { return p1; }
    public Byte getP2() { return p2; }
    public Integer getMinDataLength() { return minDataLength; }
    public Integer getMaxDataLength() { return maxDataLength; }
    public Integer getExpectedLe() { return expectedLe; }
    public List<ApduResponseSpec> getPossibleResponses() { return possibleResponses; }
    
    /**
     * Checks if the given APDU bytes match this command specification
     * @param apduBytes The APDU command to check
     * @return true if the APDU matches this specification
     */
    public boolean matches(byte[] apduBytes) {
        if (apduBytes.length < 4) return false;
        
        if (apduBytes[0] != cla || apduBytes[1] != ins) return false;
        
        if (p1 != null && apduBytes[2] != p1) return false;
        if (p2 != null && apduBytes[3] != p2) return false;
        
        // Validate data length constraints
        if (apduBytes.length > 4) {
            int dataLength = apduBytes.length - 4;
            if (expectedLe != null) dataLength -= 1; // Account for Le byte
            
            if (minDataLength != null && dataLength < minDataLength) return false;
            if (maxDataLength != null && dataLength > maxDataLength) return false;
        }
        
        return true;
    }
    
    /**
     * Creates the command prefix bytes (CLA INS P1 P2) for this command
     * @return byte array with command header
     */
    public byte[] getCommandPrefix() {
        return new byte[] {
            cla, 
            ins, 
            p1 != null ? p1 : 0x00, 
            p2 != null ? p2 : 0x00
        };
    }
    
    public static class Builder {
        private String commandId;
        private String name;
        private String description;
        private byte cla;
        private byte ins;
        private Byte p1;
        private Byte p2;
        private Integer minDataLength;
        private Integer maxDataLength;
        private Integer expectedLe;
        private List<ApduResponseSpec> possibleResponses;
        
        public Builder(String commandId, byte cla, byte ins) {
            this.commandId = commandId;
            this.cla = cla;
            this.ins = ins;
        }
        
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder p1(byte p1) { this.p1 = p1; return this; }
        public Builder p2(byte p2) { this.p2 = p2; return this; }
        public Builder dataLength(int min, int max) { 
            this.minDataLength = min; 
            this.maxDataLength = max; 
            return this; 
        }
        public Builder expectedLe(int le) { this.expectedLe = le; return this; }
        public Builder possibleResponses(List<ApduResponseSpec> responses) { 
            this.possibleResponses = responses; 
            return this; 
        }
        
        public ApduCommandSpec build() {
            return new ApduCommandSpec(this);
        }
    }
}