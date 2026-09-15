package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 冷冻加工企业产品批号（状态：1 新建 2 待确认 3 已确认 4 已下架）
 */
@Data
@TableName("froz_batch")
public class FrozBatch implements NodeOwned {

    @TableId(value = "froz_batch_id", type = IdType.AUTO)
    private Integer frozBatchId;

    /** 冷冻加工企业节点ID */
    private Integer nodeId;

    private String batchNo;

    /** 上游养殖企业所在省 */
    private Integer provId;

    /** 上游养殖企业所在市 */
    private Integer cityId;

    /** 上游养殖企业ID */
    private Integer upNodeId;

    /** 上游养殖企业产品批号 */
    private String upBatchNo;

    private String breed;

    /** 产品检验检疫合格证 */
    private String quarantineNo;

    /** 官方检验员名称 */
    private String inspector;

    /** 产品类型 */
    private String productType;

    /** 产品形态：鲜虾/冻虾（加工环节定型，下游继承） */
    private String productForm;

    /** 规格等级，如 40-50只/斤（加工环节定型，下游继承） */
    private String specGrade;

    /**
     * 本环节质量状态：0 待检 1 合格 2 不合格。
     * <p>
     * 命名不能叫 status —— 见 {@code FarmBatch#qualityStatus} 的说明。
     */
    private Integer qualityStatus;

    /** 对外产品编号：加工批号被上游确认时确定性生成，下游继承 */
    private String productCode;

    /** 1 新建 2 待确认 3 已确认 4 已下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
