package com.atguigu.payment.service;

import com.atguigu.payment.entity.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;

public interface RefundInfoService extends IService<RefundInfo> {

    RefundInfo createRefundsByOrderNo(String orderNo, String reason);

    void updateRefund1(Refund response);

    void updateRefund2(RefundNotification refundNotification);
}
