package com.atguigu.payment.service.impl;

import com.atguigu.payment.entity.Product;
import com.atguigu.payment.enums.OrderStatus;
import com.atguigu.payment.mapper.OrderInfoMapper;
import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.mapper.ProductMapper;
import com.atguigu.payment.service.OrderInfoService;
import com.atguigu.payment.util.OrderNoUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Resource
    private ProductMapper productMapper;

    @Override
    public OrderInfo createOrderByProductId(Long productId) {

        // 查找已经存在但未支付的订单
        OrderInfo orderInfo = this.getNoPayOrderByProductId(productId);
        if (orderInfo != null) {
            return orderInfo;
        }

        // 获取商品信息
        Product product = productMapper.selectById(productId);
        // 生成订单
        orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(product.getPrice()); //分
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        baseMapper.insert(orderInfo);
        return orderInfo;
    }

    /**
     * 存储订单二维码
     */
    @Override
    public void saveCodeUrl(String orderNo, String codeUrl) {
        lambdaUpdate().eq(OrderInfo::getOrderNo, orderNo)
                      .set(OrderInfo::getCodeUrl, codeUrl)
                      .update();
    }

    /****
     * 根据商品id查找未支付的订单
     * 防止重复创建订单对象
     */
    private OrderInfo getNoPayOrderByProductId(Long productId){
        return lambdaQuery().eq(OrderInfo::getProductId, productId)
                .eq(OrderInfo::getOrderStatus, OrderStatus.NOTPAY.getType())
              //.eq(OrderInfo::getUserId, xxx)     这里是要填的，但是这个项目没做权限所以省略了
                .one();
    }



}
