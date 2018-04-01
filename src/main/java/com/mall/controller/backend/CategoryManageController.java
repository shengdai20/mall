package com.mall.controller.backend;

import com.mall.common.Const;
import com.mall.common.ResponseCode;
import com.mall.common.ServerResponse;
import com.mall.pojo.User;
import com.mall.service.ICategoryService;
import com.mall.service.IUserService;
import com.mall.util.CookieUtil;
import com.mall.util.JsonUtil;
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
 * Created by cq on 2017/11/2.
 */
@Controller
@RequestMapping("/manage/category")
public class CategoryManageController {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private ICategoryService iCategoryService;

    /**
     * 添加分类
     * @param categoryName 分类名字
     * @param parentId
     * @return
     */
    @RequestMapping(value = "add_category.do", method = RequestMethod.POST)
    @ResponseBody
    //@RequestParam如果前端没有传parentId过来，则给它赋予一个默认值0，代表分类的根结点
    public ServerResponse addCategory(HttpServletRequest request, String categoryName, @RequestParam(value = "parentId", defaultValue = "0") int parentId) {
        //从session中获取用户登录信息
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录");
        }
        //校验是否是管理员
        //也可以直接用这个代码直接校验user.getRole() == Const.Role.ROLE_ADMIN
        if(iUserService.checkAdminRole(user).isSuccess()) {
            //是管理员
            //增加我们处理分类的逻辑
            return iCategoryService.addCategory(categoryName, parentId);
        }
        else {
            return ServerResponse.createByErrorMessage("无权限操作，需要管理员权限");
        }
    }

    /**
     * 更新分类名字
     * @param categoryId
     * @param categoryName
     * @return
     */
    @RequestMapping(value = "set_category_name.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse setCategoryName(HttpServletRequest request, Integer categoryId, String categoryName) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录");
        }
        if(iUserService.checkAdminRole(user).isSuccess()) {
            //更新categoryName
            return iCategoryService.updateCategoryName(categoryId, categoryName);
        }
        else {
            return ServerResponse.createByErrorMessage("无权限操作，需要管理员权限");
        }
    }

    /**
     * 获取当前分类的子分类，不递归
     * @param categoryId
     * @return
     */
    @RequestMapping(value = "get_children_parallel_category.do", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse getChildrenParallelCategory(HttpServletRequest request, @RequestParam(value = "categoryId", defaultValue = "0") Integer categoryId) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录");
        }
        if(iUserService.checkAdminRole(user).isSuccess()) {
            //查询子结点的category信息，并且不递归，保持平级
            return iCategoryService.getChildrenParallelCategory(categoryId);
        }
        else {
            return ServerResponse.createByErrorMessage("无权限操作，需要管理员权限");
        }
    }

    /**
     * 获取当前结点及孩子结点，并递归
     * @param categoryId
     * @return 返回孩子id
     */
    @RequestMapping(value = "get_category_and_deep_children_category.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse getCategoryAndDeepChildrenCategory(HttpServletRequest request, @RequestParam(value = "categoryId", defaultValue = "0") Integer categoryId) {
    //    User user = (User) session.getAttribute(Const.CURRENT_USER);

        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);

        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录");
        }
        if(iUserService.checkAdminRole(user).isSuccess()) {
            //查询当前结点的id和递归子结点的id
            return iCategoryService.selectCategoryAndChildrenById(categoryId);
        }
        else {
            return ServerResponse.createByErrorMessage("无权限操作，需要管理员权限");
        }
    }
}
