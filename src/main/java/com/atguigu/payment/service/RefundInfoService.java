package com.atguigu.payment.service;

import com.atguigu.payment.entity.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;

import java.util.List;

public interface RefundInfoService extends IService<RefundInfo> {

    RefundInfo createRefundsByOrderNo(String orderNo, String reason);

    void updateRefund1(Refund response);

    void updateRefund2(RefundNotification refundNotification);

    List<RefundInfo> getNoRefundOrderByDuration(int minutes,String paymentType);

    RefundInfo createRefundByOrderNoForAliPay(String orderNo, String reason);

    void updateRefundForAliPay(String refundNo, String body, String type);
}
