package com.atguigu.payment.controller;

import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.enums.OrderStatus;
import com.atguigu.payment.service.OrderInfoService;
import com.atguigu.payment.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * ClassName:
 * Package: com.atguigu.payment.controller
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/15 19:29
 * @Version 1.0
 */
@CrossOrigin
@Tag(name = "商品订单管理")
@RestController
@RequestMapping("/api/order-info")
public class OrderInfoController {

    @Resource
    private OrderInfoService orderInfoService;

    @GetMapping("/list")
    @Operation(summary = "商品订单列表")
    public Result<HashMap<String, Object>> list() {
        List<OrderInfo> list = orderInfoService.listOrderByCreateTimeDese();
        return Result.okByMap("list",list);
    }

    @GetMapping("query-order-status/{orderNo}")
    @Operation(summary = "查询订单状态")
    public Result queryOrderStatus(@PathVariable("orderNo") String orderNo) {
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        if(OrderStatus.SUCCESS.getType().equals(orderStatus)){
            return Result.build(null,0,"支付成功");
        }
        // 前端会不断发请求，直到返回code为0为止，然后才做页面的跳转
        return Result.build(null,101,"支付中......");
    }
}
