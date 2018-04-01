package com.mall.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by cq on 2018/3/21.
 */
@Slf4j
public class CookieUtil {
    //将这个cookie放在一级域名下，则二级域名www.mall.com、user.mall.com等都可以访问到这个cookie，而同级域名是访问不到的
    private final static String COOKIE_DOMAIN = ".mall.com";
    //这个名字会由服务端种到客户端的浏览器上，
    private final static String COOKIE_NAME = "mall_login_token";

    //获取cookie
    public static String readLoginToken(HttpServletRequest request) {
        Cookie[] cks = request.getCookies();
        if(cks != null) {
            for(Cookie ck : cks) {
                log.info("cookieName:{}, cookieValue:{}", ck.getName(), ck.getValue());
                if(StringUtils.equals(ck.getName(), COOKIE_NAME)) {
                    log.info("return cookie_name:{}, cookie_value:{}", ck.getName(), ck.getValue());
                    return ck.getValue();
                }
            }
        }
        return null;
    }

    //X:domain=".mall.com"，a,b,c,d,e都能拿到这个cookie
    //a,b不能看到彼此的cookie
    //a:A.mall.com      cookie:domain = A.mall.com;path = "/"
    //b:B.mall.com      cookie:domain = B.mall.com;path = "/"
    //c,d能共享a的cookie，因为domain相同；c,d也能共享e的cookie，因为domain和path
    //c,d不能看到彼此的cookie,也不能看到b的cookie
    //c:A.mall.com/test/cc      cookie:domain = A.mall.com;path = "/test/cc"
    //d:A.mall.com/test/dd      cookie:domain = A.mall.com;path = "/test/dd"
    //e:A.mall.com/test     cookie:domain = A.mall.com;path = "/test"
    //登录时，写入cookie
    public static void writeLoginToken(HttpServletResponse response, String token) {
        Cookie ck = new Cookie(COOKIE_NAME, token);
        ck.setDomain(COOKIE_DOMAIN);
        //"/"代表设置在根目录，
        ck.setPath("/");
        //禁止通过脚本访问cookie，可以防止脚本攻击泄露信息
        ck.setHttpOnly(true);
        //如果是-1，代表永久，单位是s；如果不设置这个变量，则cookie不会写入硬盘，而只是卸载内存，值在当前页面有效
        ck.setMaxAge(60 * 60 * 24 * 365);
        log.info("write cookie_name:{}, cookie_value:{}", ck.getName(), ck.getValue());
        response.addCookie(ck);
    }

    //删除cookie
    public static void delLoginToken(HttpServletRequest request,  HttpServletResponse response) {
        Cookie[] cks = request.getCookies();
        if(cks != null) {
            for(Cookie ck : cks) {
                if(StringUtils.equals(ck.getName(), COOKIE_NAME)) {
                    ck.setDomain(COOKIE_DOMAIN);
                    ck.setPath("/");
                    //设置成0，代表删除此cookie
                    ck.setMaxAge(0);
                    log.info("del cookie_name:{}, cookie_value:{}", ck.getName(), ck.getValue());
                    response.addCookie(ck);
                    return;
                }
            }
        }
    }
}
