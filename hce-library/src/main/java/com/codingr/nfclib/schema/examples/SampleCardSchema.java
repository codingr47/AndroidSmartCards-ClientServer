package com.codingr.nfclib.schema.examples;

import com.codingr.nfclib.schema.ApduCommandSpec;
import com.codingr.nfclib.schema.ApduResponseSpec;
import com.codingr.nfclib.schema.ApduSchema;
import com.codingr.nfclib.hce.util.ApduUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Example schema implementation for a sample smart card application.
 * Demonstrates how to define APDU protocols declaratively while maintaining
 * flexibility for custom business logic implementation.
 */
public class SampleCardSchema implements ApduSchema {
    
    public static final String SCHEMA_NAME = "SampleCard";
    public static final String SCHEMA_VERSION = "1.0";
    
    // Command IDs
    public static final String GET_DATA = "GET_DATA";
    public static final String AUTHENTICATE = "AUTHENTICATE";
    public static final String UPDATE_RECORD = "UPDATE_RECORD";
    
    // Response IDs
    public static final String SUCCESS = "SUCCESS";
    public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String INVALID_DATA = "INVALID_DATA";
    public static final String NOT_AUTHENTICATED = "NOT_AUTHENTICATED";
    
    private final List<ApduCommandSpec> commands;
    
    public SampleCardSchema() {
        this.commands = Arrays.asList(
            createGetDataCommand(),
            createAuthenticateCommand(),
            createUpdateRecordCommand()
        );
    }
    
    @Override
    public String getName() {
        return SCHEMA_NAME;
    }
    
    @Override
    public String getVersion() {
        return SCHEMA_VERSION;
    }
    
    @Override
    public String[] getSupportedAids() {
        return new String[] {
            "F0010203040506",  // Sample AID 1
            "A000000001020304" // Sample AID 2
        };
    }
    
    @Override
    public List<ApduCommandSpec> getCommands() {
        return commands;
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
    
    private ApduCommandSpec createGetDataCommand() {
        return new ApduCommandSpec.Builder(GET_DATA, (byte) 0x80, (byte) 0x10)
                .name("Get Data")
                .description("Retrieves data from the card")
                .p1((byte) 0x00)
                .p2((byte) 0x00)
                .dataLength(0, 0) // No data expected
                .possibleResponses(Arrays.asList(
                    new ApduResponseSpec.Builder(SUCCESS, ApduUtil.SW_OK)
                        .name("Success")
                        .description("Data retrieved successfully")
                        .dataLength(1, 255) // Variable data length
                        .build(),
                    new ApduResponseSpec.Builder(NOT_AUTHENTICATED, ApduUtil.SW_SECURITY_STATUS_NOT_SATISFIED)
                        .name("Not Authenticated")
                        .errorDescription("Authentication required before data access")
                        .dataLength(0, 0)
                        .build()
                ))
                .build();
    }
    
    private ApduCommandSpec createAuthenticateCommand() {
        return new ApduCommandSpec.Builder(AUTHENTICATE, (byte) 0x80, (byte) 0x20)
                .name("Authenticate")
                .description("Authenticates user with PIN or key")
                .p1((byte) 0x00)
                .p2((byte) 0x01)
                .dataLength(4, 16) // PIN/Key length between 4-16 bytes
                .possibleResponses(Arrays.asList(
                    new ApduResponseSpec.Builder(SUCCESS, ApduUtil.SW_OK)
                        .name("Authentication Success")
                        .description("User authenticated successfully")
                        .dataLength(0, 0)
                        .build(),
                    new ApduResponseSpec.Builder(INVALID_DATA, ApduUtil.SW_WRONG_DATA)
                        .name("Invalid Credentials")
                        .errorDescription("Invalid PIN or key provided")
                        .dataLength(0, 0)
                        .build()
                ))
                .build();
    }
    
    private ApduCommandSpec createUpdateRecordCommand() {
        return new ApduCommandSpec.Builder(UPDATE_RECORD, (byte) 0x80, (byte) 0x30)
                .name("Update Record")
                .description("Updates a record on the card")
                .p1((byte) 0x00) // Record number in P2
                .dataLength(1, 255) // Variable record data
                .possibleResponses(Arrays.asList(
                    new ApduResponseSpec.Builder(SUCCESS, ApduUtil.SW_OK)
                        .name("Update Success")
                        .description("Record updated successfully")
                        .dataLength(0, 0)
                        .build(),
                    new ApduResponseSpec.Builder(NOT_AUTHENTICATED, ApduUtil.SW_SECURITY_STATUS_NOT_SATISFIED)
                        .name("Not Authenticated")
                        .errorDescription("Authentication required for record updates")
                        .dataLength(0, 0)
                        .build(),
                    new ApduResponseSpec.Builder(INVALID_DATA, ApduUtil.SW_WRONG_DATA)
                        .name("Invalid Record Data")
                        .errorDescription("Record data format is invalid")
                        .dataLength(0, 0)
                        .build()
                ))
                .build();
    }
}