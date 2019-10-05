package com.test01;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MyServerHandler implements Runnable{
    private Socket socket;
    private ReciveTask reciveTask;
    private SendTask sendTask;
    private volatile ConcurrentLinkedQueue<BasicProtocol> dataQueue = new ConcurrentLinkedQueue<>();
    public MyServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            //启用接收数据线程
            reciveTask = new ReciveTask(socket.getInputStream());
            reciveTask.start();
            //启用发送数据线程
            sendTask = new SendTask(socket.getOutputStream());
            sendTask.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //接收数据
    public class ReciveTask extends Thread{
        private InputStream inputStream;
        private boolean isCancle;
        public ReciveTask(InputStream inputStream) {
            this.inputStream = inputStream;
        }
        @Override
        public void run() {
            while (!isCancle){
                if(!isConnectedFail()){
                    isCancle = true;
                    break;
                }
                BasicProtocol dataProtocol = SocketUnit.redFromStream(inputStream);

                if (dataProtocol != null){
                    if (dataProtocol.getProtocolType() == 0){
                        System.out.println(dataProtocol.printProtocol());
                        DataAckProtocol dataAck = new DataAckProtocol();
                        dataAck.setContentData("服务器回复测试数据DataAckResponse");
                        dataQueue.offer(dataAck);
                        toNotifyAll(dataQueue); //唤醒发送线程
                    }else if (dataProtocol.getProtocolType() == 2) {
                        System.out.println(dataProtocol.printProtocol());
                        PingAckProtocol pingAck = new PingAckProtocol();
                        pingAck.setContentData("服务器回复心跳pingdataResponse:" + dataProtocol.printProtocol());
                        dataQueue.offer(pingAck);
                        toNotifyAll(dataQueue); //唤醒发送线程

                    }

                }else {
                    System.out.println("client is offline...");
                    break;
                }
            }
            SocketUnit.closeInputStream(inputStream);
        }
    }
    //发送数据
    public class SendTask extends Thread{
        private OutputStream outputStream;
        private boolean isCancle;
        public SendTask(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            while (!isCancle){
                if(!isConnectedFail()){
                    isCancle = true;
                    break;
                }
                //无界线程安全队列轮询
                BasicProtocol procotol = dataQueue.poll();
                if (procotol == null) {
                    //进入队列待待
                    toWaitAll(dataQueue);
                } else if (outputStream != null) {
                    synchronized (outputStream) {
                        SocketUnit.write2Stream(procotol, outputStream);
                    }
                }
            }
            SocketUnit.closeOutputStream(outputStream);
        }
    }
    //队列等待
    public void toWaitAll(Object o) {
        synchronized (o) {
            try {
                o.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    //唤醒等待
    public void toNotifyAll(Object obj) {
        synchronized (obj) {
            obj.notifyAll();
        }
    }
    public void stopServerThread() {
        if (reciveTask != null) {
            reciveTask.isCancle = true;
            reciveTask.interrupt();
            if (reciveTask.inputStream != null) {
                SocketUnit.closeInputStream(reciveTask.inputStream);
                reciveTask.inputStream = null;
            }
            reciveTask = null;
        }

        if (sendTask != null) {
            sendTask.isCancle = true;
            sendTask.interrupt();
            if (sendTask.outputStream != null) {
                synchronized (sendTask.outputStream) {//防止写数据时停止，写完再停
                    sendTask.outputStream = null;
                }
            }
            sendTask = null;
        }
    }
    private boolean isConnectedFail() {
        if (socket.isClosed() || !socket.isConnected()) {
            MyServerHandler.this.stopServerThread();
            System.out.println("socket closed...");
            return false;
        }
        return true;
    }
}
