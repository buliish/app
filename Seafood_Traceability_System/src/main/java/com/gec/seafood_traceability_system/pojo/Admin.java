package com.gec.seafood_traceability_system.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** 系统管理员表 */
@Data
@TableName("admin")
public class Admin {

    @TableId(value = "admin_id", type = IdType.AUTO)
    private Integer adminId;

    private String adminName;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}
