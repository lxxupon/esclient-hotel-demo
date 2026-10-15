package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.json.JsonData;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    private static final String INDEX_NAME = "hotel";

    @Autowired
    private ElasticsearchClient client;

    @Override
    public PageResult search(RequestParams params) {
        try {
            int page = params.getPage();
            int size = params.getSize();

            SearchRequest.Builder builder = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .query(buildBasicQuery(params))
                    .from((page - 1) * size)
                    .size(size);

            // 距离排序
            String location = params.getLocation();
            if (StringUtils.isNotBlank(location)) {
                builder.sort(s -> s.geoDistance(g -> g
                        .field("location")
                        .location(l -> l.text(location))
                        .order(SortOrder.Asc)
                        .unit(DistanceUnit.Kilometers)
                ));
            }
            // 发送请求并解析响应
            SearchResponse<HotelDoc> response = client.search(builder.build(), HotelDoc.class);
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("搜索数据失败", e);
        }
    }

    @Override
    public Map<String, List<String>> getFilters(RequestParams params) {
        try {
            SearchRequest request = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .query(buildBasicQuery(params))
                    .size(0)
                    .aggregations("brandAgg", a -> a.terms(t -> t.field("brand").size(100)))
                    .aggregations("cityAgg", a -> a.terms(t -> t.field("city").size(100)))
                    .aggregations("starAgg", a -> a.terms(t -> t.field("starName").size(100)))
                    .build();

            SearchResponse<Void> response = client.search(request, Void.class);
            Map<String, Aggregate> aggregations = response.aggregations();

            Map<String, List<String>> filters = new HashMap<>(3);
            filters.put("brand", getAggregationByName(aggregations, "brandAgg"));
            filters.put("city", getAggregationByName(aggregations, "cityAgg"));
            filters.put("starName", getAggregationByName(aggregations, "starAgg"));

            return filters;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getSuggestion(String key) {
        try {
            SearchRequest request = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .suggest(s -> s.suggesters("hotelSuggest", f -> f
                            .prefix(key)
                            .completion(c -> c
                                    .field("suggestion")
                                    .size(10)
                                    .skipDuplicates(true))))
                    .build();

            SearchResponse<HotelDoc> response = client.search(request, HotelDoc.class);

            List<String> list = new ArrayList<>();
            List<Suggestion<HotelDoc>> suggestions = response.suggest().get("hotelSuggest");
            if (suggestions == null) {
                return list;
            }
            for (Suggestion<HotelDoc> suggestion : suggestions) {
                for (CompletionSuggestOption<HotelDoc> option : suggestion.completion().options()) {
                    list.add(option.text());
                }
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(Long hotelId) {
        try {
            client.delete(d -> d.index(INDEX_NAME).id(hotelId.toString()));
        } catch (IOException e) {
            throw new RuntimeException("删除酒店数据失败", e);
        }
    }

    @Override
    public void saveById(Long hotelId) {
        try {
            // 查询酒店数据，应该基于Feign远程调用hotel-admin，根据id查询酒店数据（现在直接去数据库查）
            Hotel hotel = getById(hotelId);
            HotelDoc hotelDoc = new HotelDoc(hotel);

            client.index(i -> i.index(INDEX_NAME).id(hotelId.toString()).document(hotelDoc));
        } catch (IOException e) {
            throw new RuntimeException("新增酒店数据失败", e);
        }
    }

    private List<String> getAggregationByName(Map<String, Aggregate> aggregations, String aggName) {
        List<StringTermsBucket> buckets = aggregations.get(aggName).sterms().buckets().array();
        List<String> list = new ArrayList<>(buckets.size());
        for (StringTermsBucket bucket : buckets) {
            list.add(bucket.key().stringValue());
        }
        return list;
    }

    private Query buildBasicQuery(RequestParams params) {
        // 1.准备Boolean查询
        Query boolQuery = Query.of(q -> q.bool(b -> {
            // 1.1.关键字搜索，match查询，放到must中
            String key = params.getKey();
            if (StringUtils.isNotBlank(key)) {
                b.must(m -> m.match(mm -> mm.field("all").query(key)));
            } else {
                b.must(m -> m.matchAll(ma -> ma));
            }
            // 1.2.品牌
            addTermFilter(b, "brand", params.getBrand());
            // 1.3.城市
            addTermFilter(b, "city", params.getCity());
            // 1.4.星级
            addTermFilter(b, "starName", params.getStarName());
            // 1.5.价格范围
            Integer minPrice = params.getMinPrice();
            Integer maxPrice = params.getMaxPrice();
            if (minPrice != null && maxPrice != null) {
                Integer upper = maxPrice == 0 ? Integer.MAX_VALUE : maxPrice;
                b.filter(f -> f.range(r -> r
                        .field("price")
                        .gte(JsonData.of(minPrice))
                        .lte(JsonData.of(upper))));
            }
            return b;
        }));

        // 2.算分函数查询：广告位加权
        return Query.of(q -> q.functionScore(f -> f
                .query(boolQuery)
                .functions(fn -> fn
                        .filter(ad -> ad.term(term -> term.field("isAD").value(true)))
                        .weight(10.0))));
    }

    private void addTermFilter(BoolQuery.Builder builder, String field, String value) {
        if (StringUtils.isNotBlank(value)) {
            builder.filter(f -> f.term(t -> t.field(field).value(FieldValue.of(value))));
        }
    }

    private PageResult handleResponse(SearchResponse<HotelDoc> response) {
        // 总条数
        long total = response.hits().total().value();
        List<Hit<HotelDoc>> hits = response.hits().hits();
        List<HotelDoc> hotels = new ArrayList<>(hits.size());
        for (Hit<HotelDoc> hit : hits) {
            HotelDoc hotelDoc = hit.source();
            if (hotelDoc == null) {
                continue;
            }
            // 处理高亮结果
            List<String> highlightedNames = hit.highlight().get("name");
            if (highlightedNames != null && !highlightedNames.isEmpty()) {
                hotelDoc.setName(highlightedNames.get(0));
            }
            // 排序信息
            List<FieldValue> sortValues = hit.sort();
            if (!sortValues.isEmpty()) {
                hotelDoc.setDistance(sortValues.get(0)._get());
            }

            hotels.add(hotelDoc);
        }
        return new PageResult(total, hotels);
    }
}
