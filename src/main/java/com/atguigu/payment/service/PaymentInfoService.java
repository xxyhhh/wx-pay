package com.atguigu.payment.service;

import com.atguigu.payment.entity.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wechat.pay.java.service.payments.model.Transaction;

public interface PaymentInfoService extends IService<PaymentInfo> {

    void createPaymentInfo(Transaction transaction);
}
