package com.codingr.nfclib.hce.core;

import com.codingr.nfclib.hce.util.ApduUtil;

public enum CardError {
    INS_NOT_SUPPORTED(ApduUtil.SW_INS_NOT_SUPPORTED),
    FILE_NOT_FOUND(ApduUtil.SW_FILE_NOT_FOUND),
    CONDITIONS_NOT_SATISFIED(ApduUtil.SW_CONDITIONS_NOT_SATISFIED),
    WRONG_LENGTH(ApduUtil.SW_WRONG_LENGTH);

    private final byte[] statusWord;

    CardError(byte[] statusWord) {
        this.statusWord = statusWord;
    }

    public byte[] getStatusWord() {
        return statusWord;
    }
}
