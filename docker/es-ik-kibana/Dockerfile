# ES 7.17.23 + IK + 拼音 中文分词（离线安装：插件 jar 与词典已随工程拷贝，构建不依赖外网）
# 版本来源：本机原生安装 F:\es\elasticsearch-7.17.23\plugins\analysis-ik / analysis-pinyin
# docker.elastic.co 内网连不上时用 Docker Hub 的同名官方镜像（均为 Elastic 发布）
FROM elasticsearch:7.17.23

COPY ik-plugin /usr/share/elasticsearch/plugins/analysis-ik/
COPY pinyin-plugin /usr/share/elasticsearch/plugins/analysis-pinyin/
