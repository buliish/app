package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.TraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 消费者端控制器（3.2.7）：输入溯源标识码查询全链路溯源信息，无需登录
 */
@RestController
@RequestMapping("/trace")
public class TraceController {

    @Autowired
    private TraceService traceService;

    @GetMapping("/{traceCode}")
    public Result<Map<String, Object>> trace(@PathVariable String traceCode) {
        Map<String, Object> data = traceService.trace(traceCode.trim());
        if (data == null) {
            return Result.error("溯源标识码不存在，请核对后重新输入");
        }
        return Result.success(data);
    }
}
