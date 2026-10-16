# 项目长期记忆

## 环境
- 本机默认 JDK 是 21，**该项目用 JDK 8**（Spring Boot 2.3.10 + 老版 Lombok）。编译前必须指定：
  `JAVA_HOME="/c/Program Files/Java/jdk1.8.0_291" mvn compile`
- JDK 21 下会报 `JCImport does not have member field qualid`，这是 Lombok 版本问题，不是代码问题。
- Maven 默认使用 F:\devtools\apache-maven-3.9.14。

## Elasticsearch
- 版本 7.17.23，使用官方 Java API Client（`co.elastic.clients` 的 `ElasticsearchClient` / `ElasticsearchAsyncClient`），**已弃用 `RestHighLevelClient`**；低层 `RestClient` 仅作传输层。
- 客户端统一由 `config/EsClientConfig` 建为单例 Bean：`RestClient`（`destroyMethod = "close"`，负责连接池释放）+ `ElasticsearchClient` + `ElasticsearchAsyncClient`（后两者经 `RestClientTransport` 构建），配置来自 `application.yaml` 的 `es.hosts`。
- `EsClientConfig` 用自定义 `JacksonJsonpMapper`（跳过 null 字段、忽略未知字段）完成 `_source` 与 POJO 转换。
