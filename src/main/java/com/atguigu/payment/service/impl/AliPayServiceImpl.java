package com.atguigu.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.TypeReference;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.diagnosis.DiagnosisUtils;
import com.alipay.api.domain.*;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.entity.RefundInfo;
import com.atguigu.payment.enums.OrderStatus;
import com.atguigu.payment.enums.PayType;
import com.atguigu.payment.enums.wxpay.AliPayTradeState;
import com.atguigu.payment.service.AliPayService;
import com.atguigu.payment.service.PaymentInfoService;
import com.atguigu.payment.service.RefundInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * ClassName:
 * Package: com.atguigu.payment.service
 * Description:
 *
 * @Author 何鸿辉
 * @Create 2025/5/26 21:29
 * @Version 1.0
 */
@Service
@Slf4j
public class AliPayServiceImpl implements AliPayService {

    @Resource
    private OrderInfoServiceImpl orderInfoService;
    @Resource
    private AlipayClient alipayClient;
    @Resource
    private Environment config;
    @Resource
    private PaymentInfoService paymentInfoService;
    @Resource
    private RefundInfoService refundInfoService;
    private static final BigDecimal HUNDRED = new BigDecimal("100");


    //    @Override
    @Transactional
    public String tradeCreate(Long productId) {
        try {
            log.info("生成订单");
            OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId, PayType.ALIPAY.getType());

            // 构造请求参数以调用接口
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();

            // 设置商户订单号
            model.setOutTradeNo(orderInfo.getOrderNo());
            // 设置订单总金额
            BigDecimal total = new BigDecimal(orderInfo.getTotalFee().toString())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP); //4舍5入保留2位小数
            model.setTotalAmount(total.toString());
            // 设置订单标题
            model.setSubject(orderInfo.getTitle());
            // 设置销售产品码(相当于是枚举，不同的支付场景这个码不同，网站的就用这个)
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
//        // 设置PC扫码支付的方式
            //  model.setQrPayMode("1");  //好看度从高到低：1 0 4（0和4差不多，4是可自定宽度的） 3，不写的内容更丰富
//
//        // 设置商户自定义二维码宽度
//        model.setQrcodeWidth(100L);
//
//        // 设置订单包含的商品列表信息
//        List<GoodsDetail> goodsDetail = new ArrayList<GoodsDetail>();
//        GoodsDetail goodsDetail0 = new GoodsDetail();
//        goodsDetail0.setGoodsName("ipad");
//        goodsDetail0.setAlipayGoodsId("20010001");
//        goodsDetail0.setQuantity(1L);
//        goodsDetail0.setPrice("2000");
//        goodsDetail0.setGoodsId("apple-01");
//        goodsDetail0.setGoodsCategory("34543238");
//        goodsDetail0.setCategoriesTree("124868003|126232002|126252004");
//        goodsDetail0.setShowUrl("http://www.alipay.com/xxx.jpg");
//        goodsDetail.add(goodsDetail0);
//        model.setGoodsDetail(goodsDetail);

//        // 设置订单绝对超时时间
//        model.setTimeExpire("2016-12-31 10:05:01");
            // 还有很多别的额外的东西，自己去看文档
            request.setBizModel(model);
            //成功跳转页面
            request.setReturnUrl(config.getProperty("alipay.return-url"));
            //异步回调
            request.setNotifyUrl(config.getProperty("alipay.notify-url"));

            AlipayTradePagePayResponse response = alipayClient.pageExecute(request, "POST");
            String pageRedirectionData = response.getBody();
            if (response.isSuccess()) {
                log.info("调用成功,返回结果====>{}", pageRedirectionData);
                return pageRedirectionData;
            } else {
                // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
                String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
                log.info("调用失败,返回结果====>诊断链接{},状态码{},信息{}", diagnosisUrl, response.getCode(), response.getMsg());
                throw new RuntimeException("创建支付交易失败");
            }
        } catch (AlipayApiException e) {
            log.error("支付宝API调用失败", e);
            throw new RuntimeException("支付宝支付请求失败", e);
        } catch (RuntimeException e) {
            log.error("运行时异常", e);
            throw new RuntimeException("创建支付交易失败", e);
        }
    }

    /**
     * 支付宝支付成功回调处理
     */
    @Override
    public void processOrder(Map<String, String> params) {
        log.info("支付成功回调处理");
        // 获取订单号
        String orderNo = params.get("out_trade_no");
        // 处理重复通知,由于网络原因，我们无法及时发送success给支付宝，导致他多次发送通知，同一个订单的日志会被记录多次
        // 接口的幂等性，无论接口被调用多少次，一下业务只执行一次
        // 是否加锁，分布式 or lock?
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        if (!OrderStatus.NOTPAY.getType().equals(orderStatus)) {
            return;
        }
        // 更新订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
        // 记录支付日志
        paymentInfoService.createPaymentInfoForAliPay(params);
    }

    /**
     * 用户取消订单
     */
    @Override
    public void cancelOrder(String orderNo) {
        // 调用支付宝提供的统一收单交易关闭接口
        closeOrder(orderNo);
        // 更新用户的订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);
    }

    private void closeOrder(String orderNo) {
        try {
            log.info("关单接口调用 ===> {}", orderNo);
            // 构造请求参数以调用接口
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            AlipayTradeCloseModel model = new AlipayTradeCloseModel();

            // 以下2选用1即可，但不能2个都不传
            // 设置该交易在支付宝系统中的交易流水号
            // model.setTradeNo("xxxxx");
            // 设置订单支付时传入的商户订单号
            model.setOutTradeNo(orderNo);
            request.setBizModel(model);

            AlipayTradeCloseResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("调用成功,返回结果====>{}", response.getBody());
            } else {
                // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
                String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
                log.info("调用失败,返回结果====>诊断链接{},状态码{},信息{}", diagnosisUrl, response.getCode(), response.getMsg());
                // 这里不抛异常(在外边改状态)，因为支付宝是你扫码后/填了账号密码后订单才会生成，如果用户没这么做，直接点取消订单后会报错，因为订单不存在
                // 这不算真正的异常
                // throw new RuntimeException("用户取消订单失败");
                // 但是这个支付宝的日志--sdk.biz.err--打印交易不存在，code40004，没找到解决办法
            }
        } catch (AlipayApiException e) {
            throw new RuntimeException("用户取消订单失败");
        }
    }

    /**
     * 通过商户订单号手动查询订单信息
     */
    @Override
    public String queryOrder(String orderNo) {
        try {
            log.info("查询订单接口调用 ===> {}", orderNo);

            // 构造请求参数以调用接口
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();

            // 2选1即可
            // 设置订单支付时传入的商户订单号
            model.setOutTradeNo(orderNo);
            // 设置支付宝交易号
            // model.setTradeNo("2014112611001004680 073956707");

            request.setBizModel(model);

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            String result = response.getBody();

            if (response.isSuccess()) {
                log.info("调用成功,返回结果====>{}", result);
                return result;
            } else {
                // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
                String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
                log.info("调用失败,返回结果====>诊断链接{},状态码{},信息{}", diagnosisUrl, response.getCode(), response.getMsg());
                // 订单不存在
                return null;
            }
        } catch (AlipayApiException e) {
            throw new RuntimeException("通过商户订单号手动查询订单信息失败", e);
        }
    }

    /**
     * 根据订单号调用支付宝查单接口，核实订单状态
     * 如果订单未创建，则更新商户端订单状态
     * 如果订单状态未支付，则调用关单接口关闭订单，并更新商户端订单状态为关闭
     * 如果订单状态已支付，则更新商户端订单状态为成功，并记录支付日志
     */
    @Override
    public void checkOrderStatus(String orderNo) {
        log.warn("根据订单号核实订单状态 ===> {}", orderNo);
        String result = queryOrder(orderNo);
        // 1、订单未创建
        if (result == null) {
            log.warn("支付宝核实订单未创建 ===> {}", orderNo);
            // 更新本地订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }
        // 2、订单状态未支付
        // 将 result 字符串解析为 JSONObject
        String tradeStatus = Optional.ofNullable(result)
                .map(JSON::parseObject)
                .map(js -> js.getObject("alipay_trade_query_response", AlipayTradeQueryResponse.class, JSONReader.Feature.SupportSmartMatch))
                .map(AlipayTradeQueryResponse::getTradeStatus)
                .orElse(null);
        if (AliPayTradeState.NOTPAY.getType().equals(tradeStatus)) {
            log.warn("支付宝核实订单未支付 ===> {}", orderNo);
            // 关单
            closeOrder(orderNo);
            // 更新本地商户端订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }
        // 3、订单状态已支付
        if (AliPayTradeState.SUCCESS.getType().equals(tradeStatus)) {
            log.warn("支付宝核实订单已支付 ===> {}", orderNo);
            // 更新本地商户端订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
            // 记录支付日志
            Map<String, String> paymentInfoMap = Optional.ofNullable(result)
                    .map(JSON::parseObject)
                    .map(json -> json.getObject("alipay_trade_query_response", new TypeReference<Map<String, String>>() {
                    }))
                    .orElse(Collections.emptyMap());
            paymentInfoService.createPaymentInfoForAliPay(paymentInfoMap);
        }
    }

    /**
     * 申请退款
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderNo, String reason) {
        try {
            log.info("支付宝调用退款API");

            //创建退款单
            RefundInfo refundInfo = refundInfoService.createRefundByOrderNoForAliPay(orderNo, reason);

            //调用统一收单交易退款接口
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();

            // 2选1
            // 设置商户订单号
            model.setOutTradeNo(orderNo);
            // 设置支付宝交易号
//            model.setTradeNo("2014112611001004680073956707");

            // 设置退款金额
            BigDecimal refund = new BigDecimal(refundInfo.getRefund().toString()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//           BigDecimal refund = new BigDecimal("2").divide(new BigDecimal("100"),2, RoundingMode.HALF_UP); 用来测试退款金额和订单金额不一致导致失败的情况
            model.setRefundAmount(String.valueOf(refund));
            // 设置退款原因说明(这是可选的，下面这行可注释)
            model.setRefundReason(reason);

            request.setBizModel(model);

            //执行请求，调用支付宝接口
            AlipayTradeRefundResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("支付宝申请退款调用成功，返回结果 ===> " + response.getBody());

                //更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);

                //更新退款单
                refundInfoService.updateRefundForAliPay(
                        refundInfo.getRefundNo(),
                        response.getBody(),
                        AliPayTradeState.REFUND_SUCCESS.getType()); //退款成功
            } else {
                log.info("支付宝申请退款调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());

                //更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_ABNORMAL);

                //更新退款单
                refundInfoService.updateRefundForAliPay(
                        refundInfo.getRefundNo(),
                        response.getBody(),
                        AliPayTradeState.REFUND_ERROR.getType()); //退款失败
            }
        } catch (AlipayApiException e) {
            throw new RuntimeException("支付宝申请退款失败", e);
        }
    }

    /**
     * 查询单笔退款
     */
    @Override
    public String queryRefund(String orderNo) {

        try {
            log.info("支付宝查询单笔退款接口调用 ===> {}", orderNo);

            // 构造请求参数以调用接口
            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();

            // 2选1
            // 设置支付宝交易号
            // model.setTradeNo("2021081722001419121412730660");
            // 设置商户订单号
            model.setOutTradeNo(orderNo);

            // 设置退款请求号(必填，如果在退款请求中没有设置就填商家订单号，如果设置了，这里也填一样的)
            model.setOutRequestNo(orderNo);
            request.setBizModel(model);

            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝查询单笔退款调用成功，返回结果 ===> " + response.getBody());
                return response.getBody();
            } else {
                log.info("支付宝查询单笔退款调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                //throw new RuntimeException("查单接口的调用失败");
                return null;//订单不存在
            }
        } catch (AlipayApiException e) {
            throw new RuntimeException("查单接口的调用失败", e);
        }
    }

    /**
     * 查询对账单,不能选则当天日期否则报错
     */
    @Override
    public String queryBill(String billDate, String type) {
        try {
            AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();
            AlipayDataDataserviceBillDownloadurlQueryModel model = new AlipayDataDataserviceBillDownloadurlQueryModel();

            // 类型包含如下2个常用枚举
            // 1、商户基于支付宝交易收单的业务账单: trade
            // 2、基于商户支付宝余额收入及支出等资金变动的账务账单: signcustomer
            model.setBillType(type);
            model.setBillDate(billDate);
            request.setBizModel(model);

            AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝获取对账单url调用成功，返回结果 ===> " + response.getBody());

                //获取账单下载地址
//                JSONObject jsonObject = JSON.parseObject(response.getBody(), JSONReader.Feature.SupportSmartMatch);
//                JSONObject res = jsonObject.getObject("alipay_data_dataservice_bill_downloadurl_query_response", JSONObject.class);
//                return res.getString("bill_download_url");
                return response.getBillDownloadUrl();
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                throw new RuntimeException("支付宝获取对账单url调用失败");
            }

        } catch (AlipayApiException e) {
            throw new RuntimeException("支付宝获取对账单url调用失败", e);
        }
    }
}
