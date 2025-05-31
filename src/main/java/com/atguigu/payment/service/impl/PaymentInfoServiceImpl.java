package com.atguigu.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.payment.enums.PayType;
import com.atguigu.payment.mapper.PaymentInfoMapper;
import com.atguigu.payment.entity.PaymentInfo;
import com.atguigu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    /**
     * 微信创建支付信息(记录支付日志)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
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

    /**
     * 支付宝创建支付信息(记录支付日志)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPaymentInfoForAliPay(Map<String, String> params) {
        // 获取商户订单号
        String orderNo = params.get("out_trade_no");
        //支付宝支付订单号
        String transactionId = params.get("trade_no");
        //支付状态
        String tradeStatus = params.get("trade_status");
        //交易金额
        String totalAmount = params.get("total_amount");
        int totalAmountInt = new BigDecimal(totalAmount).multiply(new BigDecimal("100")).intValue();

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.ALIPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType("电脑网站支付");
        paymentInfo.setTradeState(tradeStatus);
        paymentInfo.setPayerTotal(totalAmountInt);
        paymentInfo.setContent(JSON.toJSONString(params));
        save(paymentInfo);
    }
}
