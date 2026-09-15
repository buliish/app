package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 冷冻加工工序记录（清洗 / 分级 / 冷冻 / 包装）
 */
@Data
@TableName("process_record")
public class ProcessRecord implements NodeOwned {

    @TableId(value = "record_id", type = IdType.AUTO)
    private Integer recordId;

    private Integer frozBatchId;

    /** 加工企业节点ID */
    private Integer nodeId;

    /** 工序：清洗/分级/冷冻/包装 */
    private String step;

    private LocalDateTime stepTime;

    /** 工艺参数（温度、规格等） */
    private String temperature;

    private String operator;

    private String remark;

    /**
     * 工序记录没有批号字段，但它实现了 {@link NodeOwned}，
     * 而该接口为支持检测记录写入新增了 getBatchNo() 默认方法 ——
     * 不显式忽略的话，Jackson 会给每条工序都输出一个无意义的 "batchNo": null。
     */
    @JsonIgnore
    @Override
    public String getBatchNo() {
        return null;
    }
}
