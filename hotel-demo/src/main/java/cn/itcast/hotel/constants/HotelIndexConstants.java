package cn.itcast.hotel.constants;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.JsonData;

import java.io.StringReader;

/**
 * 酒店索引定义：分词配置（settings.analysis）+ 字段映射（mappings）。
 *
 * pinyin 是 analysis-pinyin 插件提供的过滤器类型，客户端的 TokenFilterDefinition
 * 只为内置过滤器建模，插件类型必须走 _custom(...)，且参数得是 JsonData
 * （传 Map 会被序列化成字符串，ES 解析不到 type）。
 */
public class HotelIndexConstants {

    public static final String INDEX_NAME = "hotel";

    private static final String PINYIN_FILTER_JSON = "{"
            + "\"type\": \"pinyin\","
            + "\"keep_full_pinyin\": false,"
            + "\"keep_joined_full_pinyin\": true,"
            + "\"keep_original\": true,"
            + "\"limit_first_letter_length\": 16,"
            + "\"remove_duplicated_term\": true,"
            + "\"none_chinese_pinyin_tokenize\": false"
            + "}";

    private static final JsonData PINYIN_FILTER = JsonData.from(new StringReader(PINYIN_FILTER_JSON));

    public static final IndexSettings HOTEL_SETTINGS = IndexSettings.of(s -> s
            .analysis(a -> a
                    .filter("py", f -> f.definition(d -> d._custom("pinyin", PINYIN_FILTER)))
                    .analyzer("text_anlyzer", an -> an.custom(c -> c
                            .tokenizer("ik_max_word")
                            .filter("py")))
                    .analyzer("completion_analyzer", an -> an.custom(c -> c
                            .tokenizer("keyword")
                            .filter("py")))));

    public static final TypeMapping HOTEL_MAPPINGS = TypeMapping.of(m -> m
            .properties("id", p -> p.keyword(k -> k))
            .properties("name", p -> p.text(t -> t
                    .analyzer("text_anlyzer")
                    .searchAnalyzer("ik_smart")
                    .copyTo("all")))
            .properties("address", p -> p.keyword(k -> k.index(false)))
            .properties("price", p -> p.integer(i -> i))
            .properties("score", p -> p.integer(i -> i))
            .properties("brand", p -> p.keyword(k -> k.copyTo("all")))
            .properties("city", p -> p.keyword(k -> k))
            .properties("starName", p -> p.keyword(k -> k))
            .properties("business", p -> p.keyword(k -> k.copyTo("all")))
            .properties("pic", p -> p.keyword(k -> k.index(false)))
            .properties("location", p -> p.geoPoint(g -> g))
            .properties("all", p -> p.text(t -> t
                    .analyzer("text_anlyzer")
                    .searchAnalyzer("ik_smart")))
            .properties("suggestion", p -> p.completion(c -> c.analyzer("completion_analyzer")))
    );
}
