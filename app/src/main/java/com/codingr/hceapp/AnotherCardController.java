package com.codingr.hceapp;

import com.codingr.nfclib.hce.annotations.ApduController;
import com.codingr.nfclib.hce.annotations.ApduMapping;
import com.codingr.nfclib.hce.core.ApduResponse;
import com.codingr.nfclib.hce.core.BaseApduController;
import com.codingr.nfclib.hce.core.CardError;

@ApduController(aids = {"F0010203040507"})
public class AnotherCardController extends BaseApduController {

    private static final byte[] GET_DATA_COMMAND = {(byte) 0x80, 0x10, 0x00, 0x00};

    @ApduMapping(command = GET_DATA_COMMAND)
    public ApduResponse getData(byte[] apdu) {
        return ok("Hello from F0010203040507!".getBytes());
    }
}
