package com.example.hcelibrary.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a class that extends ApduRouterService to generate
 * the necessary AndroidManifest.xml entries and AID filter files.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // Needed at compile time, not runtime
public @interface HceService {
    /**
     * A description for the HCE service.
     * @return The service description.
     */
    String description();

    /**
     * An array of Application IDs (AIDs) that this service will respond to.
     * @return An array of AIDs as strings.
     */
    String[] aids();
}
