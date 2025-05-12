package com.atguigu.service.impl;

import com.atguigu.mapper.OrderInfoMapper;
import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

}
