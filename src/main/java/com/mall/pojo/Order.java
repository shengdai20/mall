package com.mall.pojo;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

//包括get,set,equals,hashCode,toString方法，如果没有特殊说明，就是针对所有属性
//@Data
@Getter
@Setter
//无参构造器
@NoArgsConstructor
//所有参数构造器
@AllArgsConstructor
public class Order {
    private Integer id;

    private Long orderNo;

    private Integer userId;

    private Integer shippingId;

    private BigDecimal payment;

    private Integer paymentType;

    private Integer postage;

    private Integer status;

    private Date paymentTime;

    private Date sendTime;

    private Date endTime;

    private Date closeTime;

    private Date createTime;

    private Date updateTime;

}