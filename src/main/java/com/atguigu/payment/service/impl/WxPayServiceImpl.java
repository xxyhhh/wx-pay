package com.atguigu.payment.service.impl;

import com.atguigu.payment.config.WxPayConfig;
import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.enums.wxpay.WxNotifyType;
import com.atguigu.payment.service.OrderInfoService;
import com.atguigu.payment.service.WxPayService;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName:
 * Package: com.atguigu.payment.service.impl
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/12 20:46
 * @Version 1.0
 */
@Slf4j
@Service
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private WxPayConfig wxPayConfig;
    @Autowired
    private RSAAutoCertificateConfig config;
    @Autowired
    private OrderInfoService orderInfoService;


    @Override
    public Map<String, Object> nativePay(Long productId) {
        try{
        //生成订单
        OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId);
        String codeUrl =  orderInfo.getCodeUrl();
        if(StringUtils.hasText(codeUrl)){
            log.info("订单已经存在");
            //返回二维码
            Map<String, Object> map = new HashMap<>();
            map.put("codeUrl", codeUrl);
            map.put("orderNo", orderInfo.getOrderNo());
            return map;
        }

        //NativePayService: 扫码支付（商户侧生成二维码） com.wechat.pay.java.service.partnerpayments.nativepay.NativePayService
        //JsapiServiceExtension:  JSAPI 支付（公众号/H5） com.wechat.pay.java.service.jsapi.JsapiService
        //H5Service:  H5 浏览器支付  com.wechat.pay.java.service.h5.H5Service
        //AppService: APP 内支付   com.wechat.pay.java.service.app.AppService

        //创建微信支付使用对象
        NativePayService service = new NativePayService.Builder().config(config).build();
        //创建request请求，封装参数
        PrepayRequest request = new PrepayRequest();

        request.setAppid(wxPayConfig.getAppid());
        request.setMchid(wxPayConfig.getMchId());
        request.setDescription(orderInfo.getTitle());
        request.setOutTradeNo(orderInfo.getOrderNo());   //商户订单号
        request.setNotifyUrl(wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));
        Amount amount = new Amount();
        amount.setTotal(orderInfo.getTotalFee()); //分
        request.setAmount(amount);

        // 调用下单方法，得到应答
        PrepayResponse response = service.prepay(request);
        codeUrl = response.getCodeUrl();

        //保存二维码到订单数据
        orderInfoService.saveCodeUrl(orderInfo.getOrderNo(), codeUrl);

        // 返回订单号和二维码
        Map<String, Object> map = new HashMap<>();
        map.put("codeUrl", codeUrl);
        map.put("orderNo", orderInfo.getOrderNo());
        return map;
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException();
        }
}}
