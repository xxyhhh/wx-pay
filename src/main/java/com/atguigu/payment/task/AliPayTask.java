package com.atguigu.payment.task;

import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.enums.PayType;
import com.atguigu.payment.service.AliPayService;
import com.atguigu.payment.service.OrderInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ClassName:
 * Package: com.atguigu.payment.task
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/29 10:37
 * @Version 1.0
 */
@Slf4j
@Component
public class AliPayTask {

    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private AliPayService aliPayService;

    /**
     * 从第0秒开始，每隔30秒执行一次，查询订单创建超过5min，并且未支付的订单(解决支付宝回调失败问题)
     */
    @Scheduled(cron = "0/30 * * * * *")
    public void orderConfirm() {
        log.info("【支付宝支付】开始执行定时任务,查询所有规定时间内未支付订单，向支付宝平台验证支付状态");
        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderByDuration(5, PayType.ALIPAY.getType());
        orderInfoList.forEach(orderInfo -> {
            String orderNo = orderInfo.getOrderNo();
            log.warn("【支付宝支付】超时订单：{}", orderNo);

            //核实订单状态:调用支付宝查单接口
            aliPayService.checkOrderStatus(orderNo);
        });
    }
}
