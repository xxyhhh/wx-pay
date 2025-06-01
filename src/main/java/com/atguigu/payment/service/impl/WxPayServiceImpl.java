package com.atguigu.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.payment.config.WxPayConfig;
import com.atguigu.payment.entity.OrderInfo;
import com.atguigu.payment.entity.RefundInfo;
import com.atguigu.payment.enums.OrderStatus;
import com.atguigu.payment.enums.PayType;
import com.atguigu.payment.enums.wxpay.WxNotifyType;
import com.atguigu.payment.enums.wxpay.WxRefundStatus;
import com.atguigu.payment.enums.wxpay.WxTradeState;
import com.atguigu.payment.service.OrderInfoService;
import com.atguigu.payment.service.PaymentInfoService;
import com.atguigu.payment.service.RefundInfoService;
import com.atguigu.payment.service.WxPayService;
import com.atguigu.payment.util.HttpUtils;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.HttpException;
import com.wechat.pay.java.core.exception.MalformedMessageException;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.http.DefaultHttpClientBuilder;
import com.wechat.pay.java.core.http.HttpClient;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.core.util.IOUtil;
import com.wechat.pay.java.service.billdownload.BillDownloadService;
import com.wechat.pay.java.service.billdownload.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.*;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.wechat.pay.java.service.refund.model.CreateRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    @Autowired
    private PaymentInfoService paymentInfoService;
    @Autowired
    private RefundInfoService refundInfoService;

    /**
     * 发起支付请求，调用统一下单api,返回code_url,生成支付二维码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> nativePay(Long productId) {
        try {
            //生成订单
            OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId, PayType.WXPAY.getType());
            String codeUrl = orderInfo.getCodeUrl();
            if (StringUtils.hasText(codeUrl)) {
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
            //request.setAttach(); 商户在统一下单时传入的附加数据，将来在回调的transaction中获取
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
        } catch (Exception e) {
            log.info("微信发起支付请求，调用统一下单api失败",e);
            throw new RuntimeException();
        }
    }

    /**
     * 微信支付成功后，进行的回调
     */
    @Override
    public void wxnotify(HttpServletRequest request) {
        // 1.回调通知的验签与解密
        String wechatPaySerial = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String signature = request.getHeader("Wechatpay-Signature");
        String requestBody = HttpUtils.readData(request);

        //查看微信发过来的信息
        Map<String, Object> map = JSON.parseObject(requestBody);
        log.info("支付回调信息：{}", map);

        //2.构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(requestBody)
                .build();

        //3.初始化 NotificationParser
        NotificationParser parser = new NotificationParser(config);
        //4.以支付通知回调为例，验签、(使用apiv3)解密并转换成 Transaction
        Transaction transaction = parser.parse(requestParam, Transaction.class);

        // 判断状态是否支付成功
        if (null != transaction && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
            //5.处理支付业务
            this.handlePayment(transaction);
        }
    }

    /**
     * 退款状态改变后，微信回调该方法
     */
    @Override
    public void wxRefundsNotify(HttpServletRequest request) {
        // 1.回调通知的验签与解密
        String wechatPaySerial = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String signature = request.getHeader("Wechatpay-Signature");
        String requestBody = HttpUtils.readData(request);

        //查看微信发过来的信息
        Map<String, Object> map = JSON.parseObject(requestBody);
        log.info("退款回调信息：{}", map);

        //2.构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(requestBody)
                .build();

        //3.初始化 NotificationParser
        NotificationParser parser = new NotificationParser(config);
        //4.验签、(使用apiv3)解密并转换成 refundNotification
        RefundNotification refundNotification = parser.parse(requestParam, RefundNotification.class);

        // 判断状态是否已退款
        if (null != refundNotification && "SUCCESS".equals(refundNotification.getRefundStatus().name())) {
            //5.处理退款业务
            this.handleRefund(refundNotification); // 使用退款对象处理业务逻辑
        }
    }

    /**
     * 用户取消订单
     */
    @Override
    public void cancelOrder(String orderNo) {
        //调用微信支付的关单接口
        closeOrder(orderNo);
        //更新商户端的订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);
    }

    /**
     * 通过商户订单号手动查询订单信息
     */
    @Override
    public Transaction queryOrder(String orderNo) {
        log.info("查询订单接口调用 ===> {}", orderNo);
        //1、创建微信支付使用对象
        NativePayService service = new NativePayService.Builder().config(config).build();

        //2、封装查询支付状态需要参数
        QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
        queryRequest.setMchid(wxPayConfig.getMchId());
        queryRequest.setOutTradeNo(orderNo);

        Transaction result = null;
        try {
            result = service.queryOrderByOutTradeNo(queryRequest);
        } catch (ServiceException e) {
            // API返回失败, 例如ORDER_NOT_EXISTS
            log.error("API请求失败，错误码：{}, 错误信息：{}", e.getErrorCode(), e.getErrorMessage());
            log.error("API响应内容：{}", e.getResponseBody());
            return result;
        }
        return result;
    }

    /**
     * 根据订单号查询微信支付查单接口，核实订单状态
     * 如果订单已支付，则更新商户端订单状态,并记录支付日志
     * 如果订单未支付(在给定期限内未支付)，则调用关单接口关闭订单，并更新商户端订单状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkOrderStatus(String orderNo) {
        log.info("【微信支付】开始核实订单状态, 订单号: {}", orderNo);
        Transaction result = queryOrder(orderNo);
        
        if (result == null) {
            log.info("【微信支付】订单不存在, 订单号: {}", orderNo);
            return;
        }
        
        String state = result.getTradeState().name();
        if (WxTradeState.SUCCESS.getType().equals(state)) {
            log.info("【微信支付】订单已支付成功, 订单号: {}", orderNo);
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
            paymentInfoService.createPaymentInfo(result);
        } else if (WxTradeState.NOTPAY.getType().equals(state)) {
            log.info("【微信支付】订单未支付, 准备关闭订单, 订单号: {}", orderNo);
            closeOrder(orderNo);
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }
    }

    /**
     * 微信申请退款
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refunds(String orderNo, String reason) {
        log.info("【微信支付】开始申请退款, 订单号: {}, 退款原因: {}", orderNo, reason);
        //根据订单编号创建退款单
        RefundInfo refundInfo = refundInfoService.createRefundsByOrderNo(orderNo, reason);
        //调用退款api
        RefundService service = new RefundService.Builder().config(config).build();

        Refund response = null;
        try {
            CreateRequest request = getCreateRequest(refundInfo);

            response = service.create(request);
            log.info("【微信支付】退款申请成功, 订单号: {}, 退款单号: {}", orderNo, response.getOutRefundNo());
            //更新订单状态（退款中）
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_PROCESSING);
            //更新退款单(这次wx给的也是退款中的状态)
            refundInfoService.updateRefund1(response);

        } catch (HttpException e) { // 发送HTTP请求失败
            log.error("【微信支付】退款HTTP请求失败, 订单号: {}, 请求信息: {}", orderNo, e.getHttpRequest());
            throw new RuntimeException("退款请求失败", e);
        } catch (ServiceException e) { // 服务返回状态小于200或大于等于300，例如500
            log.error("【微信支付】退款服务异常, 订单号: {}, 响应信息: {}", orderNo, e.getResponseBody());
            throw new RuntimeException("退款服务异常", e);
        } catch (MalformedMessageException e) { // 服务返回成功，返回体类型不合法，或者解析返回体失败
            log.error("【微信支付】退款响应解析失败, 订单号: {}, 错误信息: {}", orderNo, e.getMessage());
            throw new RuntimeException("退款响应解析失败", e);
        }
    }

    /**
     * 根据(商户的)退款单号查询(官方的)退款单信息
     */
    @Override
    public Refund queryRefund(String refundNo) {
        //初始化服务
        RefundService service = new RefundService.Builder().config(config).build();

        QueryByOutRefundNoRequest request = new QueryByOutRefundNoRequest();
        request.setOutRefundNo(refundNo);
        // 调用接口
        return service.queryByOutRefundNo(request);
    }

    /**
     * 查询账单
     *
     * @param billDate 账单日期，只能查今天之前的日期且3个月内，格式必须是YYYY-MM-DD
     * @param type     账单类型
     */
    @Override
    public QueryBillEntity queryBill(String billDate, String type) {
        // 只允许 YYYY-MM-DD 格式
        if (billDate == null || !billDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("账单日期格式必须为 YYYY-MM-DD");
        }
        // 初始化 BillService
        BillDownloadService service = new BillDownloadService.Builder().config(config).build();

        if ("tradebill".equals(type)) {  // 申请交易账单API
            GetTradeBillRequest request = new GetTradeBillRequest();
            request.setBillDate(billDate);
            // request.setBillType(BillType.ALL);  默认值
            // request.setTarType(TarType.GZIP);   下载账单时返回.gzip格式的压缩文件流
            return service.getTradeBill(request);
        } else if ("fundflowbill".equals(type)) {   // 申请资金账单API
            GetFundFlowBillRequest request = new GetFundFlowBillRequest();
            request.setBillDate(billDate);
            // request.setTarType(TarType.GZIP);
            return service.getFundFlowBill(request);
        } else {
            throw new RuntimeException("不支持的账单类型");
        }
    }

    @Override
    public String downloadBill(String billDate, String type) {
        // Step 1: 获取账单信息（含下载链接、摘要算法、账单文件摘要后的结果）
        QueryBillEntity billEntity = queryBill(billDate, type);
        String downloadUrl = billEntity.getDownloadUrl();
        String hashType = billEntity.getHashType().name();
        String hashValue = billEntity.getHashValue();

        // Step 2: 创建 HttpClient 实例
        HttpClient httpClient = new DefaultHttpClientBuilder().config(config).build();

        // Step 3: 下载账单文件流
        try (InputStream inputStream = httpClient.download(downloadUrl)) {
            if (inputStream == null) {
                throw new RuntimeException("账单下载失败：输入流为空");
            }

            // Step 4: 读取账单内容（仅适用于小账单）
            String billContent = IOUtil.toString(inputStream);

            // Step 5: 验证账单完整性
            MessageDigest digest = MessageDigest.getInstance(hashType);  //拿到该算法的摘要器
            byte[] hashBytes = digest.digest(billContent.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String actualHash = hexString.toString().toLowerCase();
            String expectedHash = hashValue.toLowerCase();

            if (!actualHash.equals(expectedHash)) {
                throw new RuntimeException("账单完整性校验失败");
            }
            return billContent;
        } catch (Exception e) {
            throw new RuntimeException("【微信支付】下载或验证账单失败", e);
        }
    }

    /**
     * 根据退款单号核实退款单状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkRefundStatus(String refundNo) {
        log.info("【微信支付】开始核实退款单状态, 退款单号: {}", refundNo);

        //调用查询退款单接口
        Refund refund = this.queryRefund(refundNo);
        //获取微信支付端退款状态和商家订单号
        Status status = refund.getStatus();
        String outTradeNo = refund.getOutTradeNo();

        if (WxRefundStatus.SUCCESS.getType().equals(status.name())) {

            log.info("【微信支付】核实订单已退款成功 ===> {}", refundNo);
            //退款成功，则更新订单状态
            orderInfoService.updateStatusByOrderNo(outTradeNo, OrderStatus.REFUND_SUCCESS);
            //更新退款单
            refundInfoService.updateRefund1(refund);
        }

        if (WxRefundStatus.ABNORMAL.getType().equals(status.name())) {    // 退款异常

            log.info("【微信支付】核实订单退款异常  ===> {}", refundNo);
            //退款失败，则更新订单状态
            orderInfoService.updateStatusByOrderNo(outTradeNo, OrderStatus.REFUND_ABNORMAL);
            //更新退款单
            refundInfoService.updateRefund1(refund);
        }
    }

    private CreateRequest getCreateRequest(RefundInfo refundInfo) {
        CreateRequest request = new CreateRequest();
        // request.setTransactionId(xxx); 和商户订单号二选一即可
        request.setOutTradeNo(refundInfo.getOrderNo());
        request.setOutRefundNo(refundInfo.getRefundNo());
        request.setReason(refundInfo.getReason());
        // 退款回调
        request.setNotifyUrl(wxPayConfig.getNotifyDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));

        AmountReq amountReq = new AmountReq();
        amountReq.setTotal(Long.valueOf(refundInfo.getTotalFee()));  //原订单金额
        amountReq.setRefund(Long.valueOf(refundInfo.getRefund())); //退款金额
        amountReq.setCurrency("CNY"); //CNY：人民币 退款币种
        request.setAmount(amountReq);
        return request;
    }

    /**
     * 关闭订单接口调用
     */
    private void closeOrder(String orderNo) {
        CloseOrderRequest closeRequest = new CloseOrderRequest();
        closeRequest.setMchid(wxPayConfig.getMchId());
        closeRequest.setOutTradeNo(orderNo);  //商户订单号

        //创建微信支付使用对象
        NativePayService service = new NativePayService.Builder().config(config).build();
        // 方法没有返回值。成功时API返回204 No Content
        try {
            service.closeOrder(closeRequest);
        } catch (ValidationException e) {
            log.error("关闭订单请求参数验证失败: {},订单号: {}", e.getMessage(), orderNo);
            // 可以选择抛出自定义异常或标记为失败状态
        } catch (Exception e) {
            log.error("关闭订单发生未知错误, 订单号: {}", orderNo, e);
        }
    }

    //title:支付成功后的业务处理
    //1.1、这里先判断当前订单状态，主要是怕回调通知重复执行(如果你的服务没来得及返回/网络原因 "success" 给微信,出现超时)
    //1.2、如果重复执行，导致 mq(如多次扣库存)、或其它操作(如记录日志) 的话 会被重复执行
    @Transactional(rollbackFor = Exception.class)
    public void handlePayment(Transaction transaction) {
        //商户订单号
        String orderNo = transaction.getOutTradeNo();

        //查询该订单是否已经支付
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        if (OrderStatus.SUCCESS.name().equals(orderStatus)) {
            //如果已经支付，不需要更新（防止重复更新，保持了幂等性）
            return;
        }

        //这里可能有并发问题，比如有2个请求，前一个还没来得及改状态，另一个也认为是还未支付，那么就会更新2次订单状态
        //但是概率很低，因为wx的回调是有时间间隔的

        //更新订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

        //记录支付日志
        paymentInfoService.createPaymentInfo(transaction);
    }


    //title:退款成功后的业务处理
    //1.1、这里先判断当前退款单状态，主要是怕回调通知重复执行(如果你的服务没来得及返回/网络原因 "success" 给微信,出现超时)
    //1.2、如果重复执行，会对业务产生影响
    @Transactional(rollbackFor = Exception.class)
    public void handleRefund(RefundNotification refundNotification) {
        //商户订单号
        String orderNo = refundNotification.getOutTradeNo();

        //查询该订单是否正在退款中
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        if (!OrderStatus.REFUND_PROCESSING.getType().equals(orderStatus)) {
            //如果不是正在退款中，我也没必要改状态了
            return;
        }

        //这里可能有并发问题，比如有2个请求，前一个还没来得及改状态，另一个也认为是还未支付，那么就会更新2次订单状态
        //但是概率很低，因为wx的回调是有时间间隔的

        //更新订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);

        //更新退款单
        refundInfoService.updateRefund2(refundNotification);
    }
}


