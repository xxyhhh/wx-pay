package com.atguigu.payment.service;

import com.wechat.pay.java.service.billdownload.model.QueryBillEntity;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.Refund;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * ClassName:
 * Package: com.atguigu.payment.service
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/12 20:46
 * @Version 1.0
 */
public interface WxPayService {

    Map<String, Object> nativePay(Long productId);

    void wxnotify(HttpServletRequest request);

    void wxRefundsNotify(HttpServletRequest request);

    void cancelOrder(String orderNo);

    Transaction queryOrder(String orderNo);

    void checkOrderStatus(String orderNo);

    void refunds(String orderNo, String reason);

    Refund queryRefund(String refundNo);

    QueryBillEntity queryBill(String billDate, String type);

    String downloadBill(String billDate, String type);

    void checkRefundStatus(String refundNo);
}
