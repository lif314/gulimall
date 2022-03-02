package com.lif314.gulimall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GulimallElasticSearchConfig {

    @Bean   // 注入容器
    public RestHighLevelClient esRestClient(){
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("81.56.236.154", 9200, "http")));
    }

}
