package com.atguigu.payment.service.impl;

import com.atguigu.payment.mapper.OrderInfoMapper;
import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

}
