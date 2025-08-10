package com.codingr.nfclib.schema;

/**
 * Specification for expected APDU responses within a schema.
 * Defines the structure and meaning of possible response types.
 */
public class ApduResponseSpec {
    
    private final String responseId;
    private final String name;
    private final String description;
    private final byte[] statusWord;
    private final Integer minDataLength;
    private final Integer maxDataLength;
    private final boolean isSuccess;
    private final String errorDescription;
    
    private ApduResponseSpec(Builder builder) {
        this.responseId = builder.responseId;
        this.name = builder.name;
        this.description = builder.description;
        this.statusWord = builder.statusWord;
        this.minDataLength = builder.minDataLength;
        this.maxDataLength = builder.maxDataLength;
        this.isSuccess = builder.isSuccess;
        this.errorDescription = builder.errorDescription;
    }
    
    public String getResponseId() { return responseId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public byte[] getStatusWord() { return statusWord; }
    public Integer getMinDataLength() { return minDataLength; }
    public Integer getMaxDataLength() { return maxDataLength; }
    public boolean isSuccess() { return isSuccess; }
    public String getErrorDescription() { return errorDescription; }
    
    /**
     * Checks if the given response bytes match this response specification
     * @param responseBytes The response bytes to check
     * @return true if the response matches this specification
     */
    public boolean matches(byte[] responseBytes) {
        if (responseBytes.length < 2) return false;
        
        // Check status word (last 2 bytes)
        byte[] actualSW = new byte[] {
            responseBytes[responseBytes.length - 2],
            responseBytes[responseBytes.length - 1]
        };
        
        if (!java.util.Arrays.equals(statusWord, actualSW)) return false;
        
        // Check data length constraints
        int dataLength = responseBytes.length - 2;
        if (minDataLength != null && dataLength < minDataLength) return false;
        if (maxDataLength != null && dataLength > maxDataLength) return false;
        
        return true;
    }
    
    /**
     * Extracts the data portion from a response that matches this specification
     * @param responseBytes The complete response bytes
     * @return The data portion (without status word)
     */
    public byte[] extractData(byte[] responseBytes) {
        if (responseBytes.length <= 2) return new byte[0];
        
        byte[] data = new byte[responseBytes.length - 2];
        System.arraycopy(responseBytes, 0, data, 0, data.length);
        return data;
    }
    
    public static class Builder {
        private String responseId;
        private String name;
        private String description;
        private byte[] statusWord;
        private Integer minDataLength;
        private Integer maxDataLength;
        private boolean isSuccess = true;
        private String errorDescription;
        
        public Builder(String responseId, byte[] statusWord) {
            this.responseId = responseId;
            this.statusWord = statusWord;
        }
        
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder dataLength(int min, int max) { 
            this.minDataLength = min; 
            this.maxDataLength = max; 
            return this; 
        }
        public Builder success(boolean isSuccess) { this.isSuccess = isSuccess; return this; }
        public Builder errorDescription(String errorDescription) { 
            this.errorDescription = errorDescription; 
            this.isSuccess = false;
            return this; 
        }
        
        public ApduResponseSpec build() {
            return new ApduResponseSpec(this);
        }
    }
}