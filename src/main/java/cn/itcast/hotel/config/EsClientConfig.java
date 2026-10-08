package cn.itcast.hotel.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * ES 客户端统一配置。
 *
 * 关键点：
 * 1. 全局单例 Bean，业务层只注入不 new，避免每次请求重建连接池；
 * 2. destroyMethod = "close"，容器关闭时自动释放连接池，业务代码不用管；
 * 3. 连接参数从配置文件读取，不硬编码。
 */
@Slf4j
@Configuration
public class EsClientConfig {

    /**
     * 高层客户端：带 Request/Response API 的门面，注入给业务层用的就是它
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(RestHighLevelClient.class)
    public RestHighLevelClient restHighLevelClient(EsProperties properties) {
        List<HttpHost> hosts = new ArrayList<>();
        for (String host : properties.getHosts()) {
            hosts.add(HttpHost.create(host));
        }

        RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]));

        if (properties.getUsername() != null && !properties.getUsername().isEmpty()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword())
            );
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return new RestHighLevelClient(builder);
    }

    /**
     * 自定义连接池（可选，需要精细调优时才用）
     */
    public CloseableHttpClient buildHttpClient(EsProperties properties) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(properties.getMaxConnTotal());
        connectionManager.setDefaultMaxPerRoute(properties.getMaxConnPerRoute());

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }
}
