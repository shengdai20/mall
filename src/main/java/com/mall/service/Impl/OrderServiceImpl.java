package com.mall.service.Impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mall.common.Const;
import com.mall.common.ServerResponse;
import com.mall.dao.*;
import com.mall.pojo.*;
import com.mall.service.IOrderService;
import com.mall.util.BigDecimalUtil;
import com.mall.util.DateTimeUtil;
import com.mall.util.FTPUtil;
import com.mall.util.PropertiesUtil;
import com.mall.vo.OrderItemVo;
import com.mall.vo.OrderProductVo;
import com.mall.vo.OrderVo;
import com.mall.vo.ShippingVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.omg.CORBA.Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by cq on 2018/1/23.
 */
@Service("iOrderService")
@Transactional
public class OrderServiceImpl implements IOrderService{

    private static AlipayTradeService tradeService;
    static {

        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         *  这里默认会将字符集设为utf-8，以便后来验签的时候使用
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
    }

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;

    /**
     * 创建订单
     * @param userId
     * @param shippingId
     * @return
     */
    public ServerResponse createOrder(Integer userId, Integer shippingId) {
        //从购物车中获取数据
        //将购物车中已勾选的物品获取出来，这些物品会用来生成订单
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);

        //根据这些物品，即cartList，生成订单详情List
        ServerResponse serverResponse = this.getCartOrderItem(userId, cartList);
        if(!serverResponse.isSuccess()) {
            return serverResponse;
        }
        List<OrderItem> orderItemList = (List<OrderItem>)serverResponse.getData();
        //根据订单详情list，计算订单总价
        BigDecimal payment = this.getOrderTotalPrice(orderItemList);

        //生成订单
        Order order = this.assembleOrder(userId, shippingId, payment);
        if(order == null) {
            return ServerResponse.createByErrorMessage("生成订单错误");
        }
        if(CollectionUtils.isEmpty(orderItemList)) {
            return ServerResponse.createByErrorMessage("购物车为空");
        }
        //将订单号逐一的更新到当前订单详情中，也就是一条订单数据可以对应多条订单详情
        for(OrderItem orderItem : orderItemList) {
            orderItem.setOrderNo(order.getOrderNo());
        }
        //mybatis批量插入
        //由于订单详情，一次性可能有多个商品，多个订单详情，也就是多条数据，所以这里就要使用一次性批量插入
        orderItemMapper.batchInsert(orderItemList);
        //生成成功，减少产品的库存
        this.reduceProductStock(orderItemList);

        //清空购物车
        this.cleanCart(cartList);

        //将订单明细返回给前端，返回给前端
        //pojo->vo
        //组装vo，传给前端的数据
        OrderVo orderVo = assembleOrderVo(order, orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }

