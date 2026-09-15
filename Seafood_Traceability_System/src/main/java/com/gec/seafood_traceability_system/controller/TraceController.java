package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.TraceService;
import com.gec.seafood_traceability_system.utils.QrCodeUtil;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** 扫码后打开的页面地址，形如 http://localhost:5173/trace?code= */
    @Value("${trace.qrcode.base-url}")
    private String qrBaseUrl;

    /** 二维码图片边长 */
    @Value("${trace.qrcode.size:300}")
    private int qrSize;

    /**
     * 在售商品列表（消费者端首页"售卖的产品"）。
     * <p>
     * 免登录；/trace/** 整体在 LoginInterceptor 的放行名单里。
     */
    @GetMapping("/products")
    public Result<Map<String, Object>> products(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "12") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String form,
            @RequestParam(required = false) Integer provId) {
        // 关键词长度设限，避免超长串直接打到数据库（与溯源标识码同样的防护思路）
        if (keyword != null && keyword.length() > 50) {
            throw new BizException("查询关键词过长");
        }
        return Result.success(traceService.listProducts(current, size, keyword, form, provId));
    }

    /**
     * 商品详情：产品信息 + 四级企业链 + 各环节检测记录 + 加工工序。
     * <p>
     * 入口支持溯源码或对外产品编号。
     */
    @GetMapping("/product/{code}")
    public Result<Map<String, Object>> product(@PathVariable String code) {
        Map<String, Object> data = traceService.productDetailByTraceCode(code.trim());
        if (data == null) {
            return Result.error("未找到该产品，请核对编码后重试");
        }
        return Result.success(data);
    }

    /**
     * 生成溯源二维码（PNG 图片流）。
     * <p>
     * 供页面 &lt;img src&gt; 直接引用与下载；/trace/** 免登录，消费者扫码即可查看。
     */
    @GetMapping(value = "/qrcode/{traceCode}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qrcode(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9]{6,50}$", message = "溯源标识码格式不正确")
            String traceCode) throws Exception {
        // 先确认该溯源标识码真实存在，避免为任意字符串生成二维码
        if (traceService.trace(traceCode) == null) {
            throw new BizException("溯源标识码不存在");
        }
        return QrCodeUtil.toPng(qrBaseUrl + traceCode, qrSize);
    }

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
