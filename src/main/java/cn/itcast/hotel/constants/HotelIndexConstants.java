package cn.itcast.hotel.constants;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;

/**
 * 酒店索引相关常量。mapping 用 Java API Client 的类型安全 DSL 描述，
 * 替代原来的 JSON 字符串模板。
 */
public class HotelIndexConstants {

    public static final String INDEX_NAME = "hotel";

    public static final TypeMapping HOTEL_MAPPINGS = TypeMapping.of(m -> m
            .properties("id", p -> p.keyword(k -> k))
            .properties("name", p -> p.text(t -> t.analyzer("ik_max_word").copyTo("all")))
            .properties("address", p -> p.keyword(k -> k.index(false)))
            .properties("price", p -> p.integer(i -> i))
            .properties("score", p -> p.integer(i -> i))
            .properties("brand", p -> p.keyword(k -> k.copyTo("all")))
            .properties("city", p -> p.keyword(k -> k))
            .properties("starName", p -> p.keyword(k -> k))
            .properties("business", p -> p.keyword(k -> k.copyTo("all")))
            .properties("pic", p -> p.keyword(k -> k.index(false)))
            .properties("location", p -> p.geoPoint(g -> g))
            .properties("all", p -> p.text(t -> t.analyzer("ik_max_word")))
    );
}
