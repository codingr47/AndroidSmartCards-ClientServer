package com.example.hceapp;

import com.example.hcelibrary.annotations.ApduController;
import com.example.hcelibrary.annotations.ApduMapping;
import com.example.hcelibrary.core.ApduResponse;
import com.example.hcelibrary.util.ApduUtil;

@ApduController(aids = {"F0010203040507"})
public class AnotherCardController {

    private static final byte[] GET_DATA_COMMAND = {(byte) 0x80, 0x10, 0x00, 0x00};

    @ApduMapping(command = GET_DATA_COMMAND)
    public ApduResponse getData(byte[] apdu) {
        byte[] responseData = "Hello from F0010203040507!".getBytes();
        return new ApduResponse(responseData, ApduUtil.SW_OK);
    }
}
