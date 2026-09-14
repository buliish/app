package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 零售商产品批号（状态：1 新建 2 待确认 3 已确认 4 已下架）
 * 状态更新为已确认时，系统自动生成溯源标识码 trace_code
 */
@Data
@TableName("reta_batch")
public class RetaBatch {

    @TableId(value = "reta_batch_id", type = IdType.AUTO)
    private Integer retaBatchId;

    /** 零售商节点ID */
    private Integer nodeId;

    private String batchNo;

    /** 上游批发商所在省 */
    private Integer provId;

    /** 上游批发商所在市 */
    private Integer cityId;

    /** 上游批发商ID */
    private Integer upNodeId;

    /** 上游产品批号 */
    private String upBatchNo;

    private String breed;

    private String productType;

    /** 1 新建 2 待确认 3 已确认 4 已下架 */
    private Integer status;

    /** 溯源标识码 */
    private String traceCode;

    private LocalDateTime traceTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
