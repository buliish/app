package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
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

    /** 来源方式：人工养殖/海洋捕捞（溯源链最源头的"养殖地 or 打捞地"） */
    private String sourceType;

    /** 产品形态：鲜虾/冻虾（养殖环节出塘即鲜虾） */
    private String productForm;

    /**
     * 本环节质量状态：0 待检 1 合格 2 不合格。
     * <p>
     * 命名不能叫 status —— {@link NodeOwned#getStatus()} 被
     * OwnedBatchService.requireOwned() 用作工作流状态白名单，
     * 而 Lombok 会为新字段生成 getStatus() 覆盖默认方法，导致状态校验静默失效。
     */
    private Integer qualityStatus;

    /**
     * 本批出场量（kg）。
     * <p>
     * 养殖是链头，没有"领用量"；这个数是下游分批领用的总额度 ——
     * 下游各批号的 up_quantity_kg 之和不得超过它。
     * 为 null 表示未登记数量，此时不做超领校验（兼容历史数据）。
     */
    private BigDecimal quantityKg;

    /** 动物检验检疫合格证 */
    private String quarantineNo;

    /** 官方检疫员名称 */
    private String inspector;

    /** 1 待发布 2 已发布 3 已下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
