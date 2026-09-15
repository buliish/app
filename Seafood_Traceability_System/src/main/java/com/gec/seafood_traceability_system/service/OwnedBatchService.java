package com.gec.seafood_traceability_system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.NodeOwned;
import com.gec.seafood_traceability_system.pojo.Result;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 带归属校验的业务接口
 * <p>
 * 解决"只凭主键就能改/删别人的数据"这个问题：
 * 所有按 id 操作的端点，都必须先经过 {@link #requireOwned} 确认数据属于当前登录企业。
 * <p>
 * 用法（各批号 Service 接口继承即可，无需实现）：
 * <pre>
 * public interface FarmBatchService extends OwnedBatchService&lt;FarmBatch&gt; { ... }
 * </pre>
 */
public interface OwnedBatchService<T extends NodeOwned> extends IService<T> {

    /**
     * 取出数据并校验归属，不满足直接抛业务异常。
     *
     * @param id            数据主键
     * @param currentNodeId 当前登录企业编号（来自 token）
     * @return 命中的数据
     * @throws BizException 数据不存在(code=1) / 不属于当前企业(code=403)
     */
    default T requireOwned(Serializable id, Integer currentNodeId) {
        if (id == null) {
            throw new BizException("缺少数据主键");
        }
        T entity = getById(id);
        if (entity == null) {
            throw new BizException("数据不存在或已被删除");
        }
        if (currentNodeId == null || !currentNodeId.equals(entity.getNodeId())) {
            throw new BizException(Result.CODE_FORBIDDEN, "无权操作其他企业的数据");
        }
        return entity;
    }

    /**
     * 取出数据、校验归属，并限定允许的状态。
     * <p>
     * 用于 update / delete / offline 这类有状态前置条件的操作，
     * 例如"只有待发布状态才能删除"。
     *
     * @param allowedStatus 允许的状态值，为空则不限制
     */
    default T requireOwned(Serializable id, Integer currentNodeId, Integer... allowedStatus) {
        T entity = requireOwned(id, currentNodeId);
        if (allowedStatus != null && allowedStatus.length > 0) {
            Integer status = entity.getStatus();
            if (status == null || Arrays.stream(allowedStatus).noneMatch(s -> s.equals(status))) {
                throw new BizException(describeStatusError(entity, allowedStatus));
            }
        }
        return entity;
    }

    /** 状态不满足时给用户看的提示 */
    default String describeStatusError(T entity, Integer... allowedStatus) {
        StringBuilder sb = new StringBuilder("当前状态（").append(entity.getStatus()).append("）不允许该操作");
        if (allowedStatus != null && allowedStatus.length > 0) {
            sb.append("，仅限状态 ");
            for (int i = 0; i < allowedStatus.length; i++) {
                if (i > 0) {
                    sb.append(" / ");
                }
                sb.append(allowedStatus[i]);
            }
        }
        return sb.toString();
    }
}
