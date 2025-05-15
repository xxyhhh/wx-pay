package com.atguigu.payment.service;

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
    /**
     * 发起支付请求，调用统一下单api,返回code_url,生成支付二维码
     * @param productId
     * @return
     */
    Map<String, Object> nativePay(Long productId);
}
