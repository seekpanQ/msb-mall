package com.msb.mall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ElasticSearch的配置类
 */
@Configuration
public class MallElasticSearchConfiguration {

    @Bean
    public RestHighLevelClient restHighLevelClient() {

        RestClientBuilder builder = RestClient.builder(
                new HttpHost("192.168.91.131", 9200, "http"));
        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }
}
