package com.test02;

public class TestGetThreadName {
    public static void getName(){
        Thread threadName = Thread.currentThread();
        System.out.println("--线程名:--:" + threadName.getName());
    }
}