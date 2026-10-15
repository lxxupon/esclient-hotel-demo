# ES + IK + Kibana (Docker Desktop 本地开发环境)

三件套版本严格一致：**Elasticsearch 7.17.23 / IK 7.17.23 / Pinyin 7.17.23 / Kibana 7.17.23**

## 目录结构

```
docker/
├── docker-compose.yml        # 编排文件（ES + Kibana）
├── Dockerfile                # ES 基础镜像 + IK / Pinyin 插件离线安装
├── ik-plugin/                # IK 插件 jar（已含在工程内，构建不需要外网）
├── pinyin-plugin/            # 拼音分词插件 jar（同上，离线安装）
├── ik-config/                # IK 词典（bind-mount 进容器，改完重启容器生效）
│   └── extra_main.dic        #   ← 自定义业务词条加在这个文件里（UTF-8 无 BOM，一词一行）
├── es-config/elasticsearch.yml
└── kibana-config/kibana.yml
```

## 使用

```bash
docker compose up -d --build   # 构建并启动（首次拉镜像较慢）
docker compose ps              # ES 变 healthy 后 Kibana 才会启动
```

- Elasticsearch: http://localhost:9200
- Kibana（中文界面）: http://localhost:5601 → 左侧 Management → Dev Tools 写 DSL

## 常见操作

| 场景 | 操作 |
|------|------|
| 加自定义词条 | 编辑 `ik-config/extra_main.dic`，然后 `docker compose restart elasticsearch` |
| 改 ES/Kibana 配置 | 改对应 yml，然后 `docker compose restart elasticsearch kibana` |
| 停止/清数据 | `docker compose down`（数据保留）/ `docker compose down -v`（清空） |
| 看日志 | `docker compose logs -f elasticsearch` |

## 注意事项

- 占用本机 9200/5601 端口，与 Windows 原生安装的 ES 不能同时启动。
- 数据保存在命名卷 `es-data`，删除容器不丢数据；`down -v` 才会清空。
- 单节点环境已在集群里建了索引模板 `dev-replicas-0`（所有索引副本数为 0），避免 yellow；
  若执行过 `down -v` 清了数据，重建后需重新执行一次：
  `PUT _template/dev-replicas-0 {"index_patterns":["*"],"order":0,"settings":{"number_of_replicas":0}}`
- 生产环境不要复用本配置（需开 xpack.security 认证、集群部署、独立服务器调内核参数）。
