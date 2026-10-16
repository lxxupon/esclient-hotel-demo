package cn.itcast.hotel;

import cn.itcast.hotel.constants.HotelIndexConstants;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class HotelIndexTest {

    @Autowired
    private ElasticsearchClient client;

    @Autowired
    private ElasticsearchAsyncClient asyncClient;

    @Test
    void testGetIndex() throws IOException {
        GetIndexResponse getIndexResponse = client.indices().get(r -> r.index("shali"));
        System.out.println(getIndexResponse);
    }

    @Test
    void testCreateIndex() throws IOException {
        // PUT /hotel   注意：ik 与 pinyin 需要 ES 节点已安装对应插件
        client.indices().create(c -> c
                .index(HotelIndexConstants.INDEX_NAME)
                .settings(HotelIndexConstants.HOTEL_SETTINGS)
                .mappings(HotelIndexConstants.HOTEL_MAPPINGS)
        );
    }

    TypeMapping shali = TypeMapping.of(r ->
            r.properties("name", p -> p.text(k -> k.analyzer("ik_max_word").searchAnalyzer("text_anlyzer").copyTo("all")))
                    .properties("age", a->a.integer(t->t))
                    .properties("address", t->t.keyword(a->a.index(false)))
                    .properties("work", a->a.keyword(b->b))
    );

    @Test
    void testCreateIndex2() throws IOException {
        // PUT /hotel   注意：ik 与 pinyin 需要 ES 节点已安装对应插件
        client.indices().create(c -> c
                .index("shali")
                .settings(HotelIndexConstants.HOTEL_SETTINGS)
                .mappings(shali)
        );
    }

    @Test
    void testExistsIndex() throws IOException {
        boolean isExists = client.indices()
                .exists(e -> e.index(HotelIndexConstants.INDEX_NAME))
                .value();

        System.out.println(isExists ? "存在" : "不存在");

        asyncClient.indices().exists(e -> e.index(HotelIndexConstants.INDEX_NAME))
                .whenCompleteAsync((response, exception) -> {
                    if (exception != null) {
                        System.out.println("异步客户端：异常");
                    } else {
                        if (response.value()) {
                            System.out.println("异步客户端：存在");
                        } else {
                            System.out.println("异步客户端：不存在");
                        }
                    }
                });
        System.out.println("异步客户端：complete");
    }

    @Test
    void testDeleteIndex() throws IOException {
        // DELETE /hotel
        client.indices().delete(d -> d.index(HotelIndexConstants.INDEX_NAME));
    }
}
