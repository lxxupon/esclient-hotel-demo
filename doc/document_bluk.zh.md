# 批量索引或删除文档


**PUT /{index}/_bulk**

**此操作的所有方法和路径：**

<div>
                      <span class="operation-verb post">POST</span>
                      <span class="operation-path">/_bulk</span>
                      </div>
                    <div>
                      <span class="operation-verb put">PUT</span>
                      <span class="operation-path">/_bulk</span>
                      </div>
                    <div>
                      <span class="operation-verb post">POST</span>
                      <span class="operation-path">/{index}/_bulk</span>
                      </div>
                    <div>
                      <span class="operation-verb put">PUT</span>
                      <span class="operation-path">/{index}/_bulk</span>
                      </div>


在单个请求中执行多个 `index`、`create`、`delete` 和 `update` 操作。
这可以减少开销，大幅提高索引速度。

如果启用了 Elasticsearch 安全功能，您必须对目标数据流、索引或索引别名拥有以下索引权限：

* 要使用 `create` 操作，您必须拥有 `create_doc`、`create`、`index` 或 `write` 索引权限。数据流仅支持 `create` 操作。
* 要使用 `index` 操作，您必须拥有 `create`、`index` 或 `write` 索引权限。
* 要使用 `delete` 操作，您必须拥有 `delete` 或 `write` 索引权限。
* 要使用 `update` 操作，您必须拥有 `index` 或 `write` 索引权限。
* 要使用批量 API 请求自动创建数据流或索引，您必须拥有 `auto_configure`、`create_index` 或 `manage` 索引权限。
* 要使用 `refresh` 参数使批量操作的结果可被搜索，您必须拥有 `maintenance` 或 `manage` 索引权限。

自动创建数据流需要匹配一个启用了数据流的索引模板。

操作在请求体中使用换行符分隔的 JSON（NDJSON）结构指定：

```
action_and_meta_data\n
optional_source\n
action_and_meta_data\n
optional_source\n
....
action_and_meta_data\n
optional_source\n
```

`index` 和 `create` 操作在下一行需要一个源文档，其语义与标准索引 API 中的 `op_type` 参数相同。
如果目标中已存在具有相同 ID 的文档，`create` 操作将失败。
`index` 操作会根据需要添加或替换文档。

注意：数据流仅支持 `create` 操作。
要更新或删除数据流中的文档，您必须定位到包含该文档的后备索引。

`update` 操作要求在下一行指定部分文档、upsert 以及脚本及其选项。

`delete` 操作在下一行不需要源文档，其语义与标准删除 API 相同。

注意：数据的最后一行必须以换行符（`\n`）结尾。
每个换行符之前可以有一个回车符（`\r`）。
向 `_bulk` 端点发送 NDJSON 数据时，请使用 `Content-Type` 头为 `application/json` 或 `application/x-ndjson`。
由于此格式使用文字换行符（`\n`）作为分隔符，请确保 JSON 操作和源数据没有经过格式化打印（pretty print）。

如果您在请求路径中提供了目标索引，它将用于任何未显式指定 `_index` 参数的操作。

关于格式的说明：这里的想法是让处理尽可能快。
由于某些操作会被重定向到其他节点上的其他分片，因此只有 `action_meta_data` 在接收节点端被解析。

使用此协议的客户端库应该努力在客户端做类似的事情，并尽可能减少缓冲。

单个批量请求中执行的操作数量没有"正确"的数字。
尝试不同的设置，为您的特定工作负载找到最佳大小。
请注意，Elasticsearch 默认将 HTTP 请求的最大大小限制为 100MB，因此客户端必须确保任何请求都不超过此大小。
不可能索引单个超过大小限制的文档，因此您必须在发送到 Elasticsearch 之前将任何此类文档预处理成较小的片段。
例如，在索引之前将文档拆分为页面或章节，或者将原始二进制数据存储在 Elasticsearch 之外的系统中，并在发送给 Elasticsearch 的文档中用指向外部系统的链接替换原始数据。

**客户端对批量请求的支持**

一些官方支持的客户端提供了帮助程序来协助批量请求和重新索引：

* Go：请查看 `esutil.BulkIndexer`
* Perl：请查看 `Search::Elasticsearch::Client::5_0::Bulk` 和 `Search::Elasticsearch::Client::5_0::Scroll`
* Python：请查看 `elasticsearch.helpers.*`
* JavaScript：请查看 `client.helpers.*`
* Java：请查看 `co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester`
* .NET：请查看 `BulkAllObservable`
* PHP：请查看批量索引。
* Ruby：请查看 `Elasticsearch::Helpers::BulkHelper`

**使用 cURL 提交批量请求**

如果您向 `curl` 提供文本文件输入，必须使用 `--data-binary` 标志而不是普通的 `-d`。
后者不会保留换行符。例如：

```
$ cat requests
{ "index" : { "_index" : "test", "_id" : "1" } }
{ "field1" : "value1" }
$ curl -s -H "Content-Type: application/x-ndjson" -XPOST localhost:9200/_bulk --data-binary "@requests"; echo
{"took":7, "errors": false, "items":[{"index":{"_index":"test","_id":"1","_version":1,"result":"created","forced_refresh":false}}]}
```

**乐观并发控制**

