package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 省行政区域信息表 */
@Data
@TableName("province")
public class Province {

    @TableId(value = "prov_id", type = IdType.AUTO)
    private Integer provId;

    private String provName;

    private String remarks;
}
