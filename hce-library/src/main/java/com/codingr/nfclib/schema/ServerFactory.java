package com.codingr.nfclib.schema;

import com.codingr.nfclib.hce.annotations.ApduController;
import com.codingr.nfclib.hce.annotations.ApduMapping;
import com.codingr.nfclib.hce.core.ApduResponse;
import com.codingr.nfclib.hce.util.ApduUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Factory for creating hosted card (server) implementations from schemas.
 * Generates controllers that work with the existing ApduRouterService
 * while providing schema-based structure and validation.
 */
public class ServerFactory {
    
    /**
     * Creates a hosted card controller from a schema and business logic handler.
     * The generated controller integrates with the existing annotation-based system.
     * 
     * @param schema The APDU protocol schema
     * @param handler The business logic handler for processing commands
     * @return A controller class that can be registered with ApduRouterService
     */
    public static Class<?> createServerController(ApduSchema schema, ServerCommandHandler handler) {
        return (Class<?>) Proxy.newProxyInstance(
            ServerFactory.class.getClassLoader(),
            new Class[] { ServerController.class },
            new ServerControllerInvocationHandler(schema, handler)
        );
    }
    
    /**
     * Creates a concrete controller instance that extends SchemaBasedController.
     * This approach allows for direct instantiation and more natural inheritance.
     * 
     * @param schema The APDU protocol schema
     * @param handler The business logic handler
     * @return A concrete controller instance
     */
    public static SchemaBasedController createConcreteController(ApduSchema schema, ServerCommandHandler handler) {
        return new ConcreteSchemaController(schema, handler);
    }
    
    /**
     * Interface for handling server-side command processing.
     * Developers implement this to define their business logic.
     */
    public interface ServerCommandHandler {
        /**
         * Process a command with the given specification and raw bytes.
         * 
         * @param commandSpec The matched command specification from schema
         * @param apduBytes The raw APDU command bytes
         * @return The response data (status word will be added based on schema)
         */
        ServerResponse handleCommand(ApduCommandSpec commandSpec, byte[] apduBytes);
    }
    
    /**
     * Response from server command handler containing data and response specification.
     */
    public static class ServerResponse {
        private final String responseId;
        private final byte[] data;
        private final ApduResponseSpec responseSpec;
        
        public ServerResponse(String responseId, byte[] data, ApduResponseSpec responseSpec) {
            this.responseId = responseId;
            this.data = data;
            this.responseSpec = responseSpec;
        }
        
        public String getResponseId() { return responseId; }
        public byte[] getData() { return data; }
        public ApduResponseSpec getResponseSpec() { return responseSpec; }
    }
    
    /**
     * Marker interface for generated server controllers
     */
    public interface ServerController {
        ApduSchema getSchema();
    }
    
    /**
     * Concrete implementation of SchemaBasedController for direct use
     */
    private static class ConcreteSchemaController extends SchemaBasedController {
        private final ServerCommandHandler handler;
        
        public ConcreteSchemaController(ApduSchema schema, ServerCommandHandler handler) {
            super(schema);
            this.handler = handler;
        }
        
        @Override
        public ApduResponse handleSchemaCommand(ApduCommandSpec commandSpec, byte[] apduBytes) {
            ServerResponse response = handler.handleCommand(commandSpec, apduBytes);
            return createSchemaResponse(response.getResponseSpec(), response.getData());
        }
    }
    
    /**
     * Dynamic proxy handler for creating annotation-compatible controllers
     */
    private static class ServerControllerInvocationHandler implements InvocationHandler {
        private final ApduSchema schema;
        private final ServerCommandHandler handler;
        
        public ServerControllerInvocationHandler(ApduSchema schema, ServerCommandHandler handler) {
            this.schema = schema;
            this.handler = handler;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getSchema".equals(method.getName())) {
                return schema;
            }
            
            // Handle APDU processing methods that would be generated
            if (method.getParameterTypes().length == 1 && 
                method.getParameterTypes()[0] == byte[].class &&
                method.getReturnType() == ApduResponse.class) {
                
                byte[] apduBytes = (byte[]) args[0];
                ApduCommandSpec commandSpec = schema.findCommandByApdu(apduBytes);
                
                if (commandSpec != null) {
                    ServerResponse response = handler.handleCommand(commandSpec, apduBytes);
                    return new ApduResponse(response.getData(), response.getResponseSpec().getStatusWord());
                } else {
                    return new ApduResponse(null, ApduUtil.SW_INS_NOT_SUPPORTED);
                }
            }
            
            return method.getDefaultValue();
        }
    }
}