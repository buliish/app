package com.gec.seafood_traceability_system;

import org.junit.jupiter.api.Test;

public class ThreadLocalTest {

    @Test 
    public void testThreadLocalSetAndget(){
        //提供一个ThreadLocal对象
        ThreadLocal tl = new ThreadLocal();
        
        //开启两个线程
        new Thread(()->{
            tl.set("张三");
            System.out.println(Thread.currentThread().getName() + ":" +tl.get());
            System.out.println(Thread.currentThread().getName() + ":" +tl.get());
            System.out.println(Thread.currentThread().getName() + ":" +tl.get());
        },"用户一").start();
        
        new Thread(()->{
            tl.set("李四");
            System.out.println(Thread.currentThread().getName() + ":" +tl.get());
            System.out.println(Thread.currentThread().getName() + ":" +tl.get());
            System.out.println(Thread.currentThread().getName() + ":" +tl.get());
        },"用户二").start();
    }
}