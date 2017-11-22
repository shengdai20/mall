package com.mall.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by cq on 2017/11/6.
 */
public class CartVo {

    private List<CartProductVo> cartProductVoList;//购物车中商品的List结合

    private BigDecimal cartTotalPrice;//购物车中商品的总价

    private Boolean allChecked;//是否已经都勾选

    private String imageHost;

    public List<CartProductVo> getCartProductVoList() {
        return cartProductVoList;
    }

    public void setCartProductVoList(List<CartProductVo> cartProductVoList) {
        this.cartProductVoList = cartProductVoList;
    }

    public BigDecimal getCartTotalPrice() {
        return cartTotalPrice;
    }

    public void setCartTotalPrice(BigDecimal cartTotalPrice) {
        this.cartTotalPrice = cartTotalPrice;
    }

    public Boolean getAllChecked() {
        return allChecked;
    }

    public void setAllChecked(Boolean allChecked) {
        this.allChecked = allChecked;
    }

    public String getImageHost() {
        return imageHost;
    }

    public void setImageHost(String imageHost) {
        this.imageHost = imageHost;
    }
}
