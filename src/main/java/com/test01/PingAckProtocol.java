package com.test01;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

public class PingAckProtocol extends BasicProtocol {
    public static final int PROTOCOL_TYPE = 3;
    private String contentData;

    private int remainingBytesLen = 4;

    public String getContentData() {
        return contentData;
    }

    public void setContentData(String contenData) {
        this.contentData = contenData;
    }

    public int getRemainingBytesLen() {
        return remainingBytesLen;
    }

    public void setRemainingBytesLen(int remainingBytesLen) {
        this.remainingBytesLen = remainingBytesLen;
    }
    public int getLength(){
        return Config.LENGTH + remainingBytesLen + contentData.getBytes().length;
    }
    @Override
    public int getProtocolType() {
        return PROTOCOL_TYPE;
    }
    //发送字符串拼接
    @Override
    public byte[] genContentData() {
        byte[] protocolTypeBytes = SocketUnit.int2ByteArrays(PROTOCOL_TYPE);
        byte[] remainingBytes = SocketUnit.int2ByteArrays(contentData.getBytes().length);
        byte[] contentDataBytes = contentData.getBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(getLength());
        baos.write(protocolTypeBytes, 0 ,Config.LENGTH);
        baos.write(remainingBytes, 0, 4);
        baos.write(contentDataBytes, 0 , contentData.getBytes().length);
        return baos.toByteArray();
    }
    //接收数据解析成协议
    @Override
    public void parseContentData(byte[] data) {
        try {
            this.contentData = new String(data, 0, data.length,"Utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    @Override
    public String printProtocol() {
        return "PROTOCOL_TYPE:" + PROTOCOL_TYPE + " " + "contentData:" + contentData;
    }
}
