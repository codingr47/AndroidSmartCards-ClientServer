package com.codingr.nfclib.schema;

import com.codingr.nfclib.hce.core.ApduResponse;
import com.codingr.nfclib.hce.core.BaseApduController;

/**
 * Base class for controllers that use schema-based APDU handling.
 * Works on top of the existing annotation system, providing structure
 * while allowing developers to implement custom business logic.
 */
public abstract class SchemaBasedController extends BaseApduController {
    
    private final ApduSchema schema;
    
    public SchemaBasedController(ApduSchema schema) {
        this.schema = schema;
    }
    
    /**
     * @return The schema this controller implements
     */
    public ApduSchema getSchema() {
        return schema;
    }
    
    /**
     * Handle a schema-defined command. Developers override this method
     * to implement their business logic while the schema provides structure.
     * 
     * @param commandSpec The matched command specification from the schema
     * @param apduBytes The raw APDU command bytes
     * @return The response to send back
     */
    public abstract ApduResponse handleSchemaCommand(ApduCommandSpec commandSpec, byte[] apduBytes);
    
    /**
     * Validates a command against the schema before processing.
     * Can be overridden for custom validation logic.
     * 
     * @param commandSpec The command specification
     * @param apduBytes The APDU bytes
     * @return true if valid, false otherwise
     */
    protected boolean validateCommand(ApduCommandSpec commandSpec, byte[] apduBytes) {
        return commandSpec.matches(apduBytes);
    }
    
    /**
     * Validates a response against the schema after processing.
     * Can be overridden for custom validation logic.
     * 
     * @param responseSpec The response specification
     * @param responseBytes The response bytes
     * @return true if valid, false otherwise
     */
    protected boolean validateResponse(ApduResponseSpec responseSpec, byte[] responseBytes) {
        return responseSpec.matches(responseBytes);
    }
    
    /**
     * Creates a response that matches a specific response specification.
     * Utility method for developers to ensure schema compliance.
     * 
     * @param responseSpec The target response specification
     * @param data The response data (can be null)
     * @return An ApduResponse that matches the specification
     */
    protected ApduResponse createSchemaResponse(ApduResponseSpec responseSpec, byte[] data) {
        return new ApduResponse(data, responseSpec.getStatusWord());
    }
}