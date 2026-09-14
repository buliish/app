package com.gec.seafood_traceability_system.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下游企业进场确认列表项
 * 养殖企业确认冷冻加工企业、冷冻加工企业确认批发商、批发商确认零售商，展示字段一致
 */
@Data
public class ConfirmVO {

    /** 下游批号主键（确认操作使用） */
    private Integer id;

    /** 下游企业节点ID */
    private Integer downNodeId;

    /** 下游企业名称 */
    private String downName;

    /** 下游企业产品批号 */
    private String downBatchNo;

    /** 进场批号（本企业产品批号） */
    private String upBatchNo;

    /** 品种 */
    private String breed;

    /** 产品类型 */
    private String productType;

    /** 待确认状态值 */
    private Integer status;

    /** 进场（建号）时间 */
    private LocalDateTime createTime;
}
