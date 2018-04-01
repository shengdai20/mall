package com.mall.controller.portal;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.google.common.collect.Maps;
import com.mall.common.Const;
import com.mall.common.ResponseCode;
import com.mall.common.ServerResponse;
import com.mall.pojo.User;
import com.mall.service.IOrderService;
import com.mall.util.CookieUtil;
import com.mall.util.JsonUtil;
import com.mall.util.RedisPoolUtil;
import com.mall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by cq on 2018/1/23.
 */
@Controller
@RequestMapping("/order/")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private IOrderService iOrderService;

    /**
     * 创建订单
     * @param shippingId 收货地址id
     * @return
     */
    @RequestMapping("create.do")
    @ResponseBody
    public ServerResponse create(HttpServletRequest request, Integer shippingId) {
    //    User user = (User)session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        //将userId和shippingId传入，创建订单
        return iOrderService.createOrder(user.getId(), shippingId);
    }

    /**
     * 取消订单
     * @param orderNo 订单号
     * @return
     */
    @RequestMapping("cancel.do")
    @ResponseBody
    public ServerResponse cancel(HttpServletRequest request , Long orderNo) {
    //    User user = (User)session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.cancel(user.getId(), orderNo);
    }

    /**
     * 获取购物车中已经选中的商品详情
     * @return
     */
    @RequestMapping("get_order_cart_product.do")
    @ResponseBody
    public ServerResponse getOrderCartProduct(HttpServletRequest request) {
        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

 //       User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderCartProduct(user.getId());
    }

    /**
     * 前台的获取订单详情
     * @param orderNo 订单号
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse detail(HttpServletRequest request, Long orderNo) {
    //    User user = (User)session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderDetail(user.getId(), orderNo);
    }

    /**
     * 前台查看订单list
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse list(HttpServletRequest request, @RequestParam(value = "pageNum", defaultValue = "1") int pageNum, @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
    //    User user = (User)session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderList(user.getId(), pageNum, pageSize);
    }

    /**
     * 支付订单
     * @param orderNo 要支付的订单号
     * @param request 获取servlet上下文，拿到upload文件夹，然后将自动生成的二维码传到ftp服务器上，然后前端对二维码进行展示
     * @return
     */
    @RequestMapping("pay.do")
    @ResponseBody
    public ServerResponse pay(HttpServletRequest httpServletRequest , Long orderNo, HttpServletRequest request) {
    //    User user = (User)session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        String path = request.getSession().getServletContext().getRealPath("upload");
        return iOrderService.pay(orderNo,user.getId(),path);
    }

    /**
     * 支付宝回调
     * @param request 支付宝的回调全部放在request中
     * @return
     */
    @RequestMapping("alipay_callback.do")
    @ResponseBody
    public Object alipayCallback(HttpServletRequest request){
        Map<String,String> params = Maps.newHashMap();
        //从request中获取所有参数
        Map requestParams = request.getParameterMap();
        //遍历map参数，key是string，value是string数组
        for(Iterator iter = requestParams.keySet().iterator();iter.hasNext();){
            String name = (String)iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            //将value数组拼接成一个string字符串，用,分割
            for(int i = 0 ; i <values.length;i++){
                valueStr = (i == values.length -1)?valueStr + values[i]:valueStr + values[i]+",";
            }
            //定义一个 Map<String,String>重新存储
            params.put(name,valueStr);
        }
        //sign签名，trade_status交易状态，直接从刚才的params中获取
        logger.info("支付宝回调,sign:{},trade_status:{},参数:{}",params.get("sign"),params.get("trade_status"),params.toString());

        //非常重要,验证回调的正确性,是不是支付宝发的.并且还要避免重复通知.

        //源码里没有出去"sign_type"参数，所以这里我们要手动除掉
        params.remove("sign_type");
        try {
            //验证回调的正确性
            /*
            第一步： 在通知返回参数列表中，除去sign、sign_type两个参数外，凡是通知返回回来的参数皆是待验签的参数。
            第二步： 将剩下参数进行url_decode, 然后进行字典排序，组成字符串，得到待签名字符串：
            第三步： 将签名参数（sign）使用base64解码为字节码串。
            第四步： 使用RSA的验签方法，通过签名字符串、签名参数（经过base64解码）及支付宝公钥验证签名。
            第五步：需要严格按照如下描述校验通知数据的正确性。

             */
            //rsaCheckV2调用getSignCheckContentV2，将"sign"参数除去，并使用StringBuffer（线程安全）,
            //然后将参数拿出来放到list中，并按字典顺序进行排序，将所有参数放到content字符串（以&分割）中返回
            //将"sign"参数使用base64解码
            //rsaCheckV2有多种重载方法，应该使用带有sign_type的重载方法
            //这里的publicKey是支付宝公钥，不是应用公钥

            boolean alipayRSACheckedV2 = AlipaySignature.rsaCheckV2(params, Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());

            if(!alipayRSACheckedV2){
                return ServerResponse.createByErrorMessage("非法请求,验证不通过,再恶意请求我就报警找网警了");
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝验证回调异常",e);
        }

        /*
         商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，并判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
         同时需要校验通知中的seller_id（或者seller_email) 是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email），
         上述有任何一个验证不通过，则表明本次通知是异常通知，务必忽略。在上述验证通过后商户必须根据支付宝不同类型的业务通知，正确的进行不同的业务处理，并且过滤重复的通知结果数据。
         在支付宝的业务通知中，只有交易通知状态为TRADE_SUCCESS或TRADE_FINISHED时，支付宝才会认定为买家付款成功。
         */
        //验证各种数据
        ServerResponse serverResponse = iOrderService.aliCallback(params);
        if(serverResponse.isSuccess()){
            return Const.AlipayCallback.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallback.RESPONSE_FAILED;
    }

    /**
     * 前端轮询查询订单的交易状态，也就是当扫完二维码后，付完款，会给个指引，跳到支付订单详情页
     * @param orderNo
     * @return
     */
    @RequestMapping("query_order_pay_status.do")
    @ResponseBody
    public ServerResponse<Boolean> queryOrderPayStatus(HttpServletRequest request, Long orderNo){
     //   User user = (User)session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }

        //根据userId和orderNo查询订单状态，如果已支付，则将true返回给前端（然后跳转到支付订单详情页），否则返回false
        ServerResponse serverResponse = iOrderService.queryOrderPayStatus(user.getId(),orderNo);
        if(serverResponse.isSuccess()){
            return ServerResponse.createBySuccess(true);
        }
        return ServerResponse.createBySuccess(false);
    }
}
