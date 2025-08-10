package com.codingr.nfclib.schema;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating NFC terminal client implementations from schemas.
 * Generates clients with typed methods for each command in the schema,
 * while allowing developers to implement custom response processing logic.
 */
public class ClientFactory {
    
    /**
     * Creates an NFC terminal client from a schema and response handler.
     * The generated client has methods corresponding to each command in the schema.
     * 
     * @param schema The APDU protocol schema
     * @param transport The underlying NFC transport implementation
     * @param responseHandler Handler for processing responses with custom logic
     * @return A client interface with schema-specific methods
     */
    public static <T> T createClient(ApduSchema schema, NfcTransport transport, 
                                   ClientResponseHandler responseHandler, Class<T> clientInterface) {
        return (T) Proxy.newProxyInstance(
            ClientFactory.class.getClassLoader(),
            new Class[] { clientInterface },
            new ClientInvocationHandler(schema, transport, responseHandler)
        );
    }
    
    /**
     * Creates a generic schema-based client that provides access to all schema commands.
     * 
     * @param schema The APDU protocol schema
     * @param transport The underlying NFC transport implementation
     * @param responseHandler Handler for processing responses
     * @return A generic client for the schema
     */
    public static GenericSchemaClient createGenericClient(ApduSchema schema, NfcTransport transport, 
                                                        ClientResponseHandler responseHandler) {
        return new GenericSchemaClientImpl(schema, transport, responseHandler);
    }
    
    /**
     * Interface for handling client-side response processing.
     * Developers implement this to define custom response parsing logic.
     */
    public interface ClientResponseHandler {
        /**
         * Process a response from the card with schema context.
         * 
         * @param commandSpec The command that was sent
         * @param responseSpec The matched response specification (null if no match)
         * @param rawResponse The raw response bytes from the card
         * @return Processed response data for the application
         */
        <T> T handleResponse(ApduCommandSpec commandSpec, ApduResponseSpec responseSpec, byte[] rawResponse);
    }
    
    /**
     * Generic client interface that provides access to all commands in a schema.
     */
    public interface GenericSchemaClient {
        ApduSchema getSchema();
        <T> T sendCommand(String commandId, byte[] data) throws NfcCommunicationException;
        <T> T sendCommand(ApduCommandSpec commandSpec, byte[] data) throws NfcCommunicationException;
        boolean selectApplication(String aid) throws NfcCommunicationException;
        void disconnect();
        boolean isConnected();
    }
    
    /**
     * Low-level NFC transport interface for actual card communication.
     * Implementations handle the platform-specific NFC communication.
     */
    public interface NfcTransport {
        byte[] transmit(byte[] apdu) throws NfcCommunicationException;
        boolean isConnected();
        void disconnect();
    }
    
    /**
     * Exception for NFC communication errors
     */
    public static class NfcCommunicationException extends Exception {
        public NfcCommunicationException(String message) { super(message); }
        public NfcCommunicationException(String message, Throwable cause) { super(message, cause); }
    }
    
    /**
     * Implementation of GenericSchemaClient
     */
    private static class GenericSchemaClientImpl implements GenericSchemaClient {
        private final ApduSchema schema;
        private final NfcTransport transport;
        private final ClientResponseHandler responseHandler;
        private String selectedAid;
        
        public GenericSchemaClientImpl(ApduSchema schema, NfcTransport transport, ClientResponseHandler responseHandler) {
            this.schema = schema;
            this.transport = transport;
            this.responseHandler = responseHandler;
        }
        
        @Override
        public ApduSchema getSchema() {
            return schema;
        }
        
        @Override
        public <T> T sendCommand(String commandId, byte[] data) throws NfcCommunicationException {
            ApduCommandSpec commandSpec = schema.getCommand(commandId);
            if (commandSpec == null) {
                throw new NfcCommunicationException("Command not found in schema: " + commandId);
            }
            return sendCommand(commandSpec, data);
        }
        
        @Override
        public <T> T sendCommand(ApduCommandSpec commandSpec, byte[] data) throws NfcCommunicationException {
            // Build APDU from command spec and data
            byte[] apdu = buildApdu(commandSpec, data);
            
            // Send command
            byte[] response = transport.transmit(apdu);
            
            // Find matching response spec
            ApduResponseSpec responseSpec = findMatchingResponse(commandSpec, response);
            
            // Let developer handle the response
            return responseHandler.handleResponse(commandSpec, responseSpec, response);
        }
        
