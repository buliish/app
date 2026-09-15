package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.TraceService;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 消费者端控制器（3.2.7）：输入溯源标识码查询全链路溯源信息，无需登录
 * <p>
 * 类上的 @Validated 不能省，否则 @PathVariable 上的约束注解不会生效
 */
@RestController
@RequestMapping("/trace")
@Validated
public class TraceController {

    @Autowired
    private TraceService traceService;

    /** 溯源标识码只允许字母数字，长度 6~50，避免超长串直接打到数据库 */
    @GetMapping("/{traceCode}")
    public Result<Map<String, Object>> trace(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9]{6,50}$", message = "溯源标识码格式不正确")
            String traceCode) {
        Map<String, Object> data = traceService.trace(traceCode.trim());
        if (data == null) {
            return Result.error("溯源标识码不存在，请核对后重新输入");
        }
        return Result.success(data);
    }
}
