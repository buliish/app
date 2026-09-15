package com.gec.seafood_traceability_system.pojo;

/**
 * 业务异常
 * <p>
 * 业务代码中主动抛出，由 GlobalExceptionHandler 统一转换成 Result 返回，
 * 避免在 Controller 里到处写 {@code if (...) return Result.error(...)}。
 * <p>
 * code 取值约定：
 * <ul>
 *   <li>1   —— 普通业务失败（默认）</li>
 *   <li>401 —— 未登录 / 登录过期</li>
 *   <li>403 —— 已登录但无权操作</li>
 * </ul>
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(Result.CODE_FAIL, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /** 业务异常不需要堆栈，省去 fillInStackTrace 的开销 */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
