package com.gec.seafood_traceability_system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.seafood_traceability_system.mapper.ProvinceMapper;
import com.gec.seafood_traceability_system.pojo.Province;
import com.gec.seafood_traceability_system.service.ProvinceService;
import org.springframework.stereotype.Service;

/** 省行政区域业务实现 */
@Service
public class ProvinceServiceImpl extends ServiceImpl<ProvinceMapper, Province> implements ProvinceService {
}
