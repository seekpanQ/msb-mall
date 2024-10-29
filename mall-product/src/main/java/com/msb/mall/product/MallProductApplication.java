package com.msb.mall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableCaching
@EnableFeignClients(basePackages = "com.msb.mall.product.feign")
@SpringBootApplication
// 指定Mapper接口对应的路径
@MapperScan("com.msb.mall.product.dao")
@EnableDiscoveryClient
@ComponentScan(basePackages = "com.msb")
@EnableTransactionManagement//开启事务
public class MallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallProductApplication.class, args);
    }

}
