package com.mall.controller.backend;

import com.mall.common.Const;
import com.mall.common.ServerResponse;
import com.mall.pojo.User;
import com.mall.service.IUserService;
import com.mall.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.jws.soap.SOAPBinding;
import javax.servlet.http.HttpSession;

/**
 * 管理员模块：
 * Created by cq on 2017/11/2.
 */
@Controller
@RequestMapping("/manage/user")
public class UserManageController {

    @Autowired
    private IUserService iUserService;

    /**
     * 登录
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value = "login.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpSession session) {
        //复用普通用户用过的Login方法
        ServerResponse<User> response = iUserService.login(username, password);
        if(response.isSuccess()) {
            //从登录信息总获取user对象
            User user = response.getData();
            if(user.getRole() == Const.Role.ROLE_ADMIN) {
                //说明登录的是管理员
                session.setAttribute(Const.CURRENT_USER, user);
                //新增redis共享cookie，session的方式
           //     CookieUtil.writeLoginToken(httpServletResponse,session.getId());
            //    RedisShardedPoolUtil.setEx(session.getId(), JsonUtil.obj2String(response.getData()),Const.RedisCacheExtime.REDIS_SESSION_EXTIME);

                return response;
            }
            else {
                return ServerResponse.createByErrorMessage("不是管理员，无法登录");
            }
        }
        return response;
    }
}