批量 API 调用中的每个 `index` 和 `delete` 操作都可以在其各自的操作和元数据行中包含 `if_seq_no` 和 `if_primary_term` 参数。
`if_seq_no` 和 `if_primary_term` 参数根据对现有文档的最后修改来控制操作的运行方式。有关更多详细信息，请参阅乐观并发控制。

**版本控制**

每个批量项都可以使用 `version` 字段包含版本值。
它会根据 `_version` 映射自动遵循索引或删除操作的行为。
它还支持 `version_type`。

**路由**

每个批量项都可以使用 `routing` 字段包含路由值。
它会根据 `_routing` 映射自动遵循索引或删除操作的行为。

注意：数据流不支持自定义路由，除非它们是在模板中启用了 `allow_custom_routing` 设置的情况下创建的。

**刷新**

控制此请求所做的更改何时可被搜索可见。

注意：只有接收批量请求的分片才会受到刷新的影响。
想象一下，一个包含三个文档的 `_bulk?refresh=wait_for` 请求恰好被路由到一个有五个分片的索引中的不同分片。
该请求只会等待这三个分片刷新。
构成索引的其他两个分片根本不参与 `_bulk` 请求。

您可能希望暂时禁用刷新间隔，以提高大型批量请求的索引吞吐量。
有关使用索引设置 API 的分步说明，请参阅链接的文档。

[关于调整索引速度](https://www.elastic.co/docs/deploy-manage/production-guidance/optimize-performance/indexing-speed#disable-refresh-interval)

## 服务器
- http://api.example.com: http://api.example.com ()


## 认证方式
- Api key 认证
- Basic 认证
- Bearer 认证


## 参数

### 路径参数

- **index** (字符串)
  要执行批量操作的数据流、索引或索引别名的名称。


### 查询参数

- **include_source_on_error** (布尔值)
  如果为 true 或 false，表示在解析错误的情况下是否在错误消息中包含文档源。

- **list_executed_pipelines** (布尔值)
  如果为 `true`，响应将包含为每个索引或创建操作运行的摄取管道。

- **pipeline** (字符串)
  用于预处理传入文档的管道标识符。
  如果索引指定了默认摄取管道，将值设置为 `_none` 将关闭此请求的默认摄取管道。
  如果配置了最终管道，无论此参数的值如何，它都将始终运行。

- **refresh** (字符串)
  如果为 `true`，Elasticsearch 会刷新受影响的分片，使此操作可被搜索可见。
  如果为 `wait_for`，等待刷新以使此操作可被搜索可见。
  如果为 `false`，不执行任何刷新操作。
  有效值：`true`、`false`、`wait_for`。

- **routing** (字符串 | 字符串数组)
  用于将操作路由到特定分片的自定义值。
  当目标索引的 `index.slice.enabled` 为 `true` 时不允许使用；请改用 `_slice`。

- **_source** (布尔值 | 字符串 | 字符串数组)
  指示是否返回 `_source` 字段（`true` 或 `false`）或包含要返回的字段列表。

- **_source_excludes** (字符串 | 字符串数组)
  要从响应中排除的源字段的逗号分隔列表。
  您也可以使用此参数从 `_source_includes` 查询参数指定的子集中排除字段。
  如果 `_source` 参数为 `false`，则忽略此参数。

- **_source_includes** (字符串 | 字符串数组)
  要包含在响应中的源字段的逗号分隔列表。
  如果指定了此参数，则仅返回这些源字段。
  您可以使用 `_source_excludes` 查询参数从此子集中排除字段。
  如果 `_source` 参数为 `false`，则忽略此参数。

- **timeout** (字符串)
  每个操作等待以下操作的时间段：自动创建索引、动态映射更新和等待活动分片。
  默认值为 `1m`（一分钟），这保证 Elasticsearch 在失败前至少等待超时时间。
  实际等待时间可能更长，特别是当发生多次等待时。

- **wait_for_active_shards** ()
  在继续操作之前必须处于活动状态的分片副本数。
  设置为 `all` 或任何不超过索引中总分片数（`number_of_replicas+1`）的正整数。
  默认值为 `1`，即等待每个主分片处于活动状态。

- **require_alias** (布尔值)
  如果为 `true`，请求的操作必须以索引别名为目标。

- **require_data_stream** (布尔值)
  如果为 `true`，请求的操作必须以数据流为目标（现有或待创建）。


### 请求体: application/json (对象数组)
请求体包含换行符分隔的 `create`、`delete`、`index` 和 `update` 操作及其关联的源数据列表。
列表中的每个项目都是三种 NDJSON 行之一：

* 操作行，指定要执行的操作（`index`、`create`、`update` 或 `delete`）及其元数据。例如：`{ "index": { "_index": "my-index", "_id": "1" } }`。
* 更新源行，必须跟在 `update` 操作行之后。它包含要应用的部分文档、脚本或 upsert 选项。
* 文档源行，必须跟在 `index` 或 `create` 操作行之后。它包含要索引的文档。

`delete` 操作后面不跟源行。



## 响应
### 200


#### 响应体: application/json (对象)
- **errors** (布尔值)
  如果为 `true`，则批量请求中的一个或多个操作未成功完成。

- **items** (对象数组)
  批量请求中每个操作的结果，按提交顺序排列。

- **took** (数字)
  处理批量请求所花费的时间，以毫秒为单位。

- **ingest_took** (数字)



[由 Bump.sh 提供支持](https://bump.sh)
