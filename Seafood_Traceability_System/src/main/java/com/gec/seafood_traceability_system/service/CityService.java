package com.gec.seafood_traceability_system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.seafood_traceability_system.pojo.City;

import java.util.List;

/** 市行政区域业务接口 */
public interface CityService extends IService<City> {

    /** 查询指定省下的市列表 */
    List<City> listByProvId(Integer provId);
}
