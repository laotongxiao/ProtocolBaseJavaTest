package com.test01;

public abstract class BasicProtocol {
    public abstract int getProtocolType();
    public abstract byte[] genContentData();
    public abstract void parseContentData(byte[] data);
    public abstract String printProtocol();
}
