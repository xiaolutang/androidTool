package com.example.txl.tool.syn;



/**
 * Copyright (c) 2020 唐小陆 All rights reserved.
 * author：txl
 * date：2020/5/24
 * description：学习java同步
 *
 */
public class SynchronizedTest {
    public static String TAG = "SynchronizedTest";
    public static void main(String[] args){
        demoSynchronizedFiled();
    }

    /**
     * 同一个实例对象
     * */
    private static void demoSynchronizedMethod(){
        final SynchronizedDemo synchronizedDemo = new SynchronizedDemo();
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronizedDemo.test();
            }
        },"Thread 1 ::");
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronizedDemo.test();
            }
        },"Thread 2 ::");
        thread1.start();
        thread2.start();
    }
    /**
     * 测试不同的实例对象
     * */
    private static void demoSynchronizedMethod2(){
        final SynchronizedDemo synchronizedDemo = new SynchronizedDemo();
        final SynchronizedDemo synchronizedDemo2 = new SynchronizedDemo();
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronizedDemo.test();
            }
        },"Thread 1 ::");
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronizedDemo2.test();
            }
        },"Thread 2 ::");
        thread1.start();
        thread2.start();
    }

    /**
     * 测试同步static方法
     * */
    private static void demoSynchronizedStaticMethod(){
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                SynchronizedDemo.test1();
            }
        },"Thread 1 ::");
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                SynchronizedDemo.test1();
            }
        },"Thread 2 ::");
        thread1.start();
        thread2.start();
    }

    private static void demoSynchronizedFiled(){
        final SynchronizedDemo synchronizedDemo = new SynchronizedDemo();
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronizedFiled(synchronizedDemo);
            }
        },"Thread 1 ::");
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronizedFiled(synchronizedDemo);
            }
        },"Thread 2 ::");
        thread1.start();
        thread2.start();
    }

    private static void synchronizedFiled(SynchronizedDemo synchronizedDemo){
//        synchronizedDemo.synchronizedObject();
//        synchronizedDemo.synchronizedStaticObject();
        synchronizedDemo.synchronizedClass();
    }
}
class SynchronizedDemo{
    public static String TAG = "SynchronizedTest";

    private final Object object = new Object();

    private final static Object staticObject = new Object();

    /**
     * 同步方法
     * */
    public synchronized void test(){
        System.out.println(TAG+"线程 "+Thread.currentThread().getName() +"获得锁");
        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(TAG+Thread.currentThread().getName()+"  执行test");
    }

    /**
     * 同步静态方法
     * */
    public static synchronized void test1(){
        System.out.println(TAG+"线程 "+Thread.currentThread().getName() +"获得锁");
        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(TAG+Thread.currentThread().getName()+"  执行test1");
    }

    public void synchronizedObject(){
        synchronized (object){
            System.out.println(TAG+"线程 "+Thread.currentThread().getName() +"获得锁");
            try {
                Thread.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(TAG+Thread.currentThread().getName()+"  执行test");
        }
    }

    public void synchronizedStaticObject(){
        synchronized (staticObject){
            System.out.println(TAG+"线程 "+Thread.currentThread().getName() +"获得锁");
            try {
                Thread.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(TAG+Thread.currentThread().getName()+"  执行test");
        }
    }

    public void synchronizedClass(){
        synchronized (SynchronizedDemo.class){
            System.out.println(TAG+"线程 "+Thread.currentThread().getName() +"获得锁");
            try {
                Thread.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(TAG+Thread.currentThread().getName()+"  执行test");
        }
    }
}
