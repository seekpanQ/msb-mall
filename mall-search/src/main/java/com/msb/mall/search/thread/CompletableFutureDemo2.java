package com.msb.mall.search.thread;

import java.util.concurrent.*;

/**
 * CompletableFuture的介绍
 * * whenComplete 可以获取异步任务的返回值和抛出的异常信息，但是不能修改返回结果
 * * execptionlly 当异步任务跑出了异常后会触发的方法，如果没有抛出异常该方法不会执行
 * * handle 可以获取异步任务的返回值和抛出的异常信息，而且可以显示的修改返回的结果
 */
public class CompletableFutureDemo2 {

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(5
            , 50
            , 10
            , TimeUnit.SECONDS
            , new LinkedBlockingQueue<>(100)
            , Executors.defaultThreadFactory()
            , new ThreadPoolExecutor.AbortPolicy()
    );


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("线程开始了...");
            int i = 100 / 5;
            System.out.println("线程结束了...");
            return i;
        }, executor).handle((res, exec) -> {
            System.out.println("res = " + res + ":exec=" + exec);
            return res * 10;
        });
        // 可以处理异步任务之后的操作
        System.out.println("获取的线程的返回结果是：" + future.get());
    }

 /*   public static void main(String[] args) throws ExecutionException, InterruptedException {

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("线程开始了...");
            int i = 100 / 5;
            System.out.println("线程结束了...");
            return i;
        }, executor).whenCompleteAsync((res, exec) -> {
            System.out.println("res = " + res);
            System.out.println("exec = " + exec);
        }).exceptionally(res -> {// 在异步任务显示的抛出了异常后才会触发的方法
            System.out.println("res = " + res);
            return 10;
        });
        // 可以处理异步任务之后的操作
        System.out.println("获取的线程的返回结果是：" + future.get());
    }*/

    /*    public static void main(String[] args) throws ExecutionException, InterruptedException {

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("线程开始了...");
            int i = 100 / 0;
            System.out.println("线程结束了...");
            return i;
        }, executor).whenCompleteAsync((res,exec)->{
            System.out.println("res = " + res);
            System.out.println("exec = " + exec);
        });
        // 可以处理异步任务之后的操作
        System.out.println("获取的线程的返回结果是：" + future.get() );
    }*/
}
