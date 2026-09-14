package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.CityMapper;
import com.gec.seafood_traceability_system.pojo.City;
import com.gec.seafood_traceability_system.service.CityService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 市行政区域业务实现 */
@Service
public class CityServiceImpl extends ServiceImpl<CityMapper, City> implements CityService {

    @Override
    public List<City> listByProvId(Integer provId) {
        return lambdaQuery()
                .eq(provId != null, City::getProvId, provId)
                .orderByAsc(City::getCityId)
                .list();
    }
}
