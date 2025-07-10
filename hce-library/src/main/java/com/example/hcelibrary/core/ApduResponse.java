package com.example.hcelibrary.core;

public class ApduResponse {

    private final byte[] data;
    private final byte[] statusWord;

    public ApduResponse(byte[] data, byte[] statusWord) {
        this.data = data;
        this.statusWord = statusWord;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getStatusWord() {
        return statusWord;
    }

    public byte[] toBytes() {
        if (data == null || data.length == 0) {
            return statusWord;
        }
        byte[] response = new byte[data.length + statusWord.length];
        System.arraycopy(data, 0, response, 0, data.length);
        System.arraycopy(statusWord, 0, response, data.length, statusWord.length);
        return response;
    }
}
