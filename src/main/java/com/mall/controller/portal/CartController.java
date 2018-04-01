package com.mall.controller.portal;

import com.mall.common.Const;
import com.mall.common.ResponseCode;
import com.mall.common.ServerResponse;
import com.mall.pojo.User;
import com.mall.service.ICartService;
import com.mall.util.CookieUtil;
import com.mall.util.JsonUtil;
import com.mall.util.RedisPoolUtil;
import com.mall.util.RedisShardedPoolUtil;
import com.mall.vo.CartVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by cq on 2017/11/6.
 */
@Controller
@RequestMapping("/cart/")
public class CartController {

    @Autowired
    private ICartService iCartService;

    /**
     * 添加购物车
     * @param count 商品数量
     * @param productId 商品id
     * @return
     */
    @RequestMapping(value = "add.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<CartVo> add(HttpServletRequest request, Integer count, Integer productId) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iCartService.add(user.getId(), productId, count);
    }

    /**
     * 更新购物车某个商品的数量
     * @param count
     * @param productId
     * @return
     */
    @RequestMapping(value = "update.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<CartVo> update(HttpServletRequest request, Integer count, Integer productId) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iCartService.update(user.getId(), productId, count);
    }

    /**
     * 从购物车中删除商品
     * @param productIds 可能删除多个商品，则将多个id都存入string中，用,逗号分割id
     * @return
     */
    @RequestMapping(value = "delete_product.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<CartVo> deleteProduct(HttpServletRequest request, String productIds) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iCartService.deleteProduct(user.getId(), productIds);
    }

    /**
     * 查看购物车商品
     * @return
     */
    @RequestMapping(value = "list.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<CartVo> list(HttpServletRequest request) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iCartService.list(user.getId());
    }

    /**
     * 全选
     * @return
     */
    @RequestMapping(value = "select_all.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<CartVo> selectAll(HttpServletRequest request) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        //如果传入的productId是空的话，会把当前用户的购物车中的所有商品都置成Const.Cart.CHECKED状态
        return iCartService.selectOrUnSlect(user.getId(), null, Const.Cart.CHECKED);
    }

    /**
     * 全反选
     * @return
     */
    @RequestMapping(value = "un_select_all.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<CartVo> unSelectAll(HttpServletRequest request) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        //如果传入的productId是空的话，会把当前用户的购物车中的所有商品都置成Const.Cart.UN_CHECKED状态
        return iCartService.selectOrUnSlect(user.getId(), null, Const.Cart.UN_CHECKED);
    }

    /**
     * 单选
     * @param productId 要选中的productId
     * @return
     */
    @RequestMapping(value = "select.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<CartVo> select(HttpServletRequest request, Integer productId) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iCartService.selectOrUnSlect(user.getId(), productId, Const.Cart.CHECKED);
    }

    /**
     * 单反选
     * @param productId 要反选的productId
     * @return
     */
    @RequestMapping(value = "un_select.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<CartVo> unSelect(HttpServletRequest request, Integer productId) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iCartService.selectOrUnSlect(user.getId(), productId, Const.Cart.UN_CHECKED);
    }

    /**
     * 获取用户购物车中商品数量
     * @return
     */
    @RequestMapping(value = "get_cart_product_count.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<Integer> getCartProductCount(HttpServletRequest request) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        //如果用户未登录这里返回0值，不返回错误信息给前端
        if(user == null) {
            return ServerResponse.createBySuccess(0);
        }
        return iCartService.getCartProductCount(user.getId());
    }



}
