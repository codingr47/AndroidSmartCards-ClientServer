# Android Smart Cards NFC Library

A comprehensive Android library for Host Card Emulation (HCE) and NFC terminal communication with schema-based protocol abstraction.

## Features

- **Host Card Emulation (HCE)**: Emulate smart cards on Android devices
- **Schema-based Protocol Definition**: Declarative APDU protocol definitions
- **Factory Pattern**: Generate both server (card) and client (terminal) implementations from schemas
- **Annotation-based Routing**: Simple controller mapping with `@ApduController` and `@ApduMapping`
- **Automatic Setup**: Annotation processor handles all Android manifest configuration
- **Flexible Business Logic**: Full developer control over command processing and response handling
- **Type Safety**: Compile-time validation and runtime schema compliance

## Project Structure

```
├── hce-library/          # Core NFC library module
├── hce-processor/        # Annotation processor for automatic setup
└── app/                 # Sample application demonstrating usage
```

## Using the Library

### Option 1: As a Module Dependency

1. **Include in your project**: Copy the `hce-library` and `hce-processor` modules to your project
2. **Add to settings.gradle**:
   ```gradle
   include ':hce-library'
   include ':hce-processor'
   ```
3. **Add dependencies** in your app's `build.gradle`:
   ```gradle
   dependencies {
       implementation project(':hce-library')
       annotationProcessor project(':hce-processor')
   }
   ```

### Option 2: As a Published Library (AAR)

1. **Build the library**:
   ```bash
   ./gradlew :hce-library:assembleRelease
   ```
2. **Find the AAR** in `hce-library/build/outputs/aar/hce-library-release.aar`
3. **Include in your project**:
   ```gradle
   dependencies {
       implementation files('libs/hce-library-release.aar')
       annotationProcessor project(':hce-processor') // Still need the processor
   }
   ```

### Option 3: Publish to Maven Repository

Add publishing configuration to `hce-library/build.gradle`:

```gradle
apply plugin: 'maven-publish'

publishing {
    publications {
        release(MavenPublication) {
            from components.release
            
            groupId = 'com.codingr.nfclib'
            artifactId = 'hce-library'
            version = '1.0.0'
        }
    }
}
```

## Quick Start

### 1. Define a Schema

```java
public class MyCardSchema implements ApduSchema {
    public static final String GET_DATA = "GET_DATA";
    
    @Override
    public String getName() { return "MyCard"; }
    
    @Override
    public String[] getSupportedAids() { 
        return new String[]{"F0010203040506"}; 
    }
    
    @Override
    public List<ApduCommandSpec> getCommands() {
        return Arrays.asList(
            new ApduCommandSpec.Builder(GET_DATA, (byte) 0x80, (byte) 0x10)
                .name("Get Data")
                .possibleResponses(Arrays.asList(
                    new ApduResponseSpec.Builder("SUCCESS", ApduUtil.SW_OK)
                        .dataLength(1, 255)
                        .build()
                ))
                .build()
        );
    }
}
```

### 2. Create a Server (Card) Implementation

```java
@ApduController(aids = {"F0010203040506"})  // ← This triggers automatic setup!
public class MyCardController extends SchemaBasedController {
    
    public MyCardController() {
        super(new MyCardSchema());
    }
    
    @ApduMapping(command = {(byte) 0x80, (byte) 0x10, (byte) 0x00, (byte) 0x00})
    public ApduResponse getData(byte[] apdu) {
        ApduCommandSpec commandSpec = getSchema().findCommandByApdu(apdu);
        return handleSchemaCommand(commandSpec, apdu);
    }
    
    @Override
    public ApduResponse handleSchemaCommand(ApduCommandSpec commandSpec, byte[] apduBytes) {
        switch (commandSpec.getCommandId()) {
            case MyCardSchema.GET_DATA:
                return ok("Hello World!".getBytes());
            default:
                return error(CardError.INS_NOT_SUPPORTED);
        }
    }
}
```

**That's it!** The annotation processor automatically:
- ✅ Adds NFC permissions to your AndroidManifest.xml
- ✅ Registers the ApduRouterService
- ✅ Creates aid_list.xml with your controller's AIDs
- ✅ Sets up all required NFC hardware declarations

### 3. Create a Client (Terminal) Implementation

```java
public class MyCardClient {
    private ClientFactory.GenericSchemaClient client;
    
    public void connectAndUse() throws ClientFactory.NfcCommunicationException {
        MyCardSchema schema = new MyCardSchema();
        NfcTransport transport = new AndroidNfcTransport(); // Your implementation
        
        client = ClientFactory.createGenericClient(schema, transport, 
            new MyResponseHandler());
        
        // Use schema-defined AIDs and commands
        client.selectApplication("F0010203040506");
        String result = client.sendCommand(MyCardSchema.GET_DATA, null);
        System.out.println("Received: " + result);
    }
    
    class MyResponseHandler implements ClientFactory.ClientResponseHandler {
        @Override
        public <T> T handleResponse(ApduCommandSpec commandSpec, 
                                  ApduResponseSpec responseSpec, byte[] rawResponse) {
            if (responseSpec != null && responseSpec.isSuccess()) {
                byte[] data = responseSpec.extractData(rawResponse);
                return (T) new String(data);
            }
            return (T) "Error";
        }
    }
}
```

## No Manual Configuration Required!

Unlike other NFC libraries, you don't need to manually add:
- ❌ NFC permissions to AndroidManifest.xml
- ❌ Service declarations
- ❌ aid_list.xml files
- ❌ Intent filters or meta-data

Just add `@ApduController` to your controller class and everything is handled automatically!

## Requirements

- **Android API Level**: 21+ (Android 5.0)
- **NFC Hardware**: Required
- **HCE Support**: Required
- **Java Version**: 8+

## Migration from Other Libraries

If you're migrating from manual NFC setup:

1. Remove manual manifest entries (the processor handles them)
2. Replace your service class with `@ApduController` on your controller
3. Use `@ApduMapping` for command routing
4. Optionally adopt schema-based approach for better structure

## License

[Add your license here]