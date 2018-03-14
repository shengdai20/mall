package com.mall.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.mall.common.ServerResponse;
import com.mall.dao.ShippingMapper;
import com.mall.pojo.Shipping;
import com.mall.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by cq on 2017/11/7.
 */
@Service("iShippingService")
public class ShippingServiceImpl implements IShippingService {

    @Autowired
    private ShippingMapper shippingMapper;

    /**
     * 新增收货地址
     * @param userId
     * @param shipping
     * @return
     */
    public ServerResponse add(Integer userId, Shipping shipping) {
        shipping.setUserId(userId);
        int rowCount = shippingMapper.insert(shipping);
        if(rowCount > 0) {
            Map result = Maps.newHashMap();
            //插入之后拿到shippingId
            result.put("shippingId", shipping.getId());
            return ServerResponse.createBySuccess("新建地址成功", result);
        }
        return ServerResponse.createByErrorMessage("新建地址失败");
    }

    /**
     * 删除收货地址
     * @param userId
     * @param shippingId
     * @return
     */
    public ServerResponse<String> del(Integer userId, Integer shippingId) {
        //防止横向越权，如果这里不加userId，就有可能出现：普通用户登录之后，传一个不是自己的shippingId，而却可以更改数据库的情况
        //所以这里要加上userId来进行匹配，避免横向越权问题
        int resultCount = shippingMapper.deleteByShippingIdUserId(userId, shippingId);
        if(resultCount > 0) {
            return ServerResponse.createBySuccess("删除地址成功");
        }
        return ServerResponse.createByErrorMessage("删除地址失败");
    }

    /**
     * 更新收货地址
     * @param userId
     * @param shipping
     * @return
     */
    public ServerResponse update(Integer userId, Shipping shipping) {
        //防止横向越权问题，这里要从登录的session信息中获取userId
        shipping.setUserId(userId);
        int rowCount = shippingMapper.updateByShipping(shipping);
        if(rowCount > 0) {
            return ServerResponse.createBySuccess("更新地址成功");
        }
        return ServerResponse.createByErrorMessage("更新地址失败");
    }

    /**
     * 查看收货地址详情
     * @param userId
     * @param shippingId
     * @return
     */
    public ServerResponse<Shipping> select(Integer userId, Integer shippingId) {
        Shipping shipping = shippingMapper.selectByShippingIdUserId(userId, shippingId);
        if(shipping == null) {
            return ServerResponse.createByErrorMessage("无法查询到该地址");
        }
        //查询成功将shipping对象返回
        return ServerResponse.createBySuccess("查询地址成功", shipping);
    }

    /**
     * 分页查看收货地址
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse<PageInfo> list(Integer userId, int pageNum, int pageSize) {
        //开始分页
        PageHelper.startPage(pageNum, pageSize);
        //根据userId查询该用户的所有收货地址列表
        List<Shipping> shippingList = shippingMapper.selectByUserId(userId);
        //将查询结果放入PageInfo中进行分页
        PageInfo pageInfo = new PageInfo(shippingList);
        //将pageInfo返回给前端进行显示，这次都没有转成vo啊
        return ServerResponse.createBySuccess(pageInfo);
    }
}
