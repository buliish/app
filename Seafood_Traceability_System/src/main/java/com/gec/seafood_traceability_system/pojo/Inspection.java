package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 各环节检测记录（一条记录 = 一份检测报告的一个检测项）
 * <p>
 * 归属四个环节的批号，用 {@code stageType + batchId} 多态关联 —— 不能用
 * {@code nodeId + batchNo} 关联，因为 batch_no 只在单表内唯一、跨表可以重号，
 * 用它会在四张批号表之间串表。nodeId / batchNo 作为冗余列保留，
 * 分别服务于归属校验（配合 {@link NodeOwned}）与列表展示检索。
 * <p>
 * 同一个报告号的多个检测项聚成一组；整批是否合格同时冗余在批号表的
 * qualityStatus 上（列表看字段、明细看本表），一致性由 Service 保证。
 * <p>
 * 注意：本类<b>刻意没有 status 字段</b>，因此 {@link NodeOwned#getStatus()}
 * 返回 null，归属校验必须用不带状态白名单的 requireOwned 重载。
 */
@Data
@TableName("inspection")
public class Inspection implements NodeOwned {

    @TableId(value = "inspection_id", type = IdType.AUTO)
    private Integer inspectionId;

    /** 检测所属环节企业ID（归属校验用） */
    private Integer nodeId;

    /** 环节类型：1养殖 2冷冻加工 3批发 4零售（同 NodeInfo.nodeType） */
    private Integer stageType;

    /** 所属批号主键（四张批号表之一，由 stageType 决定指向哪张表） */
    private Integer batchId;

    /** 所属批号（冗余，便于列表展示与检索） */
    private String batchNo;

    /** 检测项目：感官/菌落总数/大肠菌群/氯霉素/重金属镉/水分等 */
    private String itemName;

    /** 检测值 */
    private String itemValue;

    /** 标准限值 */
    private String standardValue;

    /** 单项判定：1 合格 2 不合格 */
    private Integer result;

    /** 本报告整批结论：1 合格 2 不合格（一条批号可有多份报告） */
    private Integer conclusion;

    /** 检测报告编号（同一份报告的多个检测项共用） */
    private String reportNo;

    /** 检测机构名称 */
    private String orgName;

    /** 检测人/检验员 */
    private String inspector;

    /** 检测日期 */
    private LocalDate inspectDate;

    private String remark;

    private LocalDateTime createTime;
}
