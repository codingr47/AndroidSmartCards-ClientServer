package com.codingr.nfclib.hce.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a method to handle a specific APDU command.
 * The router will use the 'command' as a prefix to match against incoming APDUs.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApduMapping {
    /**
     * The command prefix to match. The router will find the longest matching prefix.
     * @return The command prefix as a byte array.
     */
    byte[] command();
}
