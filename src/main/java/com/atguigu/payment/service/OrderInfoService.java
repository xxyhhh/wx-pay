package com.atguigu.payment.service;

import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.enums.OrderStatus;
import com.atguigu.payment.enums.PayType;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wechat.pay.java.service.payments.model.Transaction;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {

    OrderInfo createOrderByProductId(Long productId,String paymentType);

    void saveCodeUrl(String orderNo, String codeUrl);

    List<OrderInfo> listOrderByCreateTimeDese();

    void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus);

    String getOrderStatus(String orderNo);

    List<OrderInfo> getNoPayOrderByDuration(int minutes, String paymentType);

    OrderInfo getOrderByOrderNo(String orderNo);
}