    /**
     * 将订单、订单详情、收货地址详情转为vo对象，返回给前端
     * @param order 订单
     * @param orderItemList 订单详情list
     * @return
     */
    private OrderVo assembleOrderVo(Order order, List<OrderItem> orderItemList) {
        OrderVo orderVo = new OrderVo();
        //订单号
        orderVo.setOrderNo(order.getOrderNo());
        //订单总价
        orderVo.setPayment(order.getPayment());
        //订单支付类型
        orderVo.setPaymentType(order.getPaymentType());
        //支付类型描述
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());
        //运费
        orderVo.setPostage(order.getPostage());
        //订单状态
        orderVo.setStatus(order.getStatus());
        //订单状态描述
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());
        //收货地址id
        orderVo.setShippingId(order.getShippingId());
        //根据收货地址id，从收货地址表中拿到当前地址
        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
        if(shipping != null){
            //收货姓名
            orderVo.setReceiverName(shipping.getReceiverName());
            //收货地址明细
            orderVo.setShippingVo(assembleShippingVo(shipping));
        }
        //支付时间
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        //发货时间
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        //结束时间
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        //创建时间
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
        //关闭时间
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));
        //订单图片地址
        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        //逐一遍历订单详情list
        for(OrderItem orderItem : orderItemList){
            //将订单详情转成vo对象
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            //将vo对象，放在voList中存储
            orderItemVoList.add(orderItemVo);
        }
        //将订单详情voList存入订单vo对象
        orderVo.setOrderItemVoList(orderItemVoList);
        return orderVo;
    }

    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());

        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));
        return orderItemVo;
    }

    /**
     * 将收货地址明细转成vo对象，返回给前端
     * @param shipping
     * @return
     */
    private ShippingVo assembleShippingVo(Shipping shipping) {
        ShippingVo shippingVo = new ShippingVo();
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        shippingVo.setReceiverPhone(shippingVo.getReceiverPhone());
        return shippingVo;
    }

    /**
     * 清空购物车
     * @param cartList 购物车list
     */
    private void cleanCart(List<Cart> cartList) {
        for(Cart cart : cartList) {
            //更新cart购物车表
            cartMapper.deleteByPrimaryKey(cart.getId());
        }
    }

    /**
     * 减少库存
     * @param orderItemList 订单详情list
     */
    private void reduceProductStock(List<OrderItem> orderItemList) {
        //根据订单详情list，逐一针对商品，去product表中减库存
        for(OrderItem orderItem : orderItemList) {
            //根据productId,从product表中拿到当前商品
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
            //减库存
            product.setStock(product.getStock() - orderItem.getQuantity());
            //将库存信息更新到product表中
            productMapper.updateByPrimaryKeySelective(product);
        }
    }

    /**
     * 生成订单
     * @param userId 用户id
     * @param shippingId 收货地址id
     * @param payment 订单总价
     * @return
     */
    private Order assembleOrder(Integer userId, Integer shippingId, BigDecimal payment) {
        Order order = new Order();
        //生成订单号orderNo，很重要
        long orderNo = this.generateOrderNo();
        //设置订单号
        order.setOrderNo(orderNo);
        //设置订单状态
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
        //设置运费
        order.setPostage(0);
        //设置支付类型
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
        //设置订单总价
        order.setPayment(payment);
        order.setUserId(userId);
        order.setShippingId(shippingId);

        //将这个订单持久化到数据库中
        int rowCount = orderMapper.insert(order);
        if(rowCount > 0) {
            return order;
        }
        return null;
    }

    /**
     * 生成订单号
     * @return
     */
    private long generateOrderNo() {
        //用时间戳
        // TODO: 2018/3/10
        long currentTime = System.currentTimeMillis();
        return currentTime + new Random().nextInt(100);
    }

    /**
     * 根据订单详情List,计算当前订单总价
     * @param orderItemList
     * @return
     */
    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList) {
        BigDecimal payment = new BigDecimal("0");
        //计算当前订单总计，逐一相加
        for(OrderItem orderItem : orderItemList) {
            payment = BigDecimalUtil.add(payment.doubleValue(), orderItem.getTotalPrice().doubleValue());
        }
        return payment;
    }

    /**
     * 根据当前购物车list，生成订单详情
     * @param userId
     * @param cartList
     * @return
     */
    private ServerResponse getCartOrderItem(Integer userId,List<Cart> cartList){
        List<OrderItem> orderItemList = Lists.newArrayList();
        if(CollectionUtils.isEmpty(cartList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        //校验购物车的数据,包括产品的状态和数量
        for(Cart cartItem : cartList){
            OrderItem orderItem = new OrderItem();
            //从购物车中获取productId，然后从product表中获取当前产品的详细信息
            Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
            //查看当前产品是否在售
            if(Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()){
                return ServerResponse.createByErrorMessage("产品"+product.getName()+"不是在线售卖状态");
            }

            //校验库存，校验当前产品选购的数量是否超过产品本身库存
            if(cartItem.getQuantity() > product.getStock()){
                return ServerResponse.createByErrorMessage("产品"+product.getName()+"库存不足");
            }

            //组装订单详情，一种商品，一个订单详情，而订单详情又由orderNo唯一标识，所以可能多种商品有同样的orderNo，而则会个orderNo又对应订单order中的一个订单，也由orderNo唯一标识

            orderItem.setUserId(userId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            // 这里将当前商品的总价：单价*数量，存进数据表
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartItem.getQuantity()));
            //将当前订单详情加入订单详情List中
            orderItemList.add(orderItem);
        }
        //将订单详情list返回
        return ServerResponse.createBySuccess(orderItemList);
    }

    /**
     * 取消订单
     * @param userId
     * @param orderNo 订单号
     * @return
     */
    public ServerResponse<String> cancel(Integer userId, Long orderNo) {
        //根据userId和orderNo拿到订单
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if(order == null) {
            return ServerResponse.createByErrorMessage("该用户此订单不存在");
        }
        //判断订单状态
        if(order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()) {
            return ServerResponse.createByErrorMessage("已付款，无法取消订单");
        }
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        //更新订单状态
        updateOrder.setStatus(Const.OrderStatusEnum.CANCELED.getCode());
        //将更新状态更新到数据库中
        int row = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if(row > 0) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

    /**
     * 获取购物车中已经选中的商品详情
     * @param userId
     * @return
     */
    public ServerResponse getOrderCartProduct(Integer userId) {
        OrderProductVo orderProductVo = new OrderProductVo();
        //从购物车中获取数据
        //根据userId，从购物车表中，将当前用户选中的商品拿出来
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        //根据选中的商品list，得到订单详情
        ServerResponse serverResponse =  this.getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<OrderItem> orderItemList =( List<OrderItem> ) serverResponse.getData();

        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        //从订单详情list中，计算订单总价
        BigDecimal payment = new BigDecimal("0");
        for(OrderItem orderItem : orderItemList){
            //计算订单总价
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
            //将订单详情，组装成订单详情vo对象，放在voList中
            orderItemVoList.add(assembleOrderItemVo(orderItem));
        }
        //将订单总价放在orderProductVo中
        orderProductVo.setProductTotalPrice(payment);
        //将订单详情voList放在orderProductVo中
        orderProductVo.setOrderItemVoList(orderItemVoList);
        //将图片服务器地址放在orderProductVo中
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        //将orderProductVo返回
        return ServerResponse.createBySuccess(orderProductVo);
    }

    /**
     * 前台获取订单详情
     * @param userId
     * @param orderNo 订单号
     * @return
     */
    public ServerResponse<OrderVo> getOrderDetail(Integer userId,Long orderNo) {
        //根据userId，orderNo从订单表中拿到当前订单
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order != null){
            //从订单详情表中，根据userId和orderNo拿到当前订单的所有详情
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo,userId);
            //根据order和订单详情，组装订单vo对象，返回给前台
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return  ServerResponse.createByErrorMessage("没有找到该订单");
    }

    /**
     * 前台查看订单
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        //根据userId，从order表中获取到当前用户所有的订单，放在list中
        List<Order> orderList = orderMapper.selectByUserId(userId);
        //根据订单list和userId,转成voList对象
        List<OrderVo> orderVoList = assembleOrderVoList(orderList,userId);
        //对list对象进行分页处理
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }

    /**
     * 根据订单list和userId，转成voList对象
     * @param orderList
     * @param userId
     * @return
     */
    private List<OrderVo> assembleOrderVoList(List<Order> orderList,Integer userId){
        List<OrderVo> orderVoList = Lists.newArrayList();
        for(Order order : orderList){
            List<OrderItem>  orderItemList = Lists.newArrayList();
            if(userId == null){
                //管理员查询的时候 不需要传userId
                orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
            }else{
                //获取当前订单的明细
                System.out.println(order.getOrderNo());
                orderItemList = orderItemMapper.getByOrderNoUserId(order.getOrderNo(),userId);
            }
            //根据订单和订单明细，转成vo对象
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }

    /**
     * 支付订单
     * @param orderNo 要支付的订单
     * @param userId 用户id
     * @param path 生成二维码传到的路径
     * @return 将订单号和二维码的url一起返回给前端
     */
    public ServerResponse pay(Long orderNo, Integer userId, String path) {
        Map<String, String> resultMap = Maps.newHashMap();
        //从数据库中根据userId和orderNo查看当前订单是否存在
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if(order == null) {
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        //如果订单存在，将orderNo存入map中
        resultMap.put("orderNo", String.valueOf(order.getOrderNo()));

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        //扫码之后就能看到
        String subject = new StringBuilder().append("happymall扫码支付,订单号:").append(outTradeNo).toString();

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();


        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";



        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();


        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
        //根据用户id和orderNo拿到订单中的所有商品
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo,userId);
        for(OrderItem orderItem : orderItemList){
            //从所有商品orderItemList中拿出每个商品的明细，创建每个商品的实例
            GoodsDetail goods = GoodsDetail.newInstance(orderItem.getProductId().toString(), orderItem.getProductName(),
                    BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(),new Double(100).doubleValue()).longValue(),
                    orderItem.getQuantity());
            //将商品放入商品明细列表中
            goodsDetailList.add(goods);
        }

        // 创建扫码支付请求builder，设置请求参数
        //setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))，是设置授权回调地址，从沙箱环境中获取，这个回调地址也是由natapp生成的
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);

        //tradeService在static块中初始化
        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                logger.info("支付宝预下单成功: )");

                //支付宝回调放在response中
                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                //判断path路径的目录文件是否存在，如果不存在则创建
                File folder = new File(path);
                if (!folder.exists()) {
                    folder.setWritable(true);
                    folder.mkdirs();
                }

                // 需要修改为运行机器上的路径
                //细节细节细节
                //根据外部订单号生成二维码的路径，也就是存放二维码的路径，这里跑酷哦path和qrFileName
                String qrPath = String.format(path + "\\qr-%s.png", response.getOutTradeNo());
                //二维码文件名
                String qrFileName = String.format("qr-%s.png", response.getOutTradeNo());
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);
                //目标文件的path，目标文件的文件名
                File targetFile = new File(path, qrFileName);
                try {
                    //将二维码上传到ftp服务器
                    FTPUtil.uploadFile(Lists.newArrayList(targetFile));
                } catch (IOException e) {
                    logger.error("上传二维码异常", e);
                }
                logger.info("qrPath:" + qrPath);
         //       System.out.println("路径：" + qrPath + ",,,名字：" + targetFile.getName());

                //拿到二维码的url，将其返回
                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFile.getName();
                resultMap.put("qrUrl", qrUrl);
                return ServerResponse.createBySuccess(resultMap);
            case FAILED:
                logger.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

            case UNKNOWN:
                logger.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

            default:
                logger.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }
    }
    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            logger.info("body:" + response.getBody());
        }
    }

    /**
     * 验证支付宝回调的数据是否正确
     * @param params 回调的各种参数
     * @return
     */
    public ServerResponse aliCallback(Map<String, String> params) {
        //支付宝的外部订单号，是商城的内部订单号
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        //支付宝的交易号
        String tradeNo = params.get("trade_no");
        //交易状态
        String tradeStatus = params.get("trade_status");
        //去数据库查看，当前外部订单号是否存在于数据库中
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null) {
            return ServerResponse.createByErrorMessage("非正常订单，回调忽略");
        }
        //查看订单状态
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
            return ServerResponse.createBySuccess("支付宝重复调用");
        }
        //查看交易状态
        if(Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)) {
            //更新付款时间，从params获取到
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
            //如果交易状态是支付成功，则将order状态设置成已付款
            order.setStatus(Const.OrderStatusEnum.PAID.getCode());
            //将order状态更新到数据库中
            orderMapper.updateByPrimaryKeySelective(order);
        }
        //将支付成功后的信息存到数据库中
        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setPlatformNumber(tradeNo);
        payInfo.setPlatformStatus(tradeStatus);

        payInfoMapper.insert(payInfo);

        return ServerResponse.createBySuccess();
    }

    public ServerResponse queryOrderPayStatus(Integer userId,Long orderNo){
        //根据userId和orderNo从数据库中拿到order订单详情
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        //查看order订单状态，是否已支付
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

    /**
     * 后台的获取所有订单
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse<PageInfo> manageList(int pageNum,int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        //获取到所有订单
        List<Order> orderList = orderMapper.selectAllOrder();
        //根据订单list，转成voList，这里共用了前台的订单list转voList的方法，所以这里要传一个null过去，表示是管理员
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList,null);
        //对voList进行分页处理
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }

    /**
     * 后台的获取订单详情
     * @param orderNo
     * @return
     */
    public ServerResponse<OrderVo> manageDetail(Long orderNo) {
        //根据订单号获取到订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null) {
            //将当前订单的所有订单详情获取出来
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            //根据order和orderItemList组装orderVo
            OrderVo orderVo = assembleOrderVo(order, orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    /**
     * 后台的按照订单号搜索订单
     * @param orderNo
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse<PageInfo> manageSearch(Long orderNo, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        //根据订单号获取到当前订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null) {
            //获取到当前订单的所有详情
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            //转成vo
            OrderVo orderVo = assembleOrderVo(order, orderItemList);
            //分页处理
            PageInfo pageResult = new PageInfo(Lists.newArrayList(order));
            pageResult.setList(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccess(pageResult);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    /**
     * 发货
     * @param orderNo
     * @return
     */
    public ServerResponse<String> manageSendGoods(Long orderNo) {
        //根据订单号获取订单
        Order order= orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            //如果已付款，更新订单状态为已发货
            if(order.getStatus() == Const.OrderStatusEnum.PAID.getCode()){
                order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
                //更新发乎哦时间
                order.setSendTime(new Date());
                //持久化到数据库
                orderMapper.updateByPrimaryKeySelective(order);
                return ServerResponse.createBySuccess("发货成功");
            }
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }
}
