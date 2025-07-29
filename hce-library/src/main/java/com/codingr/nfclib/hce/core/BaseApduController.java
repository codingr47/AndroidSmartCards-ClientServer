package com.codingr.nfclib.hce.core;

import com.codingr.nfclib.hce.util.ApduUtil;

public abstract class BaseApduController {

    protected ApduResponse ok() {
        return new ApduResponse(null, ApduUtil.SW_OK);
    }

    protected ApduResponse ok(byte[] data) {
        return new ApduResponse(data, ApduUtil.SW_OK);
    }

    protected ApduResponse error(CardError cardError) {
        return new ApduResponse(null, cardError.getStatusWord());
    }
}
