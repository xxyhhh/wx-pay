package com.atguigu.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.payment.enums.PayType;
import com.atguigu.payment.mapper.PaymentInfoMapper;
import com.atguigu.payment.entity.PaymentInfo;
import com.atguigu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.stereotype.Service;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    /**
     * 创建支付信息
     */
    @Override
    public void createPaymentInfo(Transaction transaction) {
        // 获取商户订单号
        String orderNo = transaction.getOutTradeNo();
        //微信支付订单号
        String transactionId = transaction.getTransactionId();
        //支付类型
        String tradeType = transaction.getTradeType().name();
        //交易状态
        String tradeState = transaction.getTradeState().name();

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType(tradeType);
        paymentInfo.setTradeState(tradeState);
        //用户实际支付金额（分），这里无需考虑int装不下，最大 ≈ 2147 万元
        paymentInfo.setPayerTotal(transaction.getAmount().getPayerTotal());

        // ✅ 将整个 transaction 转换为 JSON 字符串，存入 content 字段
        String contentJson = JSON.toJSONString(transaction);
        paymentInfo.setContent(contentJson);
        save(paymentInfo);
    }
}
