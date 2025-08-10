package com.codingr.nfclib.schema.examples;

import com.codingr.nfclib.hce.annotations.ApduController;
import com.codingr.nfclib.hce.annotations.ApduMapping;
import com.codingr.nfclib.hce.core.ApduResponse;
import com.codingr.nfclib.schema.ApduCommandSpec;
import com.codingr.nfclib.schema.SchemaBasedController;
import com.codingr.nfclib.schema.ServerFactory;

/**
 * Example showing how to create a hosted card (server) implementation
 * using the schema-based approach. Developers maintain full control over
 * business logic while benefiting from schema structure and validation.
 */
public class SampleServerUsage {
    
    /**
     * Example 1: Using SchemaBasedController directly
     * This approach extends the base class and works with existing annotations.
     */
    @ApduController(aids = {"F0010203040506", "A000000001020304"})
    public static class SampleCardController extends SchemaBasedController {
        
        private boolean isAuthenticated = false;
        private String userData = "Sample user data";
        
        public SampleCardController() {
            super(new SampleCardSchema());
        }
        
        // Traditional annotation-based mapping with schema validation
        @ApduMapping(command = {(byte) 0x80, (byte) 0x10, (byte) 0x00, (byte) 0x00})
        public ApduResponse getData(byte[] apdu) {
            ApduCommandSpec commandSpec = getSchema().findCommandByApdu(apdu);
            if (commandSpec != null) {
                return handleSchemaCommand(commandSpec, apdu);
            }
            return error(com.codingr.nfclib.hce.core.CardError.INS_NOT_SUPPORTED);
        }
        
        @ApduMapping(command = {(byte) 0x80, (byte) 0x20, (byte) 0x00, (byte) 0x01})
        public ApduResponse authenticate(byte[] apdu) {
            ApduCommandSpec commandSpec = getSchema().findCommandByApdu(apdu);
            if (commandSpec != null) {
                return handleSchemaCommand(commandSpec, apdu);
            }
            return error(com.codingr.nfclib.hce.core.CardError.INS_NOT_SUPPORTED);
        }
        
        @ApduMapping(command = {(byte) 0x80, (byte) 0x30, (byte) 0x00})
        public ApduResponse updateRecord(byte[] apdu) {
            ApduCommandSpec commandSpec = getSchema().findCommandByApdu(apdu);
            if (commandSpec != null) {
                return handleSchemaCommand(commandSpec, apdu);
            }
            return error(com.codingr.nfclib.hce.core.CardError.INS_NOT_SUPPORTED);
        }
        
        @Override
        public ApduResponse handleSchemaCommand(ApduCommandSpec commandSpec, byte[] apduBytes) {
            // Custom business logic based on command type
            switch (commandSpec.getCommandId()) {
                case SampleCardSchema.GET_DATA:
                    return handleGetData(commandSpec, apduBytes);
                    
                case SampleCardSchema.AUTHENTICATE:
                    return handleAuthenticate(commandSpec, apduBytes);
                    
                case SampleCardSchema.UPDATE_RECORD:
                    return handleUpdateRecord(commandSpec, apduBytes);
                    
                default:
                    return error(com.codingr.nfclib.hce.core.CardError.INS_NOT_SUPPORTED);
            }
        }
        
        private ApduResponse handleGetData(ApduCommandSpec commandSpec, byte[] apduBytes) {
            if (!isAuthenticated) {
                // Find the "NOT_AUTHENTICATED" response spec from the command
                var responseSpec = commandSpec.getPossibleResponses().stream()
                    .filter(r -> r.getResponseId().equals(SampleCardSchema.NOT_AUTHENTICATED))
                    .findFirst().orElse(null);
                
                return createSchemaResponse(responseSpec, null);
            }
            
            // Return user data
            var successSpec = commandSpec.getPossibleResponses().stream()
                .filter(r -> r.getResponseId().equals(SampleCardSchema.SUCCESS))
                .findFirst().orElse(null);
            
            return createSchemaResponse(successSpec, userData.getBytes());
        }
        
        private ApduResponse handleAuthenticate(ApduCommandSpec commandSpec, byte[] apduBytes) {
            // Extract PIN from APDU (skip header: CLA INS P1 P2 LC)
            if (apduBytes.length < 6) {
                var errorSpec = commandSpec.getPossibleResponses().stream()
                    .filter(r -> r.getResponseId().equals(SampleCardSchema.INVALID_DATA))
                    .findFirst().orElse(null);
                return createSchemaResponse(errorSpec, null);
            }
            
            int dataLength = apduBytes[4] & 0xFF;
            byte[] pin = new byte[dataLength];
            System.arraycopy(apduBytes, 5, pin, 0, dataLength);
            
            // Simple PIN check (in real implementation, use secure comparison)
            String pinString = new String(pin);
            if ("1234".equals(pinString)) {
                isAuthenticated = true;
                var successSpec = commandSpec.getPossibleResponses().stream()
                    .filter(r -> r.getResponseId().equals(SampleCardSchema.SUCCESS))
                    .findFirst().orElse(null);
                return createSchemaResponse(successSpec, null);
            } else {
                var errorSpec = commandSpec.getPossibleResponses().stream()
                    .filter(r -> r.getResponseId().equals(SampleCardSchema.INVALID_DATA))
                    .findFirst().orElse(null);
                return createSchemaResponse(errorSpec, null);
            }
        }
        
        private ApduResponse handleUpdateRecord(ApduCommandSpec commandSpec, byte[] apduBytes) {
            if (!isAuthenticated) {
                var errorSpec = commandSpec.getPossibleResponses().stream()
                    .filter(r -> r.getResponseId().equals(SampleCardSchema.NOT_AUTHENTICATED))
                    .findFirst().orElse(null);
                return createSchemaResponse(errorSpec, null);
            }
            
            // Extract new data from APDU
            if (apduBytes.length < 6) {
                var errorSpec = commandSpec.getPossibleResponses().stream()
                    .filter(r -> r.getResponseId().equals(SampleCardSchema.INVALID_DATA))
                    .findFirst().orElse(null);
                return createSchemaResponse(errorSpec, null);
            }
            
            int dataLength = apduBytes[4] & 0xFF;
            byte[] newData = new byte[dataLength];
            System.arraycopy(apduBytes, 5, newData, 0, dataLength);
            
            // Update the user data
            userData = new String(newData);
            
            var successSpec = commandSpec.getPossibleResponses().stream()
                .filter(r -> r.getResponseId().equals(SampleCardSchema.SUCCESS))
                .findFirst().orElse(null);
            return createSchemaResponse(successSpec, null);
        }
    }
    
    /**
     * Example 2: Using ServerFactory with handler pattern
     * This approach uses a factory to generate the controller with a handler.
     */
    public static class SampleServerHandler implements ServerFactory.ServerCommandHandler {
        
        private boolean isAuthenticated = false;
        private String userData = "Factory-based user data";
        
        @Override
        public ServerFactory.ServerResponse handleCommand(ApduCommandSpec commandSpec, byte[] apduBytes) {
            
            switch (commandSpec.getCommandId()) {
                case SampleCardSchema.GET_DATA:
                    return handleGetData(commandSpec, apduBytes);
                    
                case SampleCardSchema.AUTHENTICATE:
                    return handleAuthenticate(commandSpec, apduBytes);
                    
                case SampleCardSchema.UPDATE_RECORD:
                    return handleUpdateRecord(commandSpec, apduBytes);
                    
                default:
                    // This shouldn't happen if schema is working correctly
                    throw new IllegalArgumentException("Unsupported command: " + commandSpec.getCommandId());
            }
        }
        
        private ServerFactory.ServerResponse handleGetData(ApduCommandSpec commandSpec, byte[] apduBytes) {
            if (!isAuthenticated) {
                var responseSpec = findResponseSpec(commandSpec, SampleCardSchema.NOT_AUTHENTICATED);
                return new ServerFactory.ServerResponse(SampleCardSchema.NOT_AUTHENTICATED, null, responseSpec);
            }
            
            var responseSpec = findResponseSpec(commandSpec, SampleCardSchema.SUCCESS);
            return new ServerFactory.ServerResponse(SampleCardSchema.SUCCESS, userData.getBytes(), responseSpec);
        }
        
        private ServerFactory.ServerResponse handleAuthenticate(ApduCommandSpec commandSpec, byte[] apduBytes) {
            // Extract and validate PIN
            if (apduBytes.length < 6) {
                var responseSpec = findResponseSpec(commandSpec, SampleCardSchema.INVALID_DATA);
                return new ServerFactory.ServerResponse(SampleCardSchema.INVALID_DATA, null, responseSpec);
            }
            
            int dataLength = apduBytes[4] & 0xFF;
            byte[] pin = new byte[dataLength];
            System.arraycopy(apduBytes, 5, pin, 0, dataLength);
            
            if ("1234".equals(new String(pin))) {
                isAuthenticated = true;
                var responseSpec = findResponseSpec(commandSpec, SampleCardSchema.SUCCESS);
                return new ServerFactory.ServerResponse(SampleCardSchema.SUCCESS, null, responseSpec);
            } else {
                var responseSpec = findResponseSpec(commandSpec, SampleCardSchema.INVALID_DATA);
                return new ServerFactory.ServerResponse(SampleCardSchema.INVALID_DATA, null, responseSpec);
            }
        }
        
        private ServerFactory.ServerResponse handleUpdateRecord(ApduCommandSpec commandSpec, byte[] apduBytes) {
            if (!isAuthenticated) {
                var responseSpec = findResponseSpec(commandSpec, SampleCardSchema.NOT_AUTHENTICATED);
                return new ServerFactory.ServerResponse(SampleCardSchema.NOT_AUTHENTICATED, null, responseSpec);
            }
            
            // Extract and update data
            int dataLength = apduBytes[4] & 0xFF;
            byte[] newData = new byte[dataLength];
            System.arraycopy(apduBytes, 5, newData, 0, dataLength);
            userData = new String(newData);
            
            var responseSpec = findResponseSpec(commandSpec, SampleCardSchema.SUCCESS);
            return new ServerFactory.ServerResponse(SampleCardSchema.SUCCESS, null, responseSpec);
        }
        
        private com.codingr.nfclib.schema.ApduResponseSpec findResponseSpec(ApduCommandSpec commandSpec, String responseId) {
            return commandSpec.getPossibleResponses().stream()
                .filter(r -> r.getResponseId().equals(responseId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Response spec not found: " + responseId));
        }
    }
    
    /**
     * How to use these implementations
     */
    public static void demonstrateUsage() {
        // Approach 1: Direct controller usage (works with existing ApduRouterService)
        SampleCardController controller1 = new SampleCardController();
        
        // Approach 2: Factory-generated controller
        SampleServerHandler handler = new SampleServerHandler();
        SampleCardSchema schema = new SampleCardSchema();
        SchemaBasedController controller2 = ServerFactory.createConcreteController(schema, handler);
        
        // Both controllers can be used with the existing ApduRouterService
        // The schema provides structure and validation while developers retain full control
        // over business logic implementation
    }
}