package com.mall.controller.portal;

import com.github.pagehelper.PageInfo;
import com.mall.common.Const;
import com.mall.common.ResponseCode;
import com.mall.common.ServerResponse;
import com.mall.pojo.Shipping;
import com.mall.pojo.User;
import com.mall.service.IShippingService;
import com.mall.util.CookieUtil;
import com.mall.util.JsonUtil;
import com.mall.util.RedisPoolUtil;
import com.mall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by cq on 2017/11/7.
 */
@Controller
@RequestMapping("/shipping/")
public class ShippingController {

    @Autowired
    private IShippingService iShippingService;

    /**
     * 增加收货地址
     * @param shipping 收货地址对象
     * @return
     */
    @RequestMapping(value = "add.do", method = RequestMethod.POST)
    @ResponseBody
    //这里直接传shipping对象，这是SpringMVC数据绑定的对象数据绑定方式，与直接传对象中的属性是一样的效果
    //SpringMVC会自动生成shiping对象，并把对应的属性赋值上，否则就要一一写上属性
    //在前端传入的时候还是以属性的形式传入，只是这里SpringMVC可以自动的做转换
    public ServerResponse add(HttpServletRequest request, Shipping shipping) {
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
        return iShippingService.add(user.getId(), shipping);
    }

    /**
     * 删除收货地址
     * @param shippingId
     * @return
     */
    @RequestMapping(value = "del.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse del(HttpServletRequest request, Integer shippingId) {
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
        return iShippingService.del(user.getId(), shippingId);
    }

    /**
     * 更新收货地址
     * @param shipping
     * @return
     */
    @RequestMapping(value = "update.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse update(HttpServletRequest request, Shipping shipping) {
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
        return iShippingService.update(user.getId(), shipping);
    }

    /**
     * 查看收货地址详情
     * @param shippingId
     * @return
     */
    @RequestMapping(value = "select.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse select(HttpServletRequest request, Integer shippingId) {
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
        return iShippingService.select(user.getId(), shippingId);
    }

    /**
     * 分页查看收货地址
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "list.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<PageInfo> list(@RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                         @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                         HttpServletRequest request) {
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
        return iShippingService.list(user.getId(), pageNum, pageSize);
    }

}
