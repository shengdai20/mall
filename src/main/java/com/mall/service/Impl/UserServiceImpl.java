package com.mall.service.Impl;

import com.mall.common.Const;
import com.mall.common.ServerResponse;
import com.mall.common.TokenCache;
import com.mall.dao.UserMapper;
import com.mall.pojo.User;
import com.mall.service.IUserService;
import com.mall.util.MD5Util;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created by cq on 2017/10/31.
 */
@Service("iUserService")//注入
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 登录
     *
     * @param username
     * @param password
     * @return
     */
    @Override
    public ServerResponse<User> login(String username, String password) {
        //校验用户名是否存在，如果存在，查询结果会返回1，因为用户名唯一；否则返回0
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        //密码登录MD5，将密码加密后再与数据库中的密码进行比对，因为数据库中存的不是明文密码，是md5加密后的密码
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        //这里不是根据用户名校验密码是否正确，而是将用户名和密码同时传入dao层，在数据库中一起校验是否有当前用户存在，如果存在表明校验通过返回该用户对象
        User user = userMapper.selectLogin(username, md5Password);
        if (user == null) {
            return ServerResponse.createByErrorMessage("密码错误");
        }
        //将返回给controller的密码置空，是为了不让其显示出来
        user.setPassword(StringUtils.EMPTY);
        //校验通过，返回user对象给controller进行显示
        return ServerResponse.createBySuccess("登录成功", user);
    }

    /**
     * 注册
     *
     * @param user
     * @return
     */
    public ServerResponse<String> register(User user) {
        //因为下面的校验用户名和email具有相同的逻辑，所以可以集成放在一个函数中，即checkValid函数，用第二个参数辨别传入的是用户名还是email，然后再校验
        //校验用户名是否已经存在
        ServerResponse validResponse = this.checkValid(user.getUsername(), Const.USERNAME);
        if (!validResponse.isSuccess()) {
            return validResponse;
        }
        //校验email是否已经存在
        validResponse = this.checkValid(user.getEmail(), Const.EMAIL);
        if (!validResponse.isSuccess()) {
            return validResponse;
        }
        //设置用户角色
        //这里还是有点问题，注册的时候怎么区分是普通用户还是管理员？如果是由前端直接判断，那应该已经传入用户角色了啊，如果不是由前端判断，那这里应该加以判断吧？
        user.setRole(Const.Role.ROLE_CUSTOMER);
        //MD5加密，加密后再存入数据库
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        //进入dao层进行插入操作
        int resultCount = userMapper.insert(user);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    /**
     * 校验用户名和email的有效性
     *
     * @param str
     * @param type
     * @return
     */
    public ServerResponse<String> checkValid(String str, String type) {
        if (StringUtils.isNotBlank(type)) {
            //开始校验
            if (Const.USERNAME.equals(type)) {
                int resultCount = userMapper.checkUsername(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            }
            if (Const.EMAIL.equals(type)) {
                int resultCount = userMapper.checkEmail(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("email已存在");
                }
            }
        } else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMessage("校验成功");
    }

    /**
     * 找回密码的问题
     * @param username
     * @return
     */
    public ServerResponse selectQuestion(String username) {
        //校验用户名是否存在，复用上面的校验函数
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if(validResponse.isSuccess()) {
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        //根据用户名查询设置的问题
        String question = userMapper.selectQuestionByUsername(username);
        if(StringUtils.isNotBlank(question)) {
            //为什么这里调用的是泛型参数函数而不是string参数函数？
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMessage("找回密码的问题是空的");
    }

    /**
     * 校验找回密码的问题的答案的正确性
     * @param username
     * @param question
     * @param answer
     * @return
     */
    public ServerResponse<String> checkAnswer(String username, String question, String answer) {
        //校验问题答案是否正确
        int resultCount = userMapper.checkAnswer(username, question, answer);
        if(resultCount > 0) {
            //说明问题及问题答案是这个用户的，并且是正确的
            //利用UUID创建无重复token
            String forgetToken = UUID.randomUUID().toString();
            //把token放入本地cache中，然后设置其有效期
            TokenCache.setKey(TokenCache.TOKEN_PREFIX + username, forgetToken);
            //为什么这里调用的是泛型参数函数而不是string参数函数
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByErrorMessage("问题的答案错误");
    }

    /**
     * 忘记密码后重置密码
     * @param username
     * @param passwordNew
     * @param forgetToken
     * @return
     */
    public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken) {
        //校验token是否已经传递过来
        if(StringUtils.isBlank(forgetToken)) {
            return ServerResponse.createByErrorMessage("参数错误，token需要传递");
        }
        //校验用户名是否存在
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if(validResponse.isSuccess()) {
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        //从cache中获取token，根据key拿到value值
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
        //校验cache中的token是否有效
        if(StringUtils.isBlank(token)) {
            return ServerResponse.createByErrorMessage("token无效或者过期");
        }
        //校验传入的forgetToken和token是否相同
        //这里用stringUtils可以避免string.equals中string出现null的问题，即使这里传入null也是可以接受的，不会报错
        if(StringUtils.equals(forgetToken, token)) {
            //将新输入的密码进行md5加密后再更新到数据库中
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            int rowCount = userMapper.updatePasswordByUsername(username, md5Password);
            if(rowCount > 0) {
                return ServerResponse.createBySuccessMessage("修改密码成功");
            }
        }
        else {
            return ServerResponse.createByErrorMessage("token错误，请重新获取重置密码的token");
        }
        return ServerResponse.createByErrorMessage("修改密码失败");
    }

    /**
     * 登录状态重置密码
     * @param passwordOld 旧密码
     * @param passwordNew 新密码
     * @param user
     * @return
     */
    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user) {
        //防止横向越权，要校验一下这个用户的旧密码，一定要指定是这个用户，因为我们会查询一个count(1)，如果不指定id，那么结果就是true即count>0
        //其实也就是要旧密码和用户名要匹配
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld), user.getId());
        if(resultCount == 0) {
            return ServerResponse.createByErrorMessage("旧密码错误");
        }
        //将新密码md5加密后更新到数据库中
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        //其实这里也是可以直接调用跟上面一样的updatePasswordByUsername函数进行更新的
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if(updateCount > 0) {
            return ServerResponse.createBySuccessMessage("密码更新成功");
        }
        return ServerResponse.createByErrorMessage("密码更新失败");
    }

    /**
     * 登录状态下更新个人信息
     * @param user
     * @return
     */
    public ServerResponse<User> updateInformation(User user) {
        //username不能被更新
        //email进行校验，校验新的email是不是已经存在，如果已经存在要是当前用户的email
        //这里email校验不能复用上面用过的checkEmail方法，因为这里是判断这个email是否已经存在于数据库中其他用户的，所以还要传入id
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());
        if(resultCount > 0) {
            return ServerResponse.createByErrorMessage("email已经存在，请更换email再尝试更新");
        }

        //更新到数据库中
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if(updateCount > 0) {
            //将user对象返回controller进行显示
            return ServerResponse.createBySuccess("更新个人信息成功", user);
        }
        return ServerResponse.createByErrorMessage("更新个人信息失败");
    }

    /**
     * 登录状态下获取用户个人信息
     * @param userId
     * @return
     */
    public ServerResponse<User> getInformation(Integer userId) {
        //去查数据库中是否有当前用户
        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null) {
            return ServerResponse.createByErrorMessage("找不到当前用户");
        }
        //将user的密码置空后返回，因为要返回去给前端显示的
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }

    /**
     * 校验是否是管理员身份
     * @param user
     * @return
     */
    //这个方法有点鸡肋啊，可以直接用如下代码校验管理员身份user.getRole() == Const.Role.ROLE_ADMIN
    public ServerResponse checkAdminRole(User user) {
        if(user != null && user.getRole().intValue() == Const.Role.ROLE_ADMIN) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }
}
