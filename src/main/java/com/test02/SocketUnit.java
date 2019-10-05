package com.test02;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SocketUnit {
    private static Map<Integer, String> msgImp = new HashMap<>();

    static {
        msgImp.put(DataProtocol.PROTOCOL_TYPE, "com.test02.DataProtocol");       //0
        msgImp.put(DataAckProtocol.PROTOCOL_TYPE, "com.test02.DataAckProtocol"); //1
        msgImp.put(PingProtocol.PROTOCOL_TYPE, "com.test02.PingProtocol");       //2
        msgImp.put(PingAckProtocol.PROTOCOL_TYPE, "com.test02.PingAckProtocol"); //3
    }
    //bye[4]个字节数组还原成十进制表示
    public static int byteArrayToInt(byte[] b) {
        int intValue = 0;
        for (int i = 0; i < b.length; i++) {
            intValue += (b[i] & 0xFF) << (8 * (3 - i)); //int占4个字节（0，1，2，3）
        }
        return intValue;
    }
    //bye[4]个字节数组还原成十进制,byteOffset表示长串字节中的第几个开始
    public static int byteArrayToInt(byte[] b, int byteOffset, int byteCount) {
        int intValue = 0;
        for (int i = byteOffset; i < (byteOffset + byteCount); i++) {
            intValue += (b[i] & 0xFF) << (8 * (3 - (i - byteOffset)));
        }
        return intValue;
    }
    /**
     * 将int转为大端，低字节存储高位(即[0]>> 24)  Java默认的也是大端 tcp规定的也是大端
     */
    public static byte[] int2ByteArrays(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }
    ////接收数据解析成协议
    public static BasicProtocol parseContentMsg(byte[] protocolType, byte[] data) {
        int protocolTypeMap = SocketUnit.byteArrayToInt(protocolType);
        String className = msgImp.get(protocolTypeMap);
        BasicProtocol basicProtocol;
        try {
            basicProtocol = (BasicProtocol) Class.forName(className).newInstance();
            basicProtocol.parseContentData(data);
        } catch (Exception e) {
            basicProtocol = null;
            e.printStackTrace();
        }
        return basicProtocol;
    }
    /**
     * 读数据
     * 功能1服务器从客户端读取协议数据
     * @param inputStream
     */
    public static BasicProtocol redFromStream(InputStream inputStream){
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        int len = 0;
        int temp = 0;
        try {
            byte[] protocolTypeBytes = new byte[Config.LENGTH];
            //第1步 先读取一条请求协议长度字节
            while (len < protocolTypeBytes.length){
                //从包装流读取header.length - len个长度数据到header的数组中,它是表示协议总长度用4个字节存贮
                //len 表示从len这个位置开始读取
                temp = bufferedInputStream.read(protocolTypeBytes, len, protocolTypeBytes.length - len);
                if(temp > 0){
                    len += temp;
                }else if(temp == -1){
                    bufferedInputStream.close();
                    //这个是一定要返回值的因为这方法要有返回置,要不就会抛异常
                    return null;
                }
            }
            byte[] lengthBytes = new byte[Config.LENGTH];
            len = 0;
            //第2步 先读取一条请求协议长度字节
            while (len < lengthBytes.length){
                //从包装流读取header.length - len个长度数据到header的数组中,它是表示协议总长度用4个字节存贮
                //len 表示从len这个位置开始读取
                temp = bufferedInputStream.read(lengthBytes, len, lengthBytes.length - len);
                if(temp > 0){
                    len += temp;
                }else if(temp == -1){
                    bufferedInputStream.close();
                    //这个是一定要返回值的因为这方法要有返回置,要不就会抛异常
                    return null;
                }
            }
            //第3步 再读取此条协议剩下字节,那么一条协议就读取完成
            len = 0;
            int dataLength = byteArrayToInt(lengthBytes);
            //System.out.println("------dataLength:" + dataLength);
            byte[] dataBytes = new byte[dataLength];
            while (len < dataLength){
                temp = bufferedInputStream.read(dataBytes, len, dataLength - len);
                if(temp > 0){
                    len += temp;
                }
            }
            BasicProtocol protocol = parseContentMsg(protocolTypeBytes,dataBytes);
            return protocol;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void write2Stream(BasicProtocol protocol, OutputStream outputStream) {
        try {
            outputStream.write(protocol.genContentData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 关闭输入流
     *
     * @param is
     */
    public static void closeInputStream(InputStream is) {
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭输出流
     *
     * @param os
     */
    public static void closeOutputStream(OutputStream os) {
        try {
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}