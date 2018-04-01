package com.mall.controller.backend;

import com.google.common.collect.Maps;
import com.mall.common.Const;
import com.mall.common.ResponseCode;
import com.mall.common.ServerResponse;
import com.mall.pojo.Product;
import com.mall.pojo.User;
import com.mall.service.IFileService;
import com.mall.service.IProductService;
import com.mall.service.IUserService;
import com.mall.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 后台商品管理
 * Created by cq on 2017/11/4.
 */
@Controller
@RequestMapping("/manage/product")
public class ProductManageController {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private IProductService iProductService;

    @Autowired
    private IFileService iFileService;

    /**
     * 增加商品
     * @param product
     * @return
     */
    @RequestMapping(value = "product_save.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse productSave(HttpServletRequest request, Product product) {
        //从session中获取用户登录信息
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录管理员");
        }
        //检查登录用户是否是管理员
        if(iUserService.checkAdminRole(user).isSuccess()) {
            //填充我们增加产品的业务逻辑
            return iProductService.saveOrUpdateProduct(product);
        }
        else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    /**
     * 修改商品销售状态，已售，在售或下架
     * @param productId
     * @param status 销售状态
     * @return
     */
    @RequestMapping(value = "set_sale_status.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse setSaleStatus(HttpServletRequest request, Integer productId, Integer status) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录管理员");
        }
        if(iUserService.checkAdminRole(user).isSuccess()) {
            return iProductService.setSaleStatus(productId, status);
        }
        else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    /**
     * 获取商品详细信息
     * @param productId
     * @return
     */
    @RequestMapping(value = "get_detail.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse getDetail(HttpServletRequest request, Integer productId) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录管理员");
        }
        if(iUserService.checkAdminRole(user).isSuccess()) {
            //填充业务
            return iProductService.manageProductDetail(productId);
        }
        else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    /**
     * 分页查询商品列表
     * @param pageNum 页码
     * @param pageSize 每页显示数量
     * @return
     */
    @RequestMapping(value = "get_list.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse getList(HttpServletRequest request, @RequestParam(value = "pageNum", defaultValue = "1") int pageNum, @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录管理员");
        }
        if(iUserService.checkAdminRole(user).isSuccess()) {
            //填充业务
            return iProductService.getProductList(pageNum, pageSize);
        }
        else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    /**
     * 搜索商品
     * @param productName 根据商品名称搜索
     * @param productId 根据商品id搜索
     * @param pageNum 页码
     * @param pageSize 每页显示数量
     * @return
     */
    @RequestMapping(value = "product_search.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse product_search(HttpServletRequest request, String productName, Integer productId, @RequestParam(value = "pageNum", defaultValue = "1") int pageNum, @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录管理员");
        }
        if(iUserService.checkAdminRole(user).isSuccess()) {
            //填充业务
            return iProductService.searchProduct(productName, productId, pageNum, pageSize);
        }
        else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    /**
     * 上传图片文件到ftp服务器
     * @param file 要上传的图片文件，从前端获取
     * @param request
     * @return
     */
    @RequestMapping(value = "upload.do", method = RequestMethod.POST)
    @ResponseBody
    //jsp中form表单中的name与这里的value值要相同
    public ServerResponse upload(HttpServletRequest httpServletRequest, @RequestParam(value = "uploadFile", required = false) MultipartFile file, HttpServletRequest request) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录管理员");
        }
        if(iUserService.checkAdminRole(user).isSuccess()) {
            //从request的session中拿到servlet上下文，上传文件的文件夹是upload
            //这个路径上传完之后会自动创建到发布之后的webapp下，与WEB-INF同级，也就是会自动创建一个upload文件夹
            String path = request.getSession().getServletContext().getRealPath("upload");
            String targetFileName = iFileService.upload(file, path);
            //将url返回给前端
            String url = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFileName;
            Map fileMap = Maps.newHashMap();
            fileMap.put("uri", targetFileName);
            fileMap.put("url", url);
            return ServerResponse.createBySuccess(fileMap);
        }
        else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    /**
     * 富文本图片上传
     * @param file
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "richtext_img_upload.do", method = RequestMethod.POST)
    @ResponseBody
    public Map richtextImgUpload(HttpServletRequest httpServletRequest, @RequestParam(value = "uploadFile", required = false) MultipartFile file, HttpServletRequest request, HttpServletResponse response) {
        Map resultMap = Maps.newHashMap();
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            resultMap.put("success", false);
            resultMap.put("msg", "请登录管理员");
            return resultMap;
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            resultMap.put("success", false);
            resultMap.put("msg", "请登录管理员");
            return resultMap;
        }
        //富文本对于返回值有自己的要求，我们使用的是simditor的要求进行返回
        //要求的返回json格式：
        // {
        //      "success":true/false,
        //      "msg":...,
        //      "file_path":(real_file_path)
        // }
        // 在这里用map承接返回
        if(iUserService.checkAdminRole(user).isSuccess()) {
            //request.getSession().getServletContext() 获取的是Servlet容器对象，相当于tomcat容器了。
            // getRealPath("/") 获取实际路径，“/”指代项目根目录，所以代码返回的是项目在容器中的实际发布运行的根路径
            String path = request.getSession().getServletContext().getRealPath("upload");
            String targetFileName = iFileService.upload(file, path);
            if(StringUtils.isBlank(targetFileName)) {
                resultMap.put("success", false);
                resultMap.put("msg", "上传失败");
                return resultMap;
            }
            String url = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFileName;

            resultMap.put("success", true);
            resultMap.put("msg", "上传成功");
            resultMap.put("file_path", url);

            //处理header，是simditor要求的
            response.addHeader("Access-Control-Allow-Headers", "X-File-Name");
            return resultMap;
        }
        else {
            resultMap.put("success", false);
            resultMap.put("msg", "无权限操作");
            return resultMap;
        }
    }
}
