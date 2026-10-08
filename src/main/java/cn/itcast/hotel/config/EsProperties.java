package cn.itcast.hotel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ES 连接参数，值全部来自 application.yaml 中的 es.* 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "es")
public class EsProperties {

    /**
     * 集群节点地址，格式 http://host:9200
     */
    private List<String> hosts;

    /**
     * 用户名，ES 开启 basic 认证时需要，7.x 本地免认证可留空
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * 读取超时（毫秒）
     */
    private int socketTimeout = 60000;

    /**
     * 每个节点的最大连接数
     */
    private int maxConnTotal = 100;

    /**
     * 每个节点的最大路由连接数
     */
    private int maxConnPerRoute = 50;
}
