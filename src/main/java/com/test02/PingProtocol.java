package com.test02;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

public class PingProtocol extends BasicProtocol{
    public static final int PROTOCOL_TYPE = 2;
    private int pingId;
    private String contentData;

    private int remainingBytesLen = 4;
    private int pingIdBytesLen = 4;

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

    public int getPingId() {
        return pingId;
    }

    public void setPingId(int pingId) {
        this.pingId = pingId;
    }

    public int getPingIdBytesLen() {
        return pingIdBytesLen;
    }

    public void setPingIdBytesLen(int pingIdBytesLen) {
        this.pingIdBytesLen = pingIdBytesLen;
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
        byte[] remainingBytes = SocketUnit.int2ByteArrays(pingIdBytesLen + contentData.getBytes().length);
        byte[] pingIdBytes = SocketUnit.int2ByteArrays(pingId);
        byte[] contentDataBytes = contentData.getBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(getLength());
        baos.write(protocolTypeBytes, 0 ,Config.LENGTH);
        baos.write(remainingBytes, 0, 4);
        baos.write(pingIdBytes, 0, pingIdBytesLen);
        baos.write(contentDataBytes, 0 , contentData.getBytes().length);
        return baos.toByteArray();
    }
    //接收数据解析成协议
    @Override
    public void parseContentData(byte[] data) {
        int pos = 0;
        this.pingId = SocketUnit.byteArrayToInt(data, pos, pingIdBytesLen);
        pos += pingIdBytesLen;
        try {

            this.contentData = new String(data, pos, data.length - pos,"Utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    @Override
    public String printProtocol() {
        return "PROTOCOL_TYPE:" + PROTOCOL_TYPE + " " + "pingId:" + pingId + " " + "contentData:" + contentData;
    }
}
