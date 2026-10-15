package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest
class HotelAggregationTest {

    @Autowired
    private ElasticsearchClient client;

    @Test
    void testAgg() throws IOException {
        SearchResponse<Void> response = client.search(s -> s
                .index("hotel")
                .size(0)
                .aggregations("brandAgg", a -> a.terms(t -> t.field("brand").size(20))), Void.class);

        Map<String, Aggregate> aggregations = response.aggregations();
        List<StringTermsBucket> buckets = aggregations.get("brandAgg").sterms().buckets().array();
        for (StringTermsBucket bucket : buckets) {
            System.out.println("brandName = " + bucket.key().stringValue());
            System.out.println("docCount = " + bucket.docCount());
        }
    }

    @Test
    void testSuggest() throws IOException {
        SearchResponse<HotelDoc> response = client.search(s -> s
                .index("hotel")
                .suggest(sg -> sg.suggesters("hotelSuggest", f -> f
                        .prefix("s")
                        .completion(c -> c
                                .field("suggestion")
                                .size(10)
                                .skipDuplicates(true)))), HotelDoc.class);

        for (Suggestion<HotelDoc> suggestion : response.suggest().get("hotelSuggest")) {
            for (CompletionSuggestOption<HotelDoc> option : suggestion.completion().options()) {
                System.out.println(option.text());
            }
        }
    }
}
