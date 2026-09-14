package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 市行政区域信息表 */
@Data
@TableName("city")
public class City {

    @TableId(value = "city_id", type = IdType.AUTO)
    private Integer cityId;

    private String cityName;

    private Integer provId;

    private String remarks;
}
