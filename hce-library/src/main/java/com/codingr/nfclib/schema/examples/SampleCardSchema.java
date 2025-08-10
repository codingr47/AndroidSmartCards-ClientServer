package com.codingr.nfclib.schema.examples;

import com.codingr.nfclib.schema.*;
import com.codingr.nfclib.hce.annotations.ApduController;
import com.codingr.nfclib.hce.util.ApduUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Example schema implementation for a sample smart card application.
 * Demonstrates how to define APDU protocols declaratively while maintaining
 * flexibility for custom business logic implementation.
 */
@ApduController(aids = {"F0010203040506", "A000000001020304"})
public class SampleCardSchema extends BaseApduSchema<SampleCardSchema.Commands> {
    
    public static final String SCHEMA_NAME = "SampleCard";
    public static final String SCHEMA_VERSION = "1.0";
    
    // Response IDs
    public static final String SUCCESS = "SUCCESS";
    public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String INVALID_DATA = "INVALID_DATA";
    public static final String NOT_AUTHENTICATED = "NOT_AUTHENTICATED";
    
    /**
     * Command IDs enum implementing CommandId interface
     */
    public enum Commands implements CommandId {
        GET_DATA("GET_DATA", (byte) 0x80, (byte) 0x10),
        AUTHENTICATE("AUTHENTICATE", (byte) 0x80, (byte) 0x20),
        UPDATE_RECORD("UPDATE_RECORD", (byte) 0x80, (byte) 0x30);
        
        private final String id;
        private final byte cla;
        private final byte ins;
        
        Commands(String id, byte cla, byte ins) {
            this.id = id;
            this.cla = cla;
            this.ins = ins;
        }
        
        @Override
        public String getId() { return id; }
        
        @Override
        public byte getCLA() { return cla; }
        
        @Override
        public byte getINS() { return ins; }
    }
    
    public SampleCardSchema() {
        super(SCHEMA_NAME, SCHEMA_VERSION);
    }
    
    
    @SchemaCommandDeclaration(
        commandId = "GET_DATA",
        name = "Get Data",
        description = "Retrieves data from the card",
        p1 = 0x00,
        p2 = 0x00,
        minDataLength = 0,
        maxDataLength = 0
    )
    public List<ApduResponseSpec> createGetDataCommand() {
        return Arrays.asList(
            new ApduResponseSpec.Builder(SUCCESS, ApduUtil.SW_OK)
                .name("Success")
                .description("Data retrieved successfully")
                .dataLength(1, 255)
                .build(),
            new ApduResponseSpec.Builder(NOT_AUTHENTICATED, ApduUtil.SW_SECURITY_STATUS_NOT_SATISFIED)
                .name("Not Authenticated")
                .errorDescription("Authentication required before data access")
                .dataLength(0, 0)
                .build()
        );
    }
    
    @SchemaCommandDeclaration(
        commandId = "AUTHENTICATE",
        name = "Authenticate",
        description = "Authenticates user with PIN or key",
        p1 = 0x00,
        p2 = 0x01,
        minDataLength = 4,
        maxDataLength = 16
    )
    public List<ApduResponseSpec> createAuthenticateCommand() {
        return Arrays.asList(
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
        );
    }
    
    @SchemaCommandDeclaration(
        commandId = "UPDATE_RECORD",
        name = "Update Record",
        description = "Updates a record on the card",
        p1 = 0x00,
        minDataLength = 1,
        maxDataLength = 255
    )
    public List<ApduResponseSpec> createUpdateRecordCommand() {
        return Arrays.asList(
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
        );
    }

}