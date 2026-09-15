package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批发商产品批号（状态：1 新建 2 待确认 3 已确认 4 已下架）
 */
@Data
@TableName("whol_batch")
public class WholBatch implements NodeOwned {

    @TableId(value = "whol_batch_id", type = IdType.AUTO)
    private Integer wholBatchId;

    /** 批发商节点ID */
    private Integer nodeId;

    private String batchNo;

    /** 上游冷冻加工企业所在省 */
    private Integer provId;

    /** 上游冷冻加工企业所在市 */
    private Integer cityId;

    /** 上游冷冻加工企业ID */
    private Integer upNodeId;

    /** 上游产品批号 */
    private String upBatchNo;

    private String breed;

    private String productType;

    /** 1 新建 2 待确认 3 已确认 4 已下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
