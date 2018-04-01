package com.mall.controller.common;

import com.mall.common.Const;
import com.mall.pojo.User;
import com.mall.util.CookieUtil;
import com.mall.util.JsonUtil;
import com.mall.util.RedisPoolUtil;
import com.mall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by cq on 2018/3/21.
 */
public class SessionExpireFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isNotEmpty(loginToken)) {
            //判断loginToken是否为空或者""
            //如果不为空的话，符合条件，继续拿user信息
            String userJsonStr = RedisShardedPoolUtil.get(loginToken);
            User user = JsonUtil.string2Obj(userJsonStr, User.class);
            if(user != null) {
                //如果user不为空，则重置session的时间，即调用expire命令
                RedisShardedPoolUtil.expire(loginToken, Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
