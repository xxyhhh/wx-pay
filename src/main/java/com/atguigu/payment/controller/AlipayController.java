package com.atguigu.payment.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayConstants;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.service.AliPayService;
import com.atguigu.payment.service.OrderInfoService;
import com.atguigu.payment.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * ClassName:
 * Package: com.atguigu.payment.controller
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/26 21:22
 * @Version 1.0
 */
@CrossOrigin
@RestController
@RequestMapping("/api/ali-pay")
@Tag(name = "网站支付宝支付")
@Slf4j
public class AlipayController {

    @Resource
    private AliPayService aliPayService;
    @Resource
    private Environment config;
    @Resource
    private OrderInfoService orderInfoService;

    @PostMapping("/trade/page/pay/{productId}")
    @Operation(summary = "统一收单下单并支付页面接口的调用")
    public Result<HashMap<String, Object>> tradePagePay(@PathVariable("productId") Long productId) {
        log.info("发起支付请求，调用统一收单下单并支付页面接口");
        //调用接口，返回支付表单给前端，这个表单会自动提交，action到支付页面
        String formStr = aliPayService.tradeCreate(productId);
        return Result.okByMap("formStr", formStr);
    }

    @PostMapping("/trade/notify")
    @Operation(summary = "支付通知")
    public String tradeNotify(@RequestParam Map<String, String> params) {
        log.info("【支付宝支付】支付通知正在执行");
        log.info("【支付宝支付】通知参数 ==> {}", params);
        String result = "failure";

        try {
            //异步通知验签(调用SDK验证签名)
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    config.getProperty("alipay.alipay-public-key"),
                    AlipayConstants.CHARSET_UTF8,
                    AlipayConstants.SIGN_TYPE_RSA2);
            if (!signVerified) {
                // TODO 验签失败则记录异常日志，并在response中返回failure.
                log.error("【支付宝支付】支付成功,异步通知验签失败!");
                return result;
            }
            // TODO 验签成功后
            log.info("【支付宝支付】支付成功,异步通知验签成功!");
            // TODO 按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验
            // 1、商户需要验证该通知数据中的 out_trade_no 是否为商户系统中创建的订单号
            OrderInfo order = orderInfoService.getOrderByOrderNo(params.get("out_trade_no"));
            if (order == null) {
                log.error("【支付宝支付】订单不存在");
                return result;
            }
            // 2、判断 total_amount 是否确实为该订单的实际金额（即商户订单创建时的金额）
            String totalAmount = params.get("total_amount");
            int totalAmountInt = new BigDecimal(totalAmount).multiply(new BigDecimal("100")).intValue();
            int totalFeeInt = order.getTotalFee().intValue();
            if (totalAmountInt != totalFeeInt) {
                log.error("【支付宝支付】订单金额不一致");
                return result;
            }
            // 3、校验通知中的 seller_id（或者 seller_email）是否为 out_trade_no 这笔单据的对应的操作方（有的时候，一个商家可能有多个 seller_id/seller_email）。
            String sellerId = params.get("seller_id");
            String sellerIdProperty = config.getProperty("alipay.seller-id");
            if (!sellerId.equals(sellerIdProperty)) {
                log.error("【支付宝支付】商户pid不一致");
                return result;
            }
            // 4、校验通知中的 app_id 是否为该商户本身
            String appId = params.get("app_id");
            String appIdProperty = config.getProperty("alipay.app-id");
            if (!appId.equals(appIdProperty)) {
                log.error("【支付宝支付】商户appId不一致");
                return result;
            }
            // 只有交易通知状态为 TRADE_SUCCESS(产品支持退款功能) 或 TRADE_FINISHED(产品不支持退款功能) 时，支付宝才会认定为买家付款成功
            // 由于网页支付是支持退款功能的，我们只需判断 TRADE_SUCCESS 即可
            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                log.error("【支付宝支付】支付未成功");
                return result;
            }
            // 处理业务，修改订单状态，记录支付日志
            aliPayService.processOrder(params);
            // 向支付宝返回成功的结果，如果不是 success，则会不断重发通知(沙箱环境不会重发)，也是有间隔
            result = "success";
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @PostMapping("/trade/close/{orderNo}")
    @Operation(summary = "用户取消订单")
    public Result<String> tradeClose(@PathVariable("orderNo") String orderNo) {
        log.info("用户取消订单");
        //调用关单接口
        aliPayService.cancelOrder(orderNo);
        return Result.ok("订单已取消");
    }

    @Operation(summary = "申请退款")
    @PostMapping("/trade/refund/{orderNo}/{reason}")
    public Result<String> tradeRefund(@PathVariable("orderNo") String orderNo,
                                     @PathVariable("reason") String reason) {
        log.info("用户申请退款");
        //调用退款接口
        aliPayService.refund(orderNo, reason);
        return Result.ok(null);
    }

    @GetMapping("/trade/query/{orderNo}")
    @Operation(summary = "查询订单信息，测试用")
    public Result<Map<String, Object>> queryOrder(@PathVariable("orderNo") String orderNo) {
        String jsonString = aliPayService.queryOrder(orderNo);
        Map<String, Object> res = JSON.parseObject(jsonString, new TypeReference<Map<String, Object>>() {});
        return Result.ok(res);
    }

    @GetMapping("/trade/fastpay/refund/{orderNo}")
    @Operation(summary = "查询单笔退款")
    public Result<Map<String, Object>> queryRefund(@PathVariable("orderNo") String orderNo) {
        log.info("支付宝查询单笔退款");
        String jsonString = aliPayService.queryRefund(orderNo);
        Map<String, Object> res = JSON.parseObject(jsonString, new TypeReference<Map<String, Object>>() {});
        return Result.ok(res);
    }


    /**
     * 根据账单类型和日期获取账单url地址
     */
    @GetMapping("/bill/downloadurl/query/{billDate}/{type}")
    @Operation(summary = "获取账单url")
    public Result<HashMap<String, Object>> queryTradeBill(@PathVariable("billDate") String billDate, @PathVariable("type") String type)  {
        log.info("支付宝获取账单url,{}",billDate);
        String downloadUrl = aliPayService.queryBill(billDate, type);
        return Result.okByMap("downloadUrl", downloadUrl).setMessage("获取账单url成功");
    }
}
