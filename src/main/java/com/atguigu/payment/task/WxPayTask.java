package com.atguigu.payment.task;

import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.entity.RefundInfo;
import com.atguigu.payment.enums.PayType;
import com.atguigu.payment.service.OrderInfoService;
import com.atguigu.payment.service.RefundInfoService;
import com.atguigu.payment.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class WxPayTask {

    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private RefundInfoService refundInfoService;
    @Autowired
    private WxPayService wxPayService;

    /**
     * 从第0秒开始，每隔30秒执行一次，查询订单创建超过5min，并且未支付的订单(解决wx回调失败问题)
     * 如果系统重启，要等到下一个周期才会执行，自己要查一下怎么处理(不过这里才30s比较短).......
     * 倒计时自己在前端写就可以(超过时间，直接掉后端接口改状态，改为超时已关闭，然后掉wxapi关闭订单(这个没写))---但如果前端刷新倒计时也没了...自己想别的办法去网上找---代驾好像用的是redis
     */
//    @Scheduled(cron = "0/30 * * * * *")
    public void orderConfirm() {
        log.info("开始执行定时任务,查询所有规定时间内未支付订单，向微信平台验证支付状态");
        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderByDuration(5,PayType.WXPAY.getType());
        orderInfoList.forEach(orderInfo -> {
            log.warn("超时订单：{}", orderInfo.getOrderNo());

            //核实订单状态:调用微信支付查单接口
            wxPayService.checkOrderStatus(orderInfo.getOrderNo());
        });
    }

    /**
     * 从第0秒开始，每隔30秒执行一次，查询订单创建超过5min，并且未成功的退款单，(解决wx回调失败问题)
     */
//    @Scheduled(cron = "0/30 * * * * *")
    public void refundConfirm() {
        log.info("开始执行定时任务,查询所有规定时间内未成功退款的订单，向微信平台验证退款状态");
        //找出申请退款超过5min并且未成功的退款单
        List<RefundInfo> refundInfoList = refundInfoService.getNoRefundOrderByDuration(5);
        refundInfoList.forEach(refundInfo -> {
            log.warn("超时未退款的退款单号：{}", refundInfo.getOrderNo());

            //核实订单状态:调用微信支付退款接口
            wxPayService.checkRefundStatus(refundInfo.getRefundNo());
        });
    }
}