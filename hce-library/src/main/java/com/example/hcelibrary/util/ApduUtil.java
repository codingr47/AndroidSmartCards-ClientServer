package com.example.hcelibrary.util;

public class ApduUtil {

    public static final byte[] SW_OK = {(byte) 0x90, (byte) 0x00};
    public static final byte[] SW_INS_NOT_SUPPORTED = {(byte) 0x6D, (byte) 0x00};
    public static final byte[] SW_WRONG_LENGTH = {(byte) 0x67, (byte) 0x00};
    public static final byte[] SW_FILE_NOT_FOUND = {(byte) 0x6A, (byte) 0x82};
    public static final byte[] SW_CONDITIONS_NOT_SATISFIED = {(byte) 0x69, (byte) 0x85};

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
