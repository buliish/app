package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.AdminTraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端批次追溯查询（3.2.8 扩展）
 * <p>
 * 挂在 /admin/** 下，由 AdminAuthInterceptor 自动鉴权（只放行 /admin/login）。
 * <b>不能</b>放到 /trace/** —— 那个前缀在 WebMvcConfig 里是公开的，
 * 会把全链路企业信息与检测报告泄露给未登录用户。
 */
@RestController
@RequestMapping("/admin/trace")
public class AdminTraceController {

    @Autowired
    private AdminTraceService adminTraceService;

    /**
     * 按关键词搜索候选批次。
     * 支持产品编号 / 溯源码 / 零售批号 / 批发批号 / 加工批号 / 养殖批号。
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam(required = false) String keyword) {
        return Result.success(adminTraceService.search(keyword));
    }

    /** 按关键词取完整链路（命中多条时返回提示，让前端先选候选） */
    @GetMapping("/chain")
    public Result<Map<String, Object>> chain(@RequestParam String keyword) {
        Map<String, Object> data = adminTraceService.chainByKeyword(keyword);
        if (data == null) {
            throw new BizException("未找到唯一匹配的批次，请从上方的搜索结果中选择");
        }
        return Result.success(data);
    }

    /** 按零售批号主键取完整链路（候选点选后用，避免二次解析） */
    @GetMapping("/chain/{retaBatchId}")
    public Result<Map<String, Object>> chainById(@PathVariable Integer retaBatchId) {
        Map<String, Object> data = adminTraceService.chainByRetaBatchId(retaBatchId);
        if (data == null) {
            throw new BizException("批次不存在或已被删除");
        }
        return Result.success(data);
    }

    /** 各环节合格率与在售产品形态分布 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(adminTraceService.stats());
    }
}
