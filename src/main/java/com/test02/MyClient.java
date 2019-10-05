package com.test02;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.Socket;

public class MyClient {
    public static void main(String[] args){
        try {
            Socket socket = SocketFactory.getDefault().createSocket(Config.ADDRESS, Config.PORT);
            if (socket.isConnected()) {
                MyClientHandler myClientHandler = new MyClientHandler(socket);
                new Thread(myClientHandler).start();
                //发送测试数据
                if (myClientHandler != null){
                    DataProtocol testData = new DataProtocol();
                    testData.setContentData("客户端发给服务器测试数据DataProtocolRequst");
                    //System.out.println(testData.printProtocol());
                    myClientHandler.addRequest(testData);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
