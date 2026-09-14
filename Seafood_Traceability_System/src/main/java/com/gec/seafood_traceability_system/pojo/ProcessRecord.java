package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 冷冻加工工序记录（清洗 / 分级 / 冷冻 / 包装）
 */
@Data
@TableName("process_record")
public class ProcessRecord {

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
}
