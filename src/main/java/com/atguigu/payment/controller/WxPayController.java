package com.atguigu.payment.controller;

import com.alibaba.fastjson2.JSON;
import com.atguigu.payment.service.WxPayService;
import com.atguigu.payment.utils.Result;
import com.wechat.pay.java.service.billdownload.model.QueryBillEntity;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.Refund;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    private WxPayService wxPayService;

    @Operation(summary = "调用统一下单API,生成支付二维码")
    @PostMapping("/native/{productId}")
    public Result<Map<String, Object>> createWxPayment(@PathVariable("productId") Long productId) {
        log.info("【微信支付】发起支付请求,调用统一下单api开始创建订单, 商品ID: {}", productId);
        //返回支付二维码和订单号
        Map<String, Object> map = wxPayService.nativePay(productId);
        return Result.ok(map);
    }

    @PostMapping("/native/notify")
    @Operation(summary = "支付成功后，微信调用该方法")
    public ResponseEntity<Map<String, Object>> nativeNotify(HttpServletRequest request) {
        log.info("【微信支付】接收支付通知");
        try {
            wxPayService.wxnotify(request);

            //返回成功
            Map<String, Object> result = new HashMap<>();
            result.put("code", "SUCCESS");
            result.put("message", "成功");

            return ResponseEntity.ok(result); // 显式返回 200 OK + JSON body
        } catch (Exception e) {
            // 1、如果超时、抛异常、返回非 success，微信后台会重复调用该接口，具体规则看语雀的“支付通知过程”的官方文档
            // 2、对返回的成功和失败消息官方有要求，看第一点的文档。
            e.printStackTrace();
        }

        //返回失败
        Map<String, Object> result = new HashMap<>();
        result.put("code", "FAIL");
        result.put("message", "失败");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    @PostMapping("/cancel/{orderNo}")
    @Operation(summary = "用户取消订单")
    public Result<Void> cancelOrder(@PathVariable("orderNo") String orderNo) {
        wxPayService.cancelOrder(orderNo);
        // 这里有点无奈了，我按视频的来(0即是表示200)，到时自己写项目的时候前端改改即可
        return Result.build(null, 0, "订单已取消");
    }

    @PostMapping("/refunds/{orderNo}/{reason}")
    @Operation(summary = "用户申请退款")
    public Result<Void> refunds(@PathVariable("orderNo") String orderNo,
                                @PathVariable("reason") String reason) {
        wxPayService.refunds(orderNo, reason);
        return Result.ok(null);
    }

    @PostMapping("/refunds/notify")
    @Operation(summary = "退款状态改变后，微信回调该方法")
    public ResponseEntity<Map<String, Object>> refundsNotify(HttpServletRequest request) {
        try {
            wxPayService.wxRefundsNotify(request);

            //返回成功
            Map<String, Object> result = new HashMap<>();
            result.put("code", "SUCCESS");
            result.put("message", "成功");

            return ResponseEntity.ok(result); // 显式返回 200 OK + JSON body
        } catch (Exception e) {
            // 1、如果超时、抛异常、返回非 success，微信后台会重复调用该接口，具体规则看语雀的“支付通知过程”的官方文档
            // 2、对返回的成功和失败消息官方有要求，看第一点的文档。
            e.printStackTrace();
        }

        //返回失败
        Map<String, Object> result = new HashMap<>();
        result.put("code", "FAIL");
        result.put("message", "失败");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    @GetMapping("/downloadbill/{billDate}/{type}")
    @Operation(summary = "下载商家的账单")
    public Result<HashMap<String, Object>> downloadBill(@PathVariable("billDate") String billDate, @PathVariable("type") String type){
        String res = wxPayService.downloadBill(billDate, type);
        HashMap<String, Object> map = new HashMap<>();
        map.put("result", res);
        return Result.ok(map);
    }

    @GetMapping("query/{orderNo}")
    @Operation(summary = "查询订单信息，测试用")
    public Result<String> queryOrder(@PathVariable("orderNo") String orderNo) {
        Transaction result = wxPayService.queryOrder(orderNo);
        String jsonString = JSON.toJSONString(result);
        return Result.ok(jsonString);
    }

    @GetMapping("/query-refund/{refundNo}")
    @Operation(summary = "查询退款:测试用")
    public Result<String> queryRefund(@PathVariable("refundNo") String refundNo) {
        Refund result = wxPayService.queryRefund(refundNo);
        String jsonString = JSON.toJSONString(result);
        return Result.ok(jsonString);
    }

    @GetMapping("/querybill/{billDate}/{type}")
    @Operation(summary = "获取账单url，测试用")
    public Result<String> queryBill(@PathVariable("billDate") String billDate, @PathVariable("type") String type) {

        QueryBillEntity obj = wxPayService.queryBill(billDate, type);
        String downloadUrl = obj.getDownloadUrl();
        return Result.ok(downloadUrl).setMessage("获取账单url成功");
    }
}
