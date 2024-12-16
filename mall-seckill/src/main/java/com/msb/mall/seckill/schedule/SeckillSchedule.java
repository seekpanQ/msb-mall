package com.msb.mall.seckill.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务
 * 1. @EnableScheduling 开启定时任务
 * 2. @Scheduled 具体开启一个定时任务  通过corn表达式来定时
 */
@Component
@Slf4j
public class SeckillSchedule {

    /**
     * 默认情况下定时任务是一个同步的任务，那我们需要他能够异步的去处理
     * 1.我们可以把需要定时执行的任务交给异步处理器来处理
     * 2.我们还可以把需要执行的方法异步执行
     *
     * @EnableAsync 开启异步任务的功能
     */
    @Async
    @Scheduled(cron = "* * 2 * * *")
    public void schedule() {
        log.info("定时任务......");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
