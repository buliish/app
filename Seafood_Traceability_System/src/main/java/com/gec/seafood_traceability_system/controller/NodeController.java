package com.gec.seafood_traceability_system.controller;

import com.gec.seafood_traceability_system.utils.JwtUtil;
import com.gec.seafood_traceability_system.utils.PasswordUtil;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;

import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gec.seafood_traceability_system.pojo.NodeInfo;
import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.service.NodeInfoService;

import java.util.HashMap;
import java.util.Map;

/**
 * 流通节点端共通功能控制器（3.2.2）
 * 统一接口前缀：/user（登录 / 退出 / 企业信息 / 更新密码）
 */
@RestController
@RequestMapping("/user")
public class NodeController {

    @Autowired
    private NodeInfoService nodeInfoService;

    // 登录：接收前端 JSON { code, password }，校验成功后生成并返回 token
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        String code = params.get("code");
        String password = params.get("password");
        NodeInfo n = nodeInfoService.findByCode(code);
        if (n == null) {
            return Result.error("用户不存在");
        }
        if (PasswordUtil.matches(password, n.getPassword())) {
            // 平滑升级：库里还是历史明文时，登录成功即改写为 BCrypt 哈希。
            // 注意此处必须用 updatePwdById：登录接口在拦截器白名单里，
            // ThreadLocal 尚未赋值，走 updatePwd() 会拿到 null。
            if (!PasswordUtil.isHashed(n.getPassword())) {
                nodeInfoService.updatePwdById(n.getNodeId(), PasswordUtil.encode(password));
            }
            Map<String, Object> claims = new HashMap<>();
            // 与管理端的 claims.put("role","admin") 对称，供 NodeAuthInterceptor 区分身份
            claims.put("role", "node");
            claims.put("id", n.getNodeId());
            // 登录编码作为 JWT 的用户名
            claims.put("username", n.getCode());
            String token = JwtUtil.genToken(claims);
            return Result.success(token);
        }
        return Result.error("密码错误");
    }

    // 退出登录：JWT 无服务端状态，返回成功后由前端清理本地登录信息
    @PostMapping("/logout")
    public Result logout() {
        return Result.success();
    }

    // 查询指定登录编码的企业信息（登录页 / 功能菜单页展示企业名称与类型）
    @GetMapping("/getInfo/{code}")
    public Result getInfo(@PathVariable String code) {
        NodeInfo n = nodeInfoService.findByCode(code);
        if (n == null) {
            return Result.error("用户不存在");
        }
        return Result.success(n);
    }

    // 更新密码：接收前端 JSON { oldPwd, newPwd, rePwd }
    // 只有原密码正确才允许修改，防止他人随意改密
    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params) {
        String oldPwd = params.get("oldPwd");
        String newPwd = params.get("newPwd");
        String rePwd = params.get("rePwd");

        if (!StringUtils.hasText(oldPwd) || !StringUtils.hasText(newPwd) || !StringUtils.hasText(rePwd)) {
            return Result.error("缺少必要参数");
        }

        //取出当前登录企业，校验原密码
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        NodeInfo loginNode = nodeInfoService.findByCode(username);
        if (loginNode == null) {
            return Result.error("用户不存在");
        }
        if (!PasswordUtil.matches(oldPwd, loginNode.getPassword())) {
            return Result.error("原密码错误");
        }
        if (!newPwd.equals(rePwd)) {
            return Result.error("新密码与确认密码不一致");
        }

        nodeInfoService.updatePwd(PasswordUtil.encode(newPwd));
        return Result.success();
    }

    // 获取当前登录企业信息
    @GetMapping("/userInfo")
    public Result<NodeInfo> userInfo() {
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        NodeInfo nodeInfo = nodeInfoService.findByCode(username);
        return Result.success(nodeInfo);
    }

    // 更新企业信息（法人 / 联系电话 / 地址）
    @PutMapping("/update")
    public Result update(@RequestBody @Validated NodeInfo nodeInfo) {
        nodeInfoService.update(nodeInfo);
        return Result.success();
    }
}
