package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.config.EsProperties;
import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 客户端由容器注入，全程复用同一个连接池，不手动 close
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    private final RestHighLevelClient client;

    private final EsProperties esProperties;

    @Override
    public void saveHotelToEs(Long id) throws IOException {
        Hotel hotel = getById(id);
        HotelDoc hotelDoc = new HotelDoc(hotel);
        String json = JSON.toJSONString(hotelDoc);

        IndexRequest request = new IndexRequest("hotel").id(hotelDoc.getId().toString());
        request.source(json, XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    @Override
    public void saveAllToEs() throws IOException {
        List<Hotel> list = list();
        if (list.isEmpty()) {
            return;
        }

        BulkRequest request = new BulkRequest();
        for (Hotel hotel : list) {
            HotelDoc hotelDoc = new HotelDoc(hotel);
            request.add(new IndexRequest("hotel")
                    .id(hotel.getId().toString())
                    .source(JSON.toJSONString(hotelDoc), XContentType.JSON));
        }
        client.bulk(request, RequestOptions.DEFAULT);
        log.info("bulk 写入 {} 条酒店数据", list.size());
    }

    public String esHost() {
        return String.join(",", esProperties.getHosts());
    }
}
