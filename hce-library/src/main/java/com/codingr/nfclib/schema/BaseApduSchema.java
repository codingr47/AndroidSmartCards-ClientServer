package com.codingr.nfclib.schema;

import com.codingr.nfclib.hce.annotations.ApduController;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for APDU schema implementations.
 * Provides automatic command generation from annotated methods and
 * AID discovery from class-level annotations.
 *
 * @param <T> Enum type that implements CommandId interface
 */
public abstract class BaseApduSchema<T extends Enum<T> & CommandId> implements ApduSchema {
    
    protected final String schemaName;
    protected final String version;
    protected final List<ApduCommandSpec> commands;
    
    /**
     * Constructor that initializes the schema with name and version,
     * then automatically generates command specifications from annotated methods.
     *
     * @param schemaName The name/identifier of this schema
     * @param version The version of this schema
     */
    protected BaseApduSchema(String schemaName, String version) {
        this.schemaName = schemaName;
        this.version = version;
        this.commands = generateCommandsFromAnnotations();
    }
    
    @Override
    public String getName() {
        return schemaName;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    @Override
    public String[] getSupportedAids() {
        ApduController controllerAnnotation = this.getClass().getAnnotation(ApduController.class);
        if (controllerAnnotation != null) {
            return controllerAnnotation.aids();
        }
        return new String[0];
    }
    
    @Override
    public List<ApduCommandSpec> getCommands() {
        return new ArrayList<>(commands);
    }
    
    @Override
    public ApduCommandSpec getCommand(String commandId) {
        return commands.stream()
                .filter(cmd -> cmd.getCommandId().equals(commandId))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public ApduCommandSpec findCommandByApdu(byte[] apduBytes) {
        return commands.stream()
                .filter(cmd -> cmd.matches(apduBytes))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Generates ApduCommandSpec objects from methods annotated with @SchemaCommandDeclaration.
     * This method uses reflection to scan the class for annotated methods and creates
     * corresponding command specifications.
     *
     * @return List of generated ApduCommandSpec objects
     */
    private List<ApduCommandSpec> generateCommandsFromAnnotations() {
        List<ApduCommandSpec> generatedCommands = new ArrayList<>();
        
        Method[] methods = this.getClass().getDeclaredMethods();
        for (Method method : methods) {
            SchemaCommandDeclaration annotation = method.getAnnotation(SchemaCommandDeclaration.class);
            if (annotation != null) {
                ApduCommandSpec spec = createCommandSpecFromAnnotation(method, annotation);
                if (spec != null) {
                    generatedCommands.add(spec);
                }
            }
        }
        
        return generatedCommands;
    }
    
    /**
     * Creates an ApduCommandSpec from a method's SchemaCommandDeclaration annotation.
     * This method looks up the corresponding CommandId enum to get CLA and INS values.
     *
     * @param method The annotated method
     * @param annotation The SchemaCommandDeclaration annotation
     * @return The generated ApduCommandSpec or null if command ID not found
     */
    private ApduCommandSpec createCommandSpecFromAnnotation(Method method, SchemaCommandDeclaration annotation) {
        T commandId = findCommandById(annotation.commandId());
        if (commandId == null) {
            return null;
        }
        
        String name = annotation.name().isEmpty() ? annotation.commandId() : annotation.name();
        String description = annotation.description().isEmpty() ? 
            "Command: " + annotation.commandId() : annotation.description();
        
        ApduCommandSpec.Builder builder = new ApduCommandSpec.Builder(
            commandId.getId(),
            commandId.getCLA(),
            commandId.getINS()
        )
        .name(name)
        .description(description)
        .p1(annotation.p1())
        .p2(annotation.p2())
        .dataLength(annotation.minDataLength(), annotation.maxDataLength());
        
        // Try to get possible responses from the method implementation
        List<ApduResponseSpec> responses = getResponsesFromMethod(method);
        if (responses != null && !responses.isEmpty()) {
            builder.possibleResponses(responses);
        }
        
        return builder.build();
    }
    
    /**
     * Finds a CommandId enum value by its string identifier.
     * Uses reflection to get the enum type and search for matching command ID.
     *
     * @param commandId The command identifier string
     * @return The matching CommandId enum value or null if not found
     */
    @SuppressWarnings("unchecked")
    private T findCommandById(String commandId) {
        // Get the generic type parameter T
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericSuperclass;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                Class<T> enumClass = (Class<T>) typeArgs[0];
                if (enumClass.isEnum()) {
                    for (T enumConstant : enumClass.getEnumConstants()) {
                        if (enumConstant.getId().equals(commandId)) {
                            return enumConstant;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Extracts possible responses from a method implementation.
     * Attempts to invoke the method to get the response specifications.
     *
     * @param method The method to extract responses from
     * @return List of possible ApduResponseSpec objects
     */
    private List<ApduResponseSpec> getResponsesFromMethod(Method method) {
        try {
            method.setAccessible(true);
            Object result = method.invoke(this);
            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                List<ApduResponseSpec> responses = (List<ApduResponseSpec>) result;
                return responses;
            }
        } catch (Exception e) {
            // If method invocation fails, return empty list
        }
        return new ArrayList<>();
    }
}