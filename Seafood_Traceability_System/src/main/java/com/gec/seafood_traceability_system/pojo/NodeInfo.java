package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 节点企业信息表（表列 type 对应实体 nodeType）
 * type：1 养殖企业 2 冷冻加工企业 3 批发商 4 零售商
 */
@Data
@TableName("node_info")
public class NodeInfo {

    @TableId(value = "node_id", type = IdType.AUTO)
    private Integer nodeId;

    /** 登录编码 */
    private String code;

    /** 登录密码：只允许写入，接口返回时不输出 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 企业名称 */
    private String name;

    /** 企业类型：1 养殖企业 2 冷冻加工企业 3 批发商 4 零售商 */
    @TableField("type")
    private Integer nodeType;

    private Integer provId;

    private Integer cityId;

    private String address;

    private String businessId;

    private String epId;

    private String eiaId;

    private String cirId;

    private String fbId;

    private String corporation;

    private String telephone;

    private LocalDate regDate;

    private String remarks;
}
