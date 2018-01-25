package com.mall.common;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

/**
 * Created by cq on 2017/10/31.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
//保证序列化json的时候，如果是null的对象，key也会消失
//比如在创建失败的时候，只需要将status和msg显示出来，但是如果不加上面的注解会将data也显示出来，其值会显示null
public class ServerResponse<T> implements Serializable{

    private int status;
    private String msg;
    private T data;

    private ServerResponse(int status) {
        this.status = status;
    }
    //如果第二个参数不是string类型调用下面这个构造器
    private ServerResponse(int status, T data) {
        this.status = status;
        this.data = data;
    }
    //如果第二个参数是string类型调用下面这个构造器
    private ServerResponse(int status, String msg) {
        this.status = status;
        this.msg = msg;
    }
    private ServerResponse(int status, String msg, T data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    @JsonIgnore
    //使之不在json序列化结果当中
    //判断响应是否成功
    public boolean isSuccess() {
        return this.status == ResponseCode.SUCCESS.getCode();
    }

    //下面三个get开头的public方法在json序列化时都会显示出来供前端看到
    //数据格式显示也就如下：
    /*
    * {status:xxx,
    * msg:xx,
    * data:xxx}
    * */
    public int getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getMsg() {
        return msg;
    }

    //成功
    //返回值是泛型<T>,传入0表示创建是成功的
    public static <T> ServerResponse<T> createBySuccess() {
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode());
    }

    //创建成功传入一个msg再返回
    public static <T> ServerResponse<T> createBySuccessMessage(String msg) {
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(), msg);
    }

    //创建成功传入data再返回
    public static <T> ServerResponse<T> createBySuccess(T data) {
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(), data);
    }

    //如果参数是string类型，但是应该把其赋给data而不是msg该怎么做？
    //调用下面的public方法同时传入msg和data即可
    //创建成功传入一个msg和data再返回
    public static <T> ServerResponse<T> createBySuccess(String msg, T data) {
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(), msg, data);
    }

    //失败
    public static <T> ServerResponse<T> createByError() {
        return new ServerResponse<T>(ResponseCode.ERROR.getCode(), ResponseCode.ERROR.getDesc());
    }

    //创建失败传入一个错误提示信息errorMessage
    public static <T> ServerResponse<T> createByErrorMessage(String errorMessage) {
        return new ServerResponse<T>(ResponseCode.ERROR.getCode(), errorMessage);
    }

    //创建失败手动传入一个code和errorMessage
    public static <T> ServerResponse<T> createByErrorCodeMessage(int errorCode, String errorMessage) {
        return new ServerResponse<T>(errorCode, errorMessage);
    }

}
