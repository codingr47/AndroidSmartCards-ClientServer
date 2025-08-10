package com.codingr.nfclib.schema.examples;

import com.codingr.nfclib.schema.ApduCommandSpec;
import com.codingr.nfclib.schema.ApduResponseSpec;
import com.codingr.nfclib.schema.ClientFactory;

/**
 * Example showing how to create NFC terminal client implementations
 * using the schema-based approach. Developers implement custom response
 * processing logic while benefiting from schema structure and validation.
 */
public class SampleClientUsage {
    
    /**
     * Example NFC transport implementation (platform-specific)
     * In real implementation, this would use Android NFC APIs
     */
    public static class MockNfcTransport implements ClientFactory.NfcTransport {
        private boolean connected = true;
        
        @Override
        public byte[] transmit(byte[] apdu) throws ClientFactory.NfcCommunicationException {
            // Mock implementation - in real code, this would use NFC hardware
            System.out.println("Sending APDU: " + bytesToHex(apdu));
            
            // Simulate responses based on command
            if (apdu.length >= 2) {
                if (apdu[1] == (byte) 0xA4) { // SELECT
                    return new byte[]{(byte) 0x90, 0x00}; // Success
                } else if (apdu[1] == 0x10) { // GET_DATA
                    String data = "Sample response data";
                    byte[] response = new byte[data.length() + 2];
                    System.arraycopy(data.getBytes(), 0, response, 0, data.length());
                    response[response.length - 2] = (byte) 0x90;
                    response[response.length - 1] = 0x00;
                    return response;
                } else if (apdu[1] == 0x20) { // AUTHENTICATE
                    return new byte[]{(byte) 0x90, 0x00}; // Success
                } else if (apdu[1] == 0x30) { // UPDATE_RECORD
                    return new byte[]{(byte) 0x90, 0x00}; // Success
                }
            }
            
            return new byte[]{(byte) 0x6D, 0x00}; // INS not supported
        }
        
        @Override
        public boolean isConnected() {
            return connected;
        }
        
        @Override
        public void disconnect() {
            connected = false;
        }
        
