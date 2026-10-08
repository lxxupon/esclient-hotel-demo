package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;

public interface IHotelService extends IService<Hotel> {

    /**
     * 单条酒店数据同步到 ES
     */
    void saveHotelToEs(Long id) throws IOException;

    /**
     * 全量酒店数据同步到 ES（批量）
     */
    void saveAllToEs() throws IOException;
}
