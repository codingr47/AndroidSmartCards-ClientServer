# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep all public classes and methods in the library
-keep public class com.codingr.nfclib.** { *; }

# Keep annotations used by reflection
-keep @interface com.codingr.nfclib.hce.annotations.ApduController
-keep @interface com.codingr.nfclib.hce.annotations.ApduMapping
-keep @interface com.codingr.nfclib.hce.annotations.HceService

# Keep classes that are instantiated via reflection
-keep class * extends com.codingr.nfclib.hce.core.BaseApduController { *; }
-keep class * extends com.codingr.nfclib.schema.SchemaBasedController { *; }

# Keep annotation processing related classes
-keepclassmembers class * {
    @com.codingr.nfclib.hce.annotations.ApduMapping <methods>;
}

# Keep schema-related classes for factory pattern
-keep class * implements com.codingr.nfclib.schema.ApduSchema { *; }
-keep class * implements com.codingr.nfclib.schema.ServerFactory$ServerCommandHandler { *; }
-keep class * implements com.codingr.nfclib.schema.ClientFactory$ClientResponseHandler { *; }