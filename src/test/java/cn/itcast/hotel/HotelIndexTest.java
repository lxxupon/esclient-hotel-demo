package cn.itcast.hotel;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import cn.itcast.hotel.constants.HotelIndexConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class HotelIndexTest {

    @Autowired
    private ElasticsearchClient client;

    @Test
    void testCreateIndex() throws IOException {
        // PUT /hotel   注意：ik 分词器需要 ES 节点已安装 ik 插件
        client.indices().create(c -> c
                .index(HotelIndexConstants.INDEX_NAME)
                .mappings(HotelIndexConstants.HOTEL_MAPPINGS));
    }

    @Test
    void testExistsIndex() throws IOException {
        boolean isExists = client.indices()
                .exists(e -> e.index(HotelIndexConstants.INDEX_NAME))
                .value();

        System.out.println(isExists ? "存在" : "不存在");
    }

    @Test
    void testDeleteIndex() throws IOException {
        // DELETE /hotel
        client.indices().delete(d -> d.index(HotelIndexConstants.INDEX_NAME));
    }
}
