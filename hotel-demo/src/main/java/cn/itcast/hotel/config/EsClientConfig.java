package cn.itcast.hotel.config;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * ES 客户端统一配置，使用官方的 Java API Client（替代已废弃的 RestHighLevelClient）。
 *
 * 关键点：
 * 1. 全局单例 Bean，业务层只注入不 new，避免每次请求重建连接池；
 * 2. destroyMethod = "close" 挂在 RestClient 上，容器关闭时自动释放连接池，业务代码不用管；
 * 3. 连接参数从配置文件读取，不硬编码。
 */
@Slf4j
@Configuration
public class EsClientConfig {

    /**
     * 新的高层客户端：面向对象的请求/响应 API，底层仍走 RestClient 连接池。
     * 7.17 的 ElasticsearchClient 本身不实现 Closeable，连接池由下面的 RestClient Bean 负责释放。
     */
    @Bean
    @ConditionalOnMissingBean(ElasticsearchClient.class)
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ElasticsearchTransport transport = new RestClientTransport(restClient, buildJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    @Bean
    @ConditionalOnMissingBean(ElasticsearchAsyncClient.class)
    public ElasticsearchAsyncClient elasticsearchAsyncClient(RestClient restClient) {
        ElasticsearchTransport transport = new RestClientTransport(restClient, buildJsonpMapper());
        return new ElasticsearchAsyncClient(transport);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(RestClient.class)
    public RestClient elasticsearchRestClient(EsProperties properties) {
        List<HttpHost> hosts = new ArrayList<>();
        for (String host : properties.getHosts()) {
            hosts.add(HttpHost.create(host));
        }

        RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]));

        builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(properties.getConnectTimeout())
                .setSocketTimeout(properties.getSocketTimeout()));

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(properties.getMaxConnTotal());
            httpClientBuilder.setMaxConnPerRoute(properties.getMaxConnPerRoute());
            if (properties.getUsername() != null && !properties.getUsername().isEmpty()) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword())
                );
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            return httpClientBuilder;
        });

        return builder.build();
    }

    /**
     * 客户端用它完成 _source 与 POJO 的双向转换：跳过 null 字段，忽略索引中多出的字段
     */
    private JacksonJsonpMapper buildJsonpMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new JacksonJsonpMapper(objectMapper);
    }
}
