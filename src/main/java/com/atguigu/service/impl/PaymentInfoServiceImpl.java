package com.atguigu.service.impl;

import com.atguigu.mapper.PaymentInfoMapper;
import com.atguigu.payment.entity.PaymentInfo;
import com.atguigu.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

}
