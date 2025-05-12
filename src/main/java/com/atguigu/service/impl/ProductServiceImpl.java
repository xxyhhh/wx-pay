package com.atguigu.service.impl;

import com.atguigu.mapper.ProductMapper;
import com.atguigu.payment.entity.Product;
import com.atguigu.service.ProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

}
