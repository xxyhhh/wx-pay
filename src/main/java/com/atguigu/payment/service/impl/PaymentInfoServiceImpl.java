package com.atguigu.payment.service.impl;

import com.atguigu.payment.mapper.PaymentInfoMapper;
import com.atguigu.payment.entity.PaymentInfo;
import com.atguigu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

}
