package com.atguigu.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.enums.PayType;
import com.atguigu.payment.enums.wxpay.WxRefundStatus;
import com.atguigu.payment.mapper.RefundInfoMapper;
import com.atguigu.payment.entity.RefundInfo;
import com.atguigu.payment.service.OrderInfoService;
import com.atguigu.payment.service.RefundInfoService;
import com.atguigu.payment.util.OrderNoUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {

    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 创建退款单
     */
    @Override
    public RefundInfo createRefundsByOrderNo(String orderNo, String reason) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo); //订单编号
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo()); //退款单号
        refundInfo.setTotalFee(orderInfo.getTotalFee()); //原订单金额
        refundInfo.setRefund(orderInfo.getTotalFee()); //退款金额(原订单金额是多少就退多少)
        refundInfo.setPaymentType(PayType.WXPAY.getType());
        refundInfo.setReason(reason); //退款原因
        save(refundInfo);
        return refundInfo;
    }

    /**
     * 申请退款时(主动查退款信息信息时)，更新退款单
     */
    @Override
    public void updateRefund1(Refund response) {
        lambdaUpdate()
                .eq(RefundInfo::getRefundNo, response.getOutRefundNo()) //退款单号
                .set(RefundInfo::getRefundId,response.getRefundId())
                //申请退款中的返回参数
                .set(response.getStatus() != null, RefundInfo::getRefundStatus, response.getStatus()) //退款状态
                //退款回调中的回调参数
                //存入所有响应结果
                .set(RefundInfo::getContentReturn, JSON.toJSONString(response))
                .update();
    }

    /**
     * 退款回调时候，更新退款单
     */
    @Override
    public void updateRefund2(RefundNotification refundNotification) {
        String outRefundNo = refundNotification.getOutRefundNo();
        String refundId = refundNotification.getRefundId();
        String refundStatus = refundNotification.getRefundStatus().name();
        lambdaUpdate()
                .eq(RefundInfo::getRefundNo, outRefundNo) //退款单号
                .set(RefundInfo::getRefundId,refundId)
                //退款回调中的返回参数
                .set(RefundInfo::getRefundStatus, refundStatus) //退款状态
                //存入所有响应结果
                .set(RefundInfo::getContentNotify, JSON.toJSONString(refundNotification))
                .update();
    }

    /**
     * 找出申请退款超过minutes分钟并且未成功的退款单
     */
    @Override
    public List<RefundInfo> getNoRefundOrderByDuration(int minutes,String paymentType) {
        //minutes分钟之前的时间
        Instant instant = Instant.now().minus(Duration.ofMinutes(minutes));
        return lambdaQuery()
                .eq(RefundInfo::getRefundStatus, WxRefundStatus.PROCESSING.getType())
                .eq(RefundInfo::getPaymentType, paymentType)
                .le(RefundInfo::getCreateTime, instant)
                .list();
    }

    /**
     * 根据订单号创建退款订单
     */
    @Override
    public RefundInfo createRefundByOrderNoForAliPay(String orderNo, String reason) {

        //根据订单号获取订单信息
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);

        //根据订单号生成退款订单
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);//订单编号
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo());//退款单编号

        refundInfo.setTotalFee(orderInfo.getTotalFee());//原订单金额(分)
        refundInfo.setRefund(orderInfo.getTotalFee());//退款金额(分)
        refundInfo.setPaymentType(PayType.ALIPAY.getType());
        refundInfo.setReason(reason);//退款原因

        //保存退款订单
        baseMapper.insert(refundInfo);

        return refundInfo;
    }

    /**
     * 更新退款记录
     */
    @Override
    public void updateRefundForAliPay(String refundNo, String content, String refundStatus) {
        //根据退款单编号修改退款单
        lambdaUpdate()
                .eq(RefundInfo::getRefundNo, refundNo)
                .set(RefundInfo::getRefundStatus, refundStatus) //退款状态
                .set(RefundInfo::getRefundId, JSON.parseObject(content).getString("refund_id"))
                .set(RefundInfo::getContentReturn, content) //将全部响应结果存入数据库的content字段
                .update();
    }
}
