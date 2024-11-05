package com.msb.mall.search.thread;

import java.util.concurrent.*;

/**
 * 线程串行方法
 * CompletableFuture的介绍
 * thenApply 方法：当一个线程依赖另一个线程时，获取上一个任务返回的结果，并返回当前任务的返回值。
 * <p>
 * thenAccept方法：消费处理结果。接收任务的处理结果，并消费处理，无返回结果。
 * <p>
 * thenRun方法：只要上面的任务执行完成，就开始执行thenRun，只是处理完任务后，执行 thenRun的后续操作
 * <p>
 * 带有Async默认是异步执行的。这里所谓的异步指的是不在当前线程内执行。
 */
public class CompletableFutureDemo3 {

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(5
            , 50
            , 10
            , TimeUnit.SECONDS
            , new LinkedBlockingQueue<>(100)
            , Executors.defaultThreadFactory()
            , new ThreadPoolExecutor.AbortPolicy()
    );


/*    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("线程开始了...");
            int i = 100 / 5;
            System.out.println("线程结束了...");
            return i;
        }, executor).thenAcceptAsync(res -> {
            System.out.println(res + ":" + Thread.currentThread().getName());
        }, executor);
        // 可以处理异步任务之后的操作
        System.out.println("获取的线程的返回结果是：" + future.get());
    }*/

/*    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("线程开始了...");
            int i = 100 / 5;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("线程结束了...");
            return i;
        }, executor).thenRunAsync(() -> {
            System.out.println("线程开始了..." + Thread.currentThread().getName());
            int i = 100 / 10;
            System.out.println("线程结束了..." + Thread.currentThread().getName());
        }, executor);
        // 可以处理异步任务之后的操作
        System.out.println("获取的线程的返回结果是：" + future.get());
    }*/

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("线程开始了...");
            int i = 100 / 5;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("线程结束了...");
            return i;
        }, executor).thenApplyAsync(res -> {
            System.out.println("res = " + res);
            return res * 100;
        });
        // 可以处理异步任务之后的操作
        System.out.println("获取的线程的返回结果是：" + future.get());
    }


}
