package com.gec.seafood_traceability_system.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 * <p>
 * code 取值：0 成功；1 普通业务失败；401 未登录/登录过期；403 无权限
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 成功 */
    public static final int CODE_SUCCESS = 0;
    /** 普通业务失败 */
    public static final int CODE_FAIL = 1;
    /** 未登录 / 登录已过期 */
    public static final int CODE_UNAUTHORIZED = 401;
    /** 已登录但无权操作 */
    public static final int CODE_FORBIDDEN = 403;

    private Integer code;//业务状态码  0-成功  1-失败
    private String message;//提示信息
    private T data;//响应数据

    public static <E> Result<E> success(E data) {
        return new Result<>(CODE_SUCCESS, "操作成功", data);
    }

    public static Result success() {
        return new Result(CODE_SUCCESS, "操作成功", null);
    }

    public static Result error(String message) {
        return new Result(CODE_FAIL, message, null);
    }

    /**
     * 指定业务状态码的失败结果（401/403 等）
     */
    public static <E> Result<E> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
