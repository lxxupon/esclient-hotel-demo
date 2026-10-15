package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
class HotelSearchTest {

    @Autowired
    private ElasticsearchClient client;

    @Test
    void testMatchAll() throws IOException {
        SearchResponse<HotelDoc> response = client.search(s -> s
                .index("hotel")
                .query(q -> q.matchAll(m -> m)), HotelDoc.class);
        handleResponse(response);
    }

    @Test
    void testMatch() throws IOException {
        // SearchResponse<HotelDoc> response = client.search(s -> s
        //         .index("hotel")
        //         .query(q -> q.match(m -> m.field("all").query("外滩如家"))), HotelDoc.class);

        SearchResponse<HotelDoc> response = client.search(s -> s
                .index("hotel")
                .query(q -> q.multiMatch(m -> m
                        .query("外滩如家")
                        .fields("name", "brand", "city"))), HotelDoc.class);

        handleResponse(response);
    }

    @Test
    void testBool() throws IOException {
        SearchResponse<HotelDoc> response = client.search(s -> s
                .index("hotel")
                .query(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("city").value("杭州")))
                        .filter(f -> f.range(r -> r.field("price").lte(JsonData.of(250)))))),
                HotelDoc.class);

        handleResponse(response);
    }

    @Test
    void testSortAndPage() throws IOException {
        int page = 2, size = 5;

        SearchResponse<HotelDoc> response = client.search(s -> s
                .index("hotel")
                .query(q -> q.matchAll(m -> m))
                .sort(so -> so.field(f -> f.field("price").order(SortOrder.Asc)))
                .from((page - 1) * size)
                .size(size), HotelDoc.class);

        handleResponse(response);
    }

    @Test
    void testHighlight() throws IOException {
        SearchResponse<HotelDoc> response = client.search(s -> s
                .index("hotel")
                .query(q -> q.match(m -> m.field("all").query("外滩如家")))
                .highlight(h -> h.fields("name", f -> f.requireFieldMatch(false))), HotelDoc.class);

        handleResponse(response);
    }

    private void handleResponse(SearchResponse<HotelDoc> response) {
        // 总条数
        long total = response.hits().total().value();
        System.out.println("总条数：" + total);
        // 遍历命中文档
        List<Hit<HotelDoc>> hits = response.hits().hits();
        for (Hit<HotelDoc> hit : hits) {
            HotelDoc hotelDoc = hit.source();
            if (hotelDoc == null) {
                continue;
            }
            // 高亮结果覆盖原字段
            List<String> highlightName = hit.highlight().get("name");
            if (highlightName != null && !highlightName.isEmpty()) {
                hotelDoc.setName(highlightName.get(0));
            }
            System.out.println(hotelDoc);
        }
    }
}
