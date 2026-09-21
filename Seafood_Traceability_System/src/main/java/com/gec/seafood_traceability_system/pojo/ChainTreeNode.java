package com.gec.seafood_traceability_system.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 产业链树的一个节点（同时是它下面那棵子树的根）。
 * <p>
 * 产业链不是一条线而是一棵树：一批虾可以同时被加工成虾滑、虾丸、冷冻整虾，
 * 每条分支各自往下流到不同的批发商和零售商。因此这里用「节点 + 子树」
 * 来表达，前端递归渲染即可。
 * <p>
 * 批号之间的父子关系完全由现存字段表达（下游批号的 {@code up_batch_no} +
 * {@code up_node_id} 指向上游批号），不额外建关系表。
 */
@Data
public class ChainTreeNode {

    /** 环节类型：1 养殖 2 冷冻加工 3 批发 4 零售（与 node_info.type 同码） */
    private Integer stageType;

    /** 环节名称 */
    private String stage;

    /** 本环节批号主键（指向四张批号表之一） */
    private Integer batchId;

    /** 产品批号 */
    private String batchNo;

    /**
     * 所属企业。
     * <p>
     * NodeInfo.password 标了 {@code @JsonProperty(WRITE_ONLY)}，序列化时不会外泄。
     */
    private NodeInfo node;

    private String breed;

    /** 产品类型 / 养殖环节则为产品形态 */
    private String productType;

    /** 批号状态：各环节取值语义不同（养殖 1待发布/2已发布/3已下架，其余 1新建/2待确认/3已确认/4已下架） */
    private Integer status;

    /** 该环节的质量状态：0 待检 1 合格 2 不合格 */
    private Integer qualityStatus;

    private LocalDateTime createTime;

    /**
     * 本节点是否位于「当前正在查看的那条链」上。
     * <p>
     * 前端据此把用户当前所在的分支高亮；不在这条链上的零售叶子，
     * 就是"同源产品"（同一批虾做出来的别的东西）。
     */
    private boolean onCurrentChain;

    /** 下游分支。同一批号可以派生多个下游，故为列表 */
    private List<ChainTreeNode> children = new ArrayList<>();
}
