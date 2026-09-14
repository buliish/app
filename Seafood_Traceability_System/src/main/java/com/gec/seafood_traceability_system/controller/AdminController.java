package com.gec.seafood_traceability_system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.seafood_traceability_system.pojo.Admin;
import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.pojo.Province;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.AdminService;
import com.gec.seafood_traceability_system.service.NodeInfoService;
import com.gec.seafood_traceability_system.service.ProvinceService;
import com.gec.seafood_traceability_system.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统管理端控制器（3.2.8）
 * 节点企业注册信息管理（增删改查 + 模糊查询 + 分页）与注册信息统计
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private NodeInfoService nodeInfoService;

    @Autowired
    private ProvinceService provinceService;

    // ---------------- 管理员登录 ----------------

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        Admin admin = adminService.findByAdminName(params.get("adminName"));
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        if (!admin.getPassword().equals(params.get("password"))) {
            return Result.error("密码错误");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "admin");
        claims.put("id", admin.getAdminId());
        claims.put("username", admin.getAdminName());
        return Result.success(JwtUtil.genToken(claims));
    }

    // ---------------- 节点企业注册信息管理 ----------------

    /** 分页 + 多条件模糊查询 */
    @GetMapping("/node/page")
    public Result<Page<NodeInfo>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String code,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) Integer type,
                                       @RequestParam(required = false) Integer provId,
                                       @RequestParam(required = false) Integer cityId) {
        return Result.success(nodeInfoService.pageQuery(current, size, code, name, type, provId, cityId));
    }

    /** 节点企业详情 */
    @GetMapping("/node/{id}")
    public Result<NodeInfo> detail(@PathVariable Integer id) {
        return Result.success(nodeInfoService.getById(id));
    }

    /** 新建节点企业（注册） */
    @PostMapping("/node")
    public Result save(@RequestBody NodeInfo node) {
        if (nodeInfoService.findByCode(node.getCode()) != null) {
            return Result.error("登录编码已存在");
        }
        node.setNodeId(null);
        if (node.getRegDate() == null) {
            node.setRegDate(LocalDate.now());
        }
        nodeInfoService.save(node);
        return Result.success();
    }

    /** 编辑节点企业（密码由节点端企业自行维护，不在此修改） */
    @PutMapping("/node")
    public Result update(@RequestBody NodeInfo node) {
        node.setPassword(null);
        nodeInfoService.updateById(node);
        return Result.success();
    }

    /** 删除节点企业 */
    @DeleteMapping("/node/{id}")
    public Result delete(@PathVariable Integer id) {
        nodeInfoService.removeById(id);
        return Result.success();
    }

    // ---------------- 注册信息统计（可视化大屏） ----------------

    /**
     * 统计口径：
     * typeDist  企业类型分组注册数量分布（饼图）
     * provDist  省分组注册数量分布（饼图）
     * provBar   省分组注册数量统计（柱状图）
     * trend     近十二个月注册数量趋势（折线图）
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<NodeInfo> all = nodeInfoService.list();

        Map<Integer, String> typeNames = new LinkedHashMap<>();
        typeNames.put(1, "养殖企业");
        typeNames.put(2, "冷冻加工企业");
        typeNames.put(3, "批发商");
        typeNames.put(4, "零售商");

        //1.企业类型分组
        Map<Integer, Long> typeCount = all.stream()
                .filter(n -> n.getNodeType() != null)
                .collect(Collectors.groupingBy(NodeInfo::getNodeType, Collectors.counting()));
        List<Map<String, Object>> typeDist = new ArrayList<>();
        typeCount.forEach((type, count) -> typeDist.add(item(typeNames.getOrDefault(type, "未知类型"), count)));

        //2.省分组
        Map<Integer, String> provNames = provinceService.list().stream()
                .collect(Collectors.toMap(Province::getProvId, Province::getProvName));
        Map<Integer, Long> provCount = all.stream()
                .filter(n -> n.getProvId() != null)
                .collect(Collectors.groupingBy(NodeInfo::getProvId, Collectors.counting()));
        List<Map<String, Object>> provDist = new ArrayList<>();
        provCount.forEach((provId, count) -> provDist.add(item(provNames.getOrDefault(provId, "未知省份"), count)));
        provDist.sort((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")));

        //3.近十二个月注册趋势
        YearMonth start = YearMonth.now().minusMonths(11);
        List<String> months = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            months.add(ym.toString());
            counts.add(all.stream()
                    .filter(n -> n.getRegDate() != null && YearMonth.from(n.getRegDate()).equals(ym))
                    .count());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", all.size());
        data.put("typeDist", typeDist);
        data.put("provDist", provDist);
        data.put("provBar", provDist);
        data.put("trend", Map.of("months", months, "counts", counts));
        return Result.success(data);
    }

    private Map<String, Object> item(String name, Long value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }
}
