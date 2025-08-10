# Consumer proguard rules for NFC Library
# These rules are applied to consumers of this library

# Keep all public API classes and methods
-keep public class com.codingr.nfclib.** { 
    public *; 
    protected *;
}

# Keep annotations that consumers will use
-keep @interface com.codingr.nfclib.hce.annotations.*

# Keep classes that consumers will extend
-keep class com.codingr.nfclib.hce.core.BaseApduController { *; }
-keep class com.codingr.nfclib.schema.SchemaBasedController { *; }

# Keep builder pattern classes
-keep class com.codingr.nfclib.schema.ApduCommandSpec$Builder { *; }
-keep class com.codingr.nfclib.schema.ApduResponseSpec$Builder { *; }

# Keep factory classes
-keep class com.codingr.nfclib.schema.ServerFactory { *; }
-keep class com.codingr.nfclib.schema.ClientFactory { *; }

# Ensure consumers can implement interfaces
-keep interface com.codingr.nfclib.schema.ApduSchema { *; }
-keep interface com.codingr.nfclib.schema.ServerFactory$ServerCommandHandler { *; }
-keep interface com.codingr.nfclib.schema.ClientFactory$ClientResponseHandler { *; }
-keep interface com.codingr.nfclib.schema.ClientFactory$NfcTransport { *; }