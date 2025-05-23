package com.atguigu.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.mapper.RefundInfoMapper;
import com.atguigu.payment.entity.RefundInfo;
import com.atguigu.payment.service.OrderInfoService;
import com.atguigu.payment.service.RefundInfoService;
import com.atguigu.payment.util.OrderNoUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        refundInfo.setReason(reason); //退款原因
        save(refundInfo);
        return refundInfo;
    }

    /**
     * 申请退款时，更新退款单
     */
    @Override
    public void updateRefund1(Refund response) {
        lambdaUpdate()
                .eq(RefundInfo::getRefundNo, response.getOutRefundNo()) //退款单号
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
        String refundStatus = refundNotification.getRefundStatus().name();
        lambdaUpdate()
                .eq(RefundInfo::getRefundNo, outRefundNo) //退款单号
                //退款回调中的返回参数
                .set(RefundInfo::getRefundStatus, refundStatus) //退款状态
                //存入所有响应结果
                .set(RefundInfo::getContentNotify, JSON.toJSONString(refundNotification))
                .update();
    }
}
