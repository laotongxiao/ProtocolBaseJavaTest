package com.test01;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyServer {
    private static ExecutorService executor = new ThreadPoolExecutor(4,4,60L, TimeUnit.SECONDS,new ArrayBlockingQueue(4));
    public static void main(String[] args){
        try {
            ServerSocket serverSocket = new ServerSocket(8899);
            while (true){
                Socket socket= serverSocket.accept();
                System.out.println("成功接入一个客户端");
                if(socket.isConnected()){
                    executor.execute(new MyServerHandler(socket));
                }else{
                    System.out.println("Socket连接失败");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
