package com.atguigu.payment.service.impl;

import com.atguigu.payment.mapper.ProductMapper;
import com.atguigu.payment.entity.Product;
import com.atguigu.payment.service.ProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

}
