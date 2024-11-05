package com.msb.mall.search.thread;

import java.util.concurrent.*;

/**
 * 两个任务完成一个
 * 在上面5个基础上我们来看看两个任务只要有一个完成就会触发任务3的情况
 * <p>
 * * runAfterEither:不能获取完成的线程的返回结果，自身也没有返回结果
 * * acceptEither:可以获取线程的返回结果，自身没有返回结果
 * * applyToEither:既可以获取线程的返回结果，自身也有返回结果
 */
public class CompletableFutureDemo5 {

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(5
            , 50
            , 10
            , TimeUnit.SECONDS
            , new LinkedBlockingQueue<>(100)
            , Executors.defaultThreadFactory()
            , new ThreadPoolExecutor.AbortPolicy()
    );

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务1 线程开始了..." + Thread.currentThread().getName());
            int i = 100 / 5;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("任务1 线程结束了..." + Thread.currentThread().getName());
            return i;
        }, executor);

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务2 线程开始了..." + Thread.currentThread().getName());
            int i = 100 / 10;
            System.out.println("任务2 线程结束了..." + Thread.currentThread().getName());
            return i;
        }, executor);

        // runAfterEitherAsync 不能获取前面完成的线程的返回结果，自身也没有返回结果
 /*       CompletableFuture<Void> future = future1.runAfterEitherAsync(future2, () -> {
            System.out.println("任务3执行了....");
        });*/

        // acceptEitherAsync 可以获取前面完成的线程的返回结果  自身没有返回结果
 /*       CompletableFuture<Void> future = future1.acceptEitherAsync(future2, res -> {
            System.out.println("res = " + res);
        }, executor);*/

        // applyToEitherAsync 既可以获取完成任务的线程的返回结果  自身也有返回结果
        CompletableFuture<Object> future = future1.applyToEitherAsync(future2, res -> {
            System.out.println("res = " + res);
            return res + "-->OK";
        }, executor);

        // 可以处理异步任务之后的操作
        System.out.println("获取的线程的返回结果是：" + future.join());
    }


}
