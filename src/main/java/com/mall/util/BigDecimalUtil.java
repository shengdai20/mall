package com.mall.util;

import java.math.BigDecimal;

/**
 * Created by cq on 2017/11/6.
 */
public class BigDecimalUtil {

    //防止其在外部实例化
    private BigDecimalUtil() {

    }

    //加法
    public static BigDecimal add(double v1, double v2) {
        //将参数转成string再传入BigDecimal构造器中，那么数据库中就可以只存float或double类型
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.add(b2);
    }

    //减法
    public static BigDecimal sub(double v1, double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.subtract(b2);
    }

    //乘法
    public static BigDecimal mul(double v1, double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.multiply(b2);
    }

    //除法
    public static BigDecimal div(double v1, double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        //第二个参数是保留几位小数，第三个参数是相关模式
        return b1.divide(b2, 2, BigDecimal.ROUND_HALF_UP);//四舍五入，保留两位小数
    }
}
