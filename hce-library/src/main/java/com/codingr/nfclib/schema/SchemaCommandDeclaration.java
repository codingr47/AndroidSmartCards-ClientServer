package com.codingr.nfclib.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method as defining an APDU command specification.
 * Methods annotated with this will be used to automatically generate
 * ApduCommandSpec objects for the schema's command list.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaCommandDeclaration {
    /**
     * The command identifier that corresponds to the CommandId enum value
     * @return The command ID string
     */
    String commandId();
    
    /**
     * The command name for documentation
     * @return The human-readable command name
     */
    String name() default "";
    
    /**
     * The command description
     * @return The command description
     */
    String description() default "";
    
    /**
     * The P1 parameter value
     * @return The P1 byte value
     */
    byte p1() default 0x00;
    
    /**
     * The P2 parameter value  
     * @return The P2 byte value
     */
    byte p2() default 0x00;
    
    /**
     * Minimum expected data length
     * @return The minimum data length
     */
    int minDataLength() default 0;
    
    /**
     * Maximum expected data length
     * @return The maximum data length
     */
    int maxDataLength() default 255;
}