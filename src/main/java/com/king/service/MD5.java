package com.king.service;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * MD5类提供计算、比较md5值的功能
 */
public class MD5 {

    /**
     * 计算目标文件的md5值
     * @param filename  传入文件的文件名:绝对路径，包含了目录路径+文件名
     * @return           传入文件的md5值
     * @throws IOException
     */
    public static String calMD5(String filename) {
        if(filename == null || filename.length() == 0)
            return null;

        // 缓冲区大小（这个可以抽出一个参数）
        int bufferSize = 256 * 1024;

        FileInputStream fileInputStream = null;
        DigestInputStream digestInputStream = null;
        try {
            // 拿到一个MD5转换器（同样，这里可以换成SHA1）
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            // 使用DigestInputStream
            fileInputStream = new FileInputStream(filename);
            digestInputStream = new DigestInputStream(fileInputStream, messageDigest);

            // read的过程中进行MD5处理，直到读完文件
            byte[] buffer =new byte[bufferSize];

            // 读取整个文件，在读取过程中进行md5处理
            while (digestInputStream.read(buffer) > 0);

            // 获取最终的MessageDigest
            messageDigest= digestInputStream.getMessageDigest();

            // 拿到结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();
            // 同样，把字节数组转换成字符串
            return MD5ByteParser.byteArrayToHex(resultByteArray);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            try {
                if(digestInputStream != null)
                    digestInputStream.close();
                if(fileInputStream != null)
                    fileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 比较两个md5值是否相同
     * @param localMD5 本地md5值
     * @param remoteMD5 服务器文件md5值
     * @return 比较结果
     */
    public static boolean isSameMD5(String localMD5, String remoteMD5){
        return localMD5 != null && localMD5.equals(remoteMD5);
    }

}