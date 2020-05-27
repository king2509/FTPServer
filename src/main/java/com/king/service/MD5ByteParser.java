package com.king.service;

public class MD5ByteParser {
    /**
     * 字节数组转字符串
     * @param byteArray 传入的字节数组
     * @return 目标字符串
     */
     static String byteArrayToHex(byte[] byteArray) {
        StringBuilder hs = new StringBuilder();
        String stmp = "";
        for (byte b : byteArray) {
            stmp = (Integer.toHexString(b & 0XFF));
            if (stmp.length() == 1) {
                hs.append("0").append(stmp);
            } else {
                hs.append(stmp);
            }
        }
        return hs.toString();
    }
}
