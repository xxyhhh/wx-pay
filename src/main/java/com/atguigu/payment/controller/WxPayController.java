package com.atguigu.payment.controller;

import com.atguigu.payment.config.WxPayConfig;
import com.atguigu.payment.entity.Product;
import com.atguigu.payment.service.WxPayService;
import com.atguigu.payment.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery;

/**
 * ClassName:
 * Package: com.atguigu.payment.controller
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/12 10:58
 * @Version 1.0
 */
@CrossOrigin
@Tag(name = "网站微信支付接口")
@RequestMapping("/api/wx-pay")
@RestController
@Slf4j
public class WxPayController {

    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private WxPayService wxPayService;

    @Operation(summary = "调用统一下单API,生成支付二维码")
    @PostMapping("/native/{productId}")
    public Result<Map<String, Object>> createWxPayment(@PathVariable("productId") Long productId) {
        log.info("发起支付请求，调用统一下单api");
        //返回支付二维码和订单号
        Map<String, Object> map = wxPayService.nativePay(productId);
        return Result.ok(map);
    }
}
