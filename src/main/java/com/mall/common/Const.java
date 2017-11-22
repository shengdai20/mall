package com.mall.common;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by cq on 2017/11/1.
 */
public class Const {

    public static final String CURRENT_USER = "currentUser";

    public static final String EMAIL = "email";
    public static final String USERNAME = "username";

    public interface ProductListOrderBy {
        //用guava库初始化对象，里面初始化了两个值，一个是price_desc，一个是price_asc
        //为什么用set？set的contains的时间复杂度是O(1)，list的contains的时间复杂度是O(n)
        Set<String> PRICE_ASC_DESC = Sets.newHashSet("price_desc", "price_asc");
    }

    public interface Cart{
        int CHECKED = 1;//购物车选中状态
        int UN_CHECKED = 0;//购物车未选中状态

        String LIMIT_NUM_FAIL = "LIMIT_NUM_FAIL";
        String LIMIT_NUM_SUCCESS = "LIMIT_NUM_SUCCESS";
    }

    public interface Role {
        int ROLE_CUSTOMER = 1;//普通用户
        int ROLE_ADMIN = 0;//管理员
    }

    //商品状态：在售，下架
    public enum ProductStatusEnum {
        ON_SALE(1, "在售");
        private String value;
        private int code;
        ProductStatusEnum(int code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }
    }
}
