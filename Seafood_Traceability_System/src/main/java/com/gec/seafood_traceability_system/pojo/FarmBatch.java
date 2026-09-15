package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 养殖企业产品批号（状态：1 待发布 2 已发布 3 已下架）
 */
@Data
@TableName("farm_batch")
public class FarmBatch implements NodeOwned {

    @TableId(value = "farm_batch_id", type = IdType.AUTO)
    private Integer farmBatchId;

    /** 养殖企业节点ID */
    private Integer nodeId;

    /** 产品批号 */
    private String batchNo;

    /** 产品品种 */
    private String breed;

    /** 养殖阶段：虾苗/成虾 */
    private String breedStage;

    /** 动物检验检疫合格证 */
    private String quarantineNo;

    /** 官方检疫员名称 */
    private String inspector;

    /** 1 待发布 2 已发布 3 已下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