        private String bytesToHex(byte[] bytes) {
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02X ", b));
            }
            return result.toString().trim();
        }
    }
    
    /**
     * Example response handler implementing custom business logic
     */
    public static class SampleResponseHandler implements ClientFactory.ClientResponseHandler {
        
        @Override
        public <T> T handleResponse(ApduCommandSpec commandSpec, ApduResponseSpec responseSpec, byte[] rawResponse) {
            System.out.println("Processing response for command: " + commandSpec.getCommandId());
            System.out.println("Raw response: " + bytesToHex(rawResponse));
            
            if (responseSpec != null) {
                System.out.println("Matched response: " + responseSpec.getResponseId());
                
                if (responseSpec.isSuccess()) {
                    return (T) processSuccessResponse(commandSpec, responseSpec, rawResponse);
                } else {
                    return (T) processErrorResponse(commandSpec, responseSpec, rawResponse);
                }
            } else {
                System.out.println("No matching response specification found");
                return (T) processUnknownResponse(commandSpec, rawResponse);
            }
        }
        
        private Object processSuccessResponse(ApduCommandSpec commandSpec, ApduResponseSpec responseSpec, byte[] rawResponse) {
            switch (commandSpec.getCommandId()) {
                case SampleCardSchema.GET_DATA:
                    // Extract data portion (excluding status word)
                    byte[] data = responseSpec.extractData(rawResponse);
                    return new GetDataResponse(true, new String(data), null);
                    
                case SampleCardSchema.AUTHENTICATE:
                    return new AuthResponse(true, "Authentication successful");
                    
                case SampleCardSchema.UPDATE_RECORD:
                    return new UpdateResponse(true, "Record updated successfully");
                    
                default:
                    return new GenericResponse(true, responseSpec.getDescription());
            }
        }
        
        private Object processErrorResponse(ApduCommandSpec commandSpec, ApduResponseSpec responseSpec, byte[] rawResponse) {
            switch (commandSpec.getCommandId()) {
                case SampleCardSchema.GET_DATA:
                    return new GetDataResponse(false, null, responseSpec.getErrorDescription());
                    
                case SampleCardSchema.AUTHENTICATE:
                    return new AuthResponse(false, responseSpec.getErrorDescription());
                    
                case SampleCardSchema.UPDATE_RECORD:
                    return new UpdateResponse(false, responseSpec.getErrorDescription());
                    
                default:
                    return new GenericResponse(false, responseSpec.getErrorDescription());
            }
        }
        
        private Object processUnknownResponse(ApduCommandSpec commandSpec, byte[] rawResponse) {
            // Handle unexpected responses
            return new GenericResponse(false, "Unknown response format");
        }
        
        private String bytesToHex(byte[] bytes) {
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02X ", b));
            }
            return result.toString().trim();
        }
    }
    
    /**
     * Response data classes for different command types
     */
    public static class GetDataResponse {
        private final boolean success;
        private final String data;
        private final String error;
        
        public GetDataResponse(boolean success, String data, String error) {
            this.success = success;
            this.data = data;
            this.error = error;
        }
        
        public boolean isSuccess() { return success; }
        public String getData() { return data; }
        public String getError() { return error; }
    }
    
    public static class AuthResponse {
        private final boolean success;
        private final String message;
        
        public AuthResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    public static class UpdateResponse {
        private final boolean success;
        private final String message;
        
        public UpdateResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    public static class GenericResponse {
        private final boolean success;
        private final String message;
        
        public GenericResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    /**
     * Optional: Typed client interface for the sample card schema
     * ClientFactory can generate implementations of this interface
     */
    public interface SampleCardClient {
        SampleCardSchema getSchema();
        GetDataResponse getData() throws ClientFactory.NfcCommunicationException;
        AuthResponse authenticate(byte[] pin) throws ClientFactory.NfcCommunicationException;
        UpdateResponse updateRecord(byte[] recordData) throws ClientFactory.NfcCommunicationException;
        boolean selectApplication(String aid) throws ClientFactory.NfcCommunicationException;
        void disconnect();
        boolean isConnected();
    }
    
    /**
     * Demonstration of client usage
     */
    public static void demonstrateClientUsage() {
        try {
            // Set up components
            SampleCardSchema schema = new SampleCardSchema();
            MockNfcTransport transport = new MockNfcTransport();
            SampleResponseHandler responseHandler = new SampleResponseHandler();
            
            // Approach 1: Generic schema client
            ClientFactory.GenericSchemaClient genericClient = 
                ClientFactory.createGenericClient(schema, transport, responseHandler);
            
            // Select application using AID from schema
            boolean selected = genericClient.selectApplication("F0010203040506");
            System.out.println("Application selected: " + selected);
            
            // Use command IDs from schema
            AuthResponse authResult = genericClient.sendCommand(SampleCardSchema.AUTHENTICATE, "1234".getBytes());
            System.out.println("Authentication: " + authResult.isSuccess() + " - " + authResult.getMessage());
            
            GetDataResponse dataResult = genericClient.sendCommand(SampleCardSchema.GET_DATA, null);
            System.out.println("Get Data: " + dataResult.isSuccess() + " - " + dataResult.getData());
            
            UpdateResponse updateResult = genericClient.sendCommand(SampleCardSchema.UPDATE_RECORD, "New data".getBytes());
            System.out.println("Update: " + updateResult.isSuccess() + " - " + updateResult.getMessage());
            
            // Approach 2: Typed client interface
            SampleCardClient typedClient = ClientFactory.createClient(
                schema, transport, responseHandler, SampleCardClient.class);
            
            // Use typed methods
            typedClient.selectApplication("F0010203040506");
            AuthResponse typedAuthResult = typedClient.authenticate("1234".getBytes());
            GetDataResponse typedDataResult = typedClient.getData();
            UpdateResponse typedUpdateResult = typedClient.updateRecord("Updated data".getBytes());
            
            // Clean up
            genericClient.disconnect();
            typedClient.disconnect();
            
        } catch (ClientFactory.NfcCommunicationException e) {
            System.err.println("NFC communication error: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        demonstrateClientUsage();
    }
}