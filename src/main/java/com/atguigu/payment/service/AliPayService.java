package com.atguigu.payment.service;

import java.util.Map;

/**
 * ClassName:
 * Package: com.atguigu.payment.service
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/26 21:29
 * @Version 1.0
 */
public interface AliPayService {

    String tradeCreate(Long productId);

    void processOrder(Map<String, String> params);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    void checkOrderStatus(String orderNo);

    void refund(String orderNo, String reason);

    String queryRefund(String orderNo);

    String queryBill(String billDate, String type);
}