        @Override
        public boolean selectApplication(String aid) throws NfcCommunicationException {
            // Check if AID is supported by schema
            String[] supportedAids = schema.getSupportedAids();
            boolean aidSupported = false;
            for (String supportedAid : supportedAids) {
                if (supportedAid.equalsIgnoreCase(aid)) {
                    aidSupported = true;
                    break;
                }
            }
            
            if (!aidSupported) {
                throw new NfcCommunicationException("AID not supported by schema: " + aid);
            }
            
            // Build SELECT APDU
            byte[] aidBytes = hexStringToBytes(aid);
            byte[] selectApdu = new byte[4 + 1 + aidBytes.length];
            selectApdu[0] = 0x00; // CLA
            selectApdu[1] = (byte) 0xA4; // INS
            selectApdu[2] = 0x04; // P1
            selectApdu[3] = 0x00; // P2
            selectApdu[4] = (byte) aidBytes.length; // LC
            System.arraycopy(aidBytes, 0, selectApdu, 5, aidBytes.length);
            
            byte[] response = transport.transmit(selectApdu);
            boolean success = response.length >= 2 && 
                            response[response.length - 2] == (byte) 0x90 && 
                            response[response.length - 1] == 0x00;
            
            if (success) {
                selectedAid = aid;
            }
            
            return success;
        }
        
        @Override
        public void disconnect() {
            transport.disconnect();
            selectedAid = null;
        }
        
        @Override
        public boolean isConnected() {
            return transport.isConnected();
        }
        
        private byte[] buildApdu(ApduCommandSpec commandSpec, byte[] data) {
            byte[] prefix = commandSpec.getCommandPrefix();
            
            if (data == null || data.length == 0) {
                return prefix;
            }
            
            byte[] apdu = new byte[prefix.length + 1 + data.length];
            System.arraycopy(prefix, 0, apdu, 0, prefix.length);
            apdu[prefix.length] = (byte) data.length; // LC
            System.arraycopy(data, 0, apdu, prefix.length + 1, data.length);
            
            return apdu;
        }
        
        private ApduResponseSpec findMatchingResponse(ApduCommandSpec commandSpec, byte[] response) {
            for (ApduResponseSpec responseSpec : commandSpec.getPossibleResponses()) {
                if (responseSpec.matches(response)) {
                    return responseSpec;
                }
            }
            return null;
        }
        
        private byte[] hexStringToBytes(String hex) {
            int len = hex.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                     + Character.digit(hex.charAt(i + 1), 16));
            }
            return data;
        }
    }
    
    /**
     * Dynamic proxy handler for creating typed client interfaces
     */
    private static class ClientInvocationHandler implements InvocationHandler {
        private final GenericSchemaClient genericClient;
        private final Map<String, String> methodToCommandMapping;
        
        public ClientInvocationHandler(ApduSchema schema, NfcTransport transport, ClientResponseHandler responseHandler) {
            this.genericClient = new GenericSchemaClientImpl(schema, transport, responseHandler);
            this.methodToCommandMapping = buildMethodMapping(schema);
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            
            // Delegate standard methods
            if ("getSchema".equals(methodName)) return genericClient.getSchema();
            if ("disconnect".equals(methodName)) { genericClient.disconnect(); return null; }
            if ("isConnected".equals(methodName)) return genericClient.isConnected();
            if ("selectApplication".equals(methodName)) return genericClient.selectApplication((String) args[0]);
            
            // Handle command methods
            String commandId = methodToCommandMapping.get(methodName);
            if (commandId != null) {
                byte[] data = args.length > 0 ? (byte[]) args[0] : null;
                return genericClient.sendCommand(commandId, data);
            }
            
            throw new UnsupportedOperationException("Method not supported: " + methodName);
        }
        
        private Map<String, String> buildMethodMapping(ApduSchema schema) {
            Map<String, String> mapping = new HashMap<>();
            for (ApduCommandSpec command : schema.getCommands()) {
                // Convert command ID to method name (e.g., "GET_DATA" -> "getData")
                String methodName = commandIdToMethodName(command.getCommandId());
                mapping.put(methodName, command.getCommandId());
            }
            return mapping;
        }
        
        private String commandIdToMethodName(String commandId) {
            String[] parts = commandId.toLowerCase().split("_");
            StringBuilder methodName = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                methodName.append(Character.toUpperCase(parts[i].charAt(0)))
                         .append(parts[i].substring(1));
            }
            return methodName.toString();
        }
    }
}