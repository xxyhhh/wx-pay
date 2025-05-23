package com.atguigu.payment.service.impl;

import com.atguigu.payment.entity.Product;
import com.atguigu.payment.enums.OrderStatus;
import com.atguigu.payment.mapper.OrderInfoMapper;
import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.mapper.ProductMapper;
import com.atguigu.payment.service.OrderInfoService;
import com.atguigu.payment.util.OrderNoUtils;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.apache.tomcat.SimpleInstanceManager;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

    /***
     * 按照创建时间倒序获取所有订单
     */
    @Override
    public List<OrderInfo> listOrderByCreateTimeDese() {
        return lambdaQuery().orderByDesc(OrderInfo::getCreateTime).list();
    }

    /**
     * 根据订单号，更新订单状态为支付成功
     */
    @Override
    public void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus) {
        // 执行更新
        lambdaUpdate().eq(OrderInfo::getOrderNo, orderNo)
                    .set(OrderInfo::getOrderStatus, orderStatus.getType())
                    .update();
    }

    /**
     * 根据订单号获取订单状态
     * 在这里我做了空对象的处理，因为可能在回调时，这个订单可能被删除了，所以要先判断一下，防止空指针
     */
    @Override
    public String getOrderStatus(String orderNo) {
        return
                Optional.ofNullable(lambdaQuery()
                                .eq(OrderInfo::getOrderNo, orderNo)
                                .select(OrderInfo::getOrderStatus)
                                .one())
                        .map(OrderInfo::getOrderStatus)
                        .orElse(null);
    }

    /**
     * 查询超过5min还未支付的订单
     */
    @Override
    public List<OrderInfo> getNoPayOrderByDuration(int minutes) {
        Instant minus = Instant.now().minus(Duration.ofMinutes(minutes)); // 当前时间减去 5min
        List<OrderInfo> list = lambdaQuery()
                .eq(OrderInfo::getOrderStatus, OrderStatus.NOTPAY.getType())
                .lt(OrderInfo::getCreateTime, minus)
                .list();
        return list;
    }

    /**
     * 根据订单号获取订单信息
     */
    @Override
    public OrderInfo getOrderByOrderNo(String orderNo) {
        OrderInfo orderInfo = lambdaQuery().eq(OrderInfo::getOrderNo, orderNo).one();
        return orderInfo;
    }

    /****
     * 根据商品id查找未支付的订单
     * 防止重复创建订单对象
     */
    private OrderInfo getNoPayOrderByProductId(Long productId){
        return lambdaQuery()
              //.eq(OrderInfo::getUserId, xxx)     这里是要填的，但是这个项目没做权限所以省略了
                .eq(OrderInfo::getProductId, productId)
                .eq(OrderInfo::getOrderStatus, OrderStatus.NOTPAY.getType())
                .one();
    }
}
