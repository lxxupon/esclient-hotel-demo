package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SpringBootTest
class HotelDocumentTest {

    private static final String INDEX = "hotel";

    @Autowired
    private ElasticsearchClient client;

    @Autowired
    private IHotelService hotelService;

    @Test
    void testAddDocument() throws IOException {
        // 1.查询数据库hotel数据
        Hotel hotel = hotelService.getById(61083L);
        // 2.转换为HotelDoc，客户端内部完成 JSON 序列化
        HotelDoc hotelDoc = new HotelDoc(hotel);

        // PUT /hotel/_doc/{id}
        client.index(i -> i
                .index(INDEX)
                .id(hotelDoc.getId().toString())
                .document(hotelDoc));
    }

    @Test
    void testGetDocumentById() throws IOException {
        // GET /hotel/_doc/{id}
        HotelDoc hotelDoc = client
                .get(g -> g.index(INDEX).id("61083"), HotelDoc.class)
                .source();

        System.out.println("hotelDoc = " + hotelDoc);
    }

    @Test
    void testDeleteDocumentById() throws IOException {
        // DELETE /hotel/_doc/61083
        client.delete(d -> d.index(INDEX).id("61083"));
    }

    @Test
    void testUpdateById() throws IOException {
        // POST /hotel/_update/61083，partial doc 只需包含要修改的字段
        client.update(u -> u
                .index(INDEX)
                .id("61083")
                .doc(Collections.singletonMap("price", "870")), HotelDoc.class);
    }

    @Test
    void testBulkRequest() throws IOException {
        // 查询所有的酒店数据
        List<Hotel> list = hotelService.list();

        List<BulkOperation> operations = new ArrayList<>(list.size());
        for (Hotel hotel : list) {
            HotelDoc hotelDoc = new HotelDoc(hotel);
            operations.add(BulkOperation.of(o -> o
                    .index(i -> i
                            .index(INDEX)
                            .id(hotel.getId().toString())
                            .document(hotelDoc))));
        }

        client.bulk(b -> b.operations(operations));
    }
}
