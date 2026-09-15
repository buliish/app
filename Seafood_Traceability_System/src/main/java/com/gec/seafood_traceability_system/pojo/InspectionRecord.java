package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 检测记录（四个环节共用一张表）。
 * <p>
 * 用 {@code stageType} 区分记录挂在哪一类批号上，避免为四类批号
 * 各建一张结构完全相同的检测表；{@code batchId} 指向对应环节批号表的主键。
 * <p>
 * 实现 {@link NodeOwned} 以复用 {@code OwnedBatchService.requireOwned()}：
 * 检测记录的录入与删除同样要限制在"本企业自己的批号"上。
 */
@Data
@TableName("inspection_record")
public class InspectionRecord implements NodeOwned {

    /** 单项判定 / 整批结论：合格 */
    public static final int RESULT_PASSED = 1;
    /** 单项判定 / 整批结论：不合格 */
    public static final int RESULT_REJECTED = 2;

    /**
     * 主键。
     * <p>
     * 列名是 {@code record_id}，但对外（前端契约）叫 {@code inspectionId}——
     * 前端 BatchDetailView 删除检测记录时读的就是 {@code rec.inspectionId}。
     * 这里用 @TableId 把两者接起来，不额外产生一个重复字段。
     */
    @TableId(value = "record_id", type = IdType.AUTO)
    private Integer inspectionId;

    /** 环节类型：1 养殖 2 冷冻加工 3 批发 4 零售 */
    private Integer stageType;

    /** 所属环节批号主键 */
    private Integer batchId;

    /** 录入企业节点ID */
    private Integer nodeId;

    /** 检测项目名称 */
    private String itemName;

    /** 检测值，如 0.62 mg/kg */
    private String itemValue;

    /** 标准限值，如 ≤0.5 mg/kg */
    private String standardValue;

    /** 单项判定：1 合格 2 不合格 */
    private Integer result;

    /** 整批结论：1 合格 2 不合格 */
    private Integer conclusion;

    /** 检测报告编号 */
    private String reportNo;

    /** 检测机构名称 */
    private String orgName;

    /** 检测人姓名 */
    private String inspector;

    /** 检测日期 */
    private LocalDate inspectDate;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 检测记录没有批号字段。
     * <p>
     * 不显式忽略的话，Jackson 会给每条记录都输出一个无意义的 {@code "batchNo": null}
     * （{@link NodeOwned} 为支持本接口而定义了该默认方法）。
     */
    @JsonIgnore
    @Override
    public String getBatchNo() {
        return null;
    }

    /**
     * 检测记录没有批号那种"流转状态"，
     * 返回 null 表示不参与 {@code requireOwned} 的状态校验。
     */
    @JsonIgnore
    @Override
    public Integer getStatus() {
        return null;
    }
}
