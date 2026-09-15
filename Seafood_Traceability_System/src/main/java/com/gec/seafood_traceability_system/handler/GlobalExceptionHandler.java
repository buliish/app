package com.gec.seafood_traceability_system.handler;

import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理
 * <p>
 * 统一把各类异常转换成 Result 结构返回，保证前端拿到的永远是
 * {@code {code, message, data}}，而不是 Spring 默认的 HTML 错误页。
 * <p>
 * 注意：鉴权失败（401）不在这里处理 —— 那个在 LoginInterceptor 里直接写响应，
 * 因为它发生在进入 Controller 之前。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：由业务代码主动抛出，message 直接展示给用户
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        // 无权限类异常属正常业务分支，不打印堆栈，避免日志噪音
        if (e.getCode() != Result.CODE_FORBIDDEN && e.getCode() != Result.CODE_UNAUTHORIZED) {
            log.warn("业务异常：{}", e.getMessage());
        }
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * @Validated 校验失败（@RequestBody 上的 @Valid 触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null
                ? "请求参数不合法"
                : fieldError.getField() + ": " + fieldError.getDefaultMessage();
        return Result.error(msg);
    }

    /**
     * 表单绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null
                ? "请求参数不合法"
                : fieldError.getField() + ": " + fieldError.getDefaultMessage();
        return Result.error(msg);
    }

    /**
     * 方法参数上的约束校验失败（如 @PathVariable 上的 @Pattern，需类上加 @Validated）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("请求参数不合法");
        return Result.error(msg);
    }

    /**
     * 请求体 JSON 格式错误 / 缺失
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.error("请求体格式不正确，请检查提交的数据");
    }

    /**
     * 唯一键冲突：批号重复、溯源码重复等
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突：{}", e.getMessage());
        return Result.error("数据已存在（批号或溯源标识码重复），请刷新后重试");
    }

    /**
     * 兜底：任何未被上面捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error("服务器繁忙，请稍后重试");
    }
}
