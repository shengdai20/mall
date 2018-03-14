package com.mall.service.Impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mall.common.Const;
import com.mall.common.ResponseCode;
import com.mall.common.ServerResponse;
import com.mall.dao.CartMapper;
import com.mall.dao.ProductMapper;
import com.mall.pojo.Cart;
import com.mall.pojo.Product;
import com.mall.service.ICartService;
import com.mall.util.BigDecimalUtil;
import com.mall.util.PropertiesUtil;
import com.mall.vo.CartProductVo;
import com.mall.vo.CartVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by cq on 2017/11/6.
 */
@Service("iCartService")
public class CartServiceImpl implements ICartService{
    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 加购物车
     * 这里添加购物车的逻辑就是我们使用时的逻辑：浏览商品->点击添加购物车按钮->如果购物车表中没有该商品则添加一行新的购物车商品放进购物车表中；如果有，则直接更新购物车中该商品的数量即可
     * 这里对购物车进行了持久化，存进数据库中的，是否可以放进缓存中呢？因为购物车中的商品应该是有期限的，时间到了就会自动清除掉
     * 这个自动清楚工作可否由数据库承担
     * @param userId 用户id
     * @param productId 商品id
     * @param count 商品数量
     * @return
     */
    public ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count) {
        //校验传入的参数是否正确
        if(productId == null || count == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //根据用户id和商品id查购物车并返回
        Cart cart = cartMapper.selectCartByUserIdProductId(userId, productId);
        if(cart == null) {
            //这个产品不在这个购物车里，需要新增一个这个产品的记录
            //如果购物车中没有该商品，新建一个购物车记录
            Cart cartItem = new Cart();
            cartItem.setQuantity(count);
            cartItem.setChecked(Const.Cart.CHECKED);//新加入购物车时，默认是选中状态，这个选中不是从商品列表中选一个商品加入购物车的选中，而是已经加入购物车了，在当前购物车中选中某一个商品进入下一步提交订单操作或其他操作
            cartItem.setProductId(productId);
            cartItem.setUserId(userId);
            cartMapper.insert(cartItem);
        }
        else {
            //这个产品已经在购物车里了
            //如果产品已存在，数量相加
            count = cart.getQuantity() + count;
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        //添加完购物车后，将购物车列表返回即可
        return this.list(userId);
    }

    /**
     * 更新购物车某个商品的数量
     * @param userId
     * @param productId
     * @param count
     * @return
     */
    public ServerResponse<CartVo> update(Integer userId, Integer productId, Integer count) {
        if(productId == null || count == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //根据userId和productId查询该商品的购物车
        Cart cart = cartMapper.selectCartByUserIdProductId(userId, productId);
        //如果购物车存在，更新当前购物车中商品的数量
        if(cart != null) {
            cart.setQuantity(count);
        }
        //更新购物车
        cartMapper.updateByPrimaryKeySelective(cart);
        //更新完购物车后将购物车列表返回即可
        return this.list(userId);
    }

    /**
     * 从购物车中删除商品
     * @param userId
     * @param productIds
     * @return
     */
    public ServerResponse<CartVo> deleteProduct(Integer userId, String productIds) {
        //传统方法：将productIds分割后转成数组，再遍历数组才能添加到集合当中
        //这里使用guava提供的方法，直接将其转成集合
        List<String> productList = Splitter.on(".").splitToList(productIds);
        //这里也使用guava提供的集合判空
        if(CollectionUtils.isEmpty(productList)) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //从数据库中删除商品
        cartMapper.deleteByUserIdProductIds(userId, productList);
        //删除商品完成后将购物车列表返回即可
        return this.list(userId);
    }

    /**
     * 查询购物车
     * @param userId
     * @return
     */
    public ServerResponse<CartVo> list(Integer userId) {
        CartVo cartVo = this.getCartVoLimit(userId);
        return ServerResponse.createBySuccess(cartVo);
    }

    /**
     * 全选，全反选，单选，单反选
     * @param userId
     * @param productId 商品id
     * @param checked 是否选中状态
     * @return
     */
    public ServerResponse<CartVo> selectOrUnSlect(Integer userId, Integer productId, Integer checked) {
        //更新选中商品选中状态
        cartMapper.checkedOrUncheckedProduct(userId, productId, checked);
        //更新后将购物车列表返回即可
        return this.list(userId);
    }

    /**
     * 获取购物车中商品数量，即显示在页面最顶端的数量，淘宝限制购物车总数量是120
     * 查询当前用户的购物车里的产品数量，如果一个产品有10个，那么数量就是10
     * @param userId
     * @return
     */
    public ServerResponse<Integer> getCartProductCount(Integer userId) {
        if(userId == null) {
            return ServerResponse.createBySuccess(0);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }

    private CartVo getCartVoLimit(Integer userId) {
        CartVo cartVo = new CartVo();
        //根据userId查出当前用户的所有购物车，其实一行也就存了一个商品
        List<Cart> cartList = cartMapper.selectCartByUserId(userId);
        List<CartProductVo> cartProductVoList = Lists.newArrayList();

        //初始化购物车的总价为0，这里要用BigDecimal的string构造器，否则会造成精度丢失问题
        BigDecimal cartTotalPrice = new BigDecimal("0");
        //对购物车列表进行判空
        if(CollectionUtils.isNotEmpty(cartList)) {
            //如果非空遍历购物车列表
            for(Cart cartItem : cartList) {
                //创建cartProductVo对象，将购物车中有用的信息放入cartProductVo对象中
                CartProductVo cartProductVo = new CartProductVo();
                cartProductVo.setId(cartItem.getId());
                cartProductVo.setUserId(cartItem.getUserId());
                cartProductVo.setProductId(cartItem.getProductId());
                //根据购物车中的商品id取产品表中查出该商品的详细信息
                Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
                if(product != null) {
                    //将该商品的详细信息有用的部分放入cartProductVo对象中
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductSubtitle(product.getSubtitle());
                    cartProductVo.setProductStatus(product.getStatus());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductStock(product.getStock());
                    //判断库存，定义购买数量限制
                    int buyLimitCount = 0;
                    //当该商品的库存>=购物车中的数量时
                    if(product.getStock() >= cartItem.getQuantity()) {
                        //库存充足的时候
                        buyLimitCount = cartItem.getQuantity();//将购买数量限制设定为购物车中的商品数量
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                    }
                    //当该商品的库存<购物车中的数量时
                    else {
                        buyLimitCount = product.getStock();//将购买数量限制设定为商品的库存
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                        //购物车中更新有效库存
                        Cart cartForQuantity = new Cart();
                        cartForQuantity.setId(cartItem.getId());
                        cartForQuantity.setQuantity(buyLimitCount);
                        cartMapper.updateByPrimaryKeySelective(cartForQuantity);
                    }
                    //重新设置caartProductVo的数量
                    cartProductVo.setQuantity(buyLimitCount);
                    //计算总价，这里只是当前商品的一个总价，即该商品单价*数量
                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(), cartProductVo.getQuantity()));
                    //将勾选状态更新到cartProductVo对象中
                    cartProductVo.setProductChecked(cartItem.getChecked());
                }
                if(cartItem.getChecked() == Const.Cart.CHECKED) {
                    //如果已经勾选，增加到整个的购物车总价中
                    cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(), cartProductVo.getProductTotalPrice().doubleValue());
                }
                //将当前cartProductVo对象增加到list中
                cartProductVoList.add(cartProductVo);
            }
        }
        //更新cartVo相关的信息
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setCartProductVoList(cartProductVoList);
        cartVo.setAllChecked(this.getAllCheckedStatus(userId));
        //设置图片服务器前缀
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

        return cartVo;
    }
    //判断购物车中的商品是否全部勾选
    private boolean getAllCheckedStatus(Integer userId) {
        if(userId == null) {
            return false;
        }
        //反向思维：查看购物车中是否有该用户的商品呈未勾选状态即可，如果有未勾选的则说明没有全部勾选
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;
    }


}
