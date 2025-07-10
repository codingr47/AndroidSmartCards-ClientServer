package com.example.hcelibrary.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a controller for handling APDU commands.
 * The ApduRouterService will scan for classes with this annotation
 * to discover and register APDU handlers.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApduController {
    /**
     * The Application IDs (AIDs) that this controller is responsible for.
     * @return An array of AIDs as strings.
     */
    String[] aids();
}
