package com.test02;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MyClientHandler implements Runnable {
    private Socket socket;
    private SendTask mSendTask;
    private ReciveTask mReciveTask;
    private HeartBeatTask mHeartBeatTask;
    private boolean isLongConnection = true;
    private boolean closeSendTask = false;
    protected volatile ConcurrentLinkedQueue<BasicProtocol> dataQueue = new ConcurrentLinkedQueue<>();
    public MyClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            //启动发数据线程
            SendTask sendTask = new SendTask(socket.getOutputStream());
            sendTask.start();
            //启动心跳发数据线程
            if(isLongConnection){
                HeartBeatTask heartBeatTask = new HeartBeatTask(socket.getOutputStream());
                heartBeatTask.start();
            }
            //启动收数据线程
            ReciveTask reciveTask = new ReciveTask(socket.getInputStream());
            reciveTask.start();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 待发送数据
     * @param data
     */
    public void addRequest(DataProtocol data) {
        dataQueue.offer(data);
        toNotifyAll(dataQueue);//有新增待发送数据，则唤醒发送线程
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
    public class ReciveTask extends Thread{
        private boolean isCancle = false;
        private InputStream inputStream;

        public ReciveTask(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            while (!isCancle){
                if(!isConnectedClient()){
                    break;
                }
                BasicProtocol dataProtocol = SocketUnit.redFromStream(inputStream);

                if (dataProtocol != null){
                    if (dataProtocol.getProtocolType() == 0){
                        System.out.println(dataProtocol.printProtocol());
                    }else if (dataProtocol.getProtocolType() == 1) {
                        System.out.println(dataProtocol.printProtocol());
                    }else if (dataProtocol.getProtocolType() == 2) {
                        System.out.println(dataProtocol.printProtocol());
                    }else if (dataProtocol.getProtocolType() == 3) {
                        System.out.println(dataProtocol.printProtocol());
                    }

                }else {
                    break;
                }
            }
            SocketUnit.closeInputStream(inputStream);//循环结束则退出输入流
        }
    }
    public class SendTask extends Thread{
        private boolean isCancle = false;
        private OutputStream outputStream;

        public SendTask(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            while (!isCancle){
                if(!isConnectedClient()){
                    break;
                }
                //无界线程安全队列轮询
                BasicProtocol procotol = dataQueue.poll();
                if (procotol == null) {
                    //进入队列待待
                    toWaitAll(dataQueue);
                    if (closeSendTask) {
                        closeSendTask();//notify()调用后，并不是马上就释放对象锁的，所以在此处中断发送线程
                    }
                } else if (outputStream != null) {
                    synchronized (outputStream) {
                        SocketUnit.write2Stream(procotol, outputStream);
                    }
                }
            }
            SocketUnit.closeOutputStream(outputStream);//循环结束则退出输出流
        }
    }
    public void toWaitAll(Object o) {
        synchronized (o) {
            try {
                o.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 心跳实现，频率5秒
     * Created by meishan on 16/12/1.
     */
    public class HeartBeatTask extends Thread {

        private static final int REPEATTIME = 5000;
        private boolean isCancle = false;
        private OutputStream outputStream;
        private int pingId;
        public HeartBeatTask(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            pingId = 1;
            while (!isCancle) {
                if (!isConnectedClient()) {
                    break;
                }

                if (outputStream != null) {
                    PingProtocol pingProtocol = new PingProtocol();
                    pingProtocol.setContentData("pingdataRequest");
                    pingProtocol.setPingId(pingId);
                    SocketUnit.write2Stream(pingProtocol, outputStream);
                    pingId = pingId + 2;
                }

                try {
                    Thread.sleep(REPEATTIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            SocketUnit.closeOutputStream(outputStream);//循环结束则退出输出流
        }
    }
    //停止所有
    public synchronized void stopClientThread() {

        //关闭接收线程
        closeReciveTask();

        //关闭发送线程
        closeSendTask = true;
        toNotifyAll(dataQueue);

        //关闭心跳线程
        closeHeartBeatTask();

        //关闭socket
        closeSocket();

        //清除数据
        clearData();
    }
    /**
     * 关闭接收线程
     */
    private void closeReciveTask() {
        if (mReciveTask != null) {
            mReciveTask.interrupt(); //停止一个线程
            mReciveTask.isCancle = true;
            if (mReciveTask.inputStream != null) {
                try {
                    if (!socket.isClosed() && socket.isConnected()) {
                        socket.shutdownInput();//解决java.net.SocketException问题，需要先shutdownInput
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SocketUnit.closeInputStream(mReciveTask.inputStream);
                mReciveTask.inputStream = null;
            }
            mReciveTask = null;
        }
    }

    /**
     * 关闭发送线程
     */
    private void closeSendTask() {
        if (mSendTask != null) {
            mSendTask.isCancle = true;
            mSendTask.interrupt(); //停止一个线程
            if (mSendTask.outputStream != null) {
                synchronized (mSendTask.outputStream) {//防止写数据时停止，写完再停
                    SocketUnit.closeOutputStream(mSendTask.outputStream);
                    mSendTask.outputStream = null;
                }
            }
            mSendTask = null;
        }
    }

    /**
     * 关闭心跳线程
     */
    private void closeHeartBeatTask() {
        if (mHeartBeatTask != null) {
            mHeartBeatTask.isCancle = true;
            if (mHeartBeatTask.outputStream != null) {
                SocketUnit.closeOutputStream(mHeartBeatTask.outputStream);
                mHeartBeatTask.outputStream = null;
            }
            mHeartBeatTask = null;
        }
    }

    /**
     * 关闭socket
     */
    private void closeSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 清除数据
     */
    private void clearData() {
        dataQueue.clear();
        isLongConnection = false;
    }
    //Socket是关闭,断连进行处理
    private boolean isConnectedClient() {
        if (socket.isClosed() || !socket.isConnected()) {
            MyClientHandler.this.stopClientThread();
            return false;
        }
        return true;
    }
    /**
     * notify()调用后，并不是马上就释放对象锁的，而是在相应的synchronized(){}语句块执行结束，自动释放锁后
     *
     * @param o
     */
    protected void toNotifyAll(Object o) {
        synchronized (o) {
            o.notifyAll();
        }
    }

}
