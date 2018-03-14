package com.mall.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mall.common.Const;
import com.mall.common.ResponseCode;
import com.mall.common.ServerResponse;
import com.mall.dao.CategoryMapper;
import com.mall.dao.ProductMapper;
import com.mall.pojo.Category;
import com.mall.pojo.Product;
import com.mall.service.ICategoryService;
import com.mall.service.IProductService;
import com.mall.util.DateTimeUtil;
import com.mall.util.PropertiesUtil;
import com.mall.vo.ProductDetailVo;
import com.mall.vo.ProductListVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cq on 2017/11/4.
 */
@Service("iProductService")
public class ProductServiceImpl implements IProductService{

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ICategoryService iCategoryService;

    /**
     * 新增或更新商品
     * @param product
     * @return
     */
    public ServerResponse saveOrUpdateProduct(Product product) {
        //检查传过来的product是否非空
        if(product != null) {
            //如果当前商品的子图非空，取子图的第一个图片赋给主图
            if(StringUtils.isNotBlank(product.getSubImages())) {
                //将子图分割成数组，也就是把多张子图分成一张张的图片
                String[] subImageArray = product.getSubImages().split(",");
                if(subImageArray.length > 0) {
                    //将子图第一个图片作为我们的主图
                    product.setMainImage(subImageArray[0]);
                }
            }
            //根据传入的product是否有id，来判断是增加商品还是更新商品
            //因为product的id是自增的，所以如果是新增商品传进来的时候还没有id，而如果是更新商品，则有id
            // 因此不用查数据库就可以知道是新增商品还是更新商品
            if(product.getId() != null) {
                //如果有id，是更新商品
                //将product商品更新到数据库中
                int rowCount = productMapper.updateByPrimaryKey(product);
                if(rowCount > 0) {
                    return ServerResponse.createBySuccess("更新产品成功");
                }
                return ServerResponse.createBySuccess("更新产品失败");
            }
            else {
                //如果没有id，是增加商品
                //将product商品增加到数据库中
                int rowCount = productMapper.insert(product);
                if(rowCount > 0) {
                    return ServerResponse.createBySuccess("新增产品成功");
                }
                return ServerResponse.createBySuccess("新增产品失败");
            }
        }
        return ServerResponse.createByErrorMessage("新增或更新产品参数不正确");
    }

    /**
     * 修改商品销售状态，已售，在售或下架等
     * @param productId
     * @param status 销售状态
     * @return
     */
    public ServerResponse<String> setSaleStatus(Integer productId, Integer status) {
        //校验传入参数是否正确
        if(productId == null || status == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //创建product对象，以便更新属性值到数据库
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        //更新到数据库
        int rowCount = productMapper.updateByPrimaryKeySelective(product);
        if(rowCount > 0) {
            return ServerResponse.createBySuccess("修改产品销售状态成功");
        }
        return ServerResponse.createByErrorMessage("修改产品销售状态失败");
    }

    /**
     * 获取商品详细信息
     * @param productId
     * @return 返回vo对象
     */
    public ServerResponse<ProductDetailVo> manageProductDetail(Integer productId) {
        //校验传入参数
        if(productId == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //根据传入的productId查询商品对象
        Product product = productMapper.selectByPrimaryKey(productId);
        if(product == null) {
            return ServerResponse.createByErrorMessage("产品已下架或删除");
        }
        //如果该商品存在，将其显示给前端
        //这里用vo封装商品详情对象，为了前端显示数据
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);

    }
    //组装product，也就是把pojo对象product转换成vo对象ProductDetailVo
    private ProductDetailVo assembleProductDetailVo(Product product) {
        ProductDetailVo productDetailVo = new ProductDetailVo();
        productDetailVo.setId(product.getId());
        productDetailVo.setSubtitle(product.getSubtitle());
        productDetailVo.setPrice(product.getPrice());
        productDetailVo.setMainImage(product.getMainImage());
        productDetailVo.setSubImages(product.getSubImages());
        productDetailVo.setCategoryId(product.getCategoryId());
        productDetailVo.setDetail(product.getDetail());
        productDetailVo.setName(product.getName());
        productDetailVo.setStatus(product.getStatus());
        productDetailVo.setStock(product.getStock());

        //key是ftp.server.http.prefix
        //默认value是http://img.mall.com/
        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix", "http://img.mall.com/"));
        //根据product的分类id去查数据库中该分类是否存在于分类表中
        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if(category == null) {
            //如果该分类不存在，则说明当前商品分类为第一级分类结点，将其父分类置0
            productDetailVo.setParentCategoryId(0);//默认根节点
        }
        else {
            //如果该分类存在，则直接将其父类id赋给vo对象
            productDetailVo.setParentCategoryId(category.getParentId());
        }
        //时间转换，因为要将从数据库中拿到的时间转换为容易阅读的时间格式
        //mybatis中获取过来的是ms级别的，转换为年月日标准
        productDetailVo.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
        return productDetailVo;
    }

    /**
     * 分页查询商品列表
     * @param pageNum 页码
     * @param pageSize 每页显示数量
     * @return
     */
    public ServerResponse<PageInfo> getProductList(int pageNum, int pageSize) {
        //1.startPage--start
        //开始分页，页码默认值是1，每页显示数量默认值是10
        PageHelper.startPage(pageNum, pageSize);

        //2.填充自己的sql查询逻辑
        //从数据库中按分页查出所有product信息
        //这里到底是先分页再查询还是先查询再分页？我觉得是先查询出所有的后分页，虽然这里加了limit语句
        //这里pageHelper会在sql语句中自动加入limit语句，这样在数据库层就实现了分页查询而不用把所有数据都查出来之后再进行分页处理
        List<Product> productList = productMapper.selectList();
        //创建vo对象
        List<ProductListVo> productListVoList = Lists.newArrayList();
        //将查出来的product对象放入转变成vo对象后放入voList中保存
        for(Product productItem : productList) {
            ProductListVo productListVo = assembleProductListVo(productItem);
            productListVoList.add(productListVo);
        }

        //3.pageHelper--收尾
        //分页结果用PageInfo接收，会根据productList这个集合自动进行分页处理
        PageInfo pageResult = new PageInfo(productList);
        //前端展示对象用vo，所以这里将voList对象再次传入PageInfo中，对其进行重置
        pageResult.setList(productListVoList);
        //将PageInfo返回
        return ServerResponse.createBySuccess(pageResult);
    }
    //组装product对象成vo对象
    private ProductListVo assembleProductListVo(Product product) {
        ProductListVo productListVo = new ProductListVo();
        productListVo.setId(product.getId());
        productListVo.setName(product.getName());
        productListVo.setCategoryId(product.getCategoryId());
        productListVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix", "http://img.mall.com/"));
        productListVo.setMainImage(product.getMainImage());
        productListVo.setPrice(product.getPrice());
        productListVo.setSubtitle(product.getSubtitle());
        productListVo.setStatus(product.getStatus());
        return productListVo;
    }

    /**
     * 搜索商品
     * @param productName 商品名称
     * @param productId 商品id
     * @param pageNum 页码
     * @param pageSize 每页显示数量
     * @return
     */
    public ServerResponse<PageInfo> searchProduct(String productName, Integer productId, int pageNum, int pageSize) {
        //分页开始
        PageHelper.startPage(pageNum, pageSize);
        //如果productName非空，则将其构建成sql语句查询形式，也就是加上%，让其实现模糊查找
        if(StringUtils.isNotBlank(productName)) {
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
        }
        //从数据库中查询product
        List<Product> productList = productMapper.selectByNameAndProductId(productName, productId);
        //将product对象转成vo对象
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for(Product productItem : productList) {
            ProductListVo productListVo = assembleProductListVo(productItem);
            productListVoList.add(productListVo);
        }
        //将分页结果放入PageInfo中
        PageInfo pageResult = new PageInfo(productList);
        pageResult.setList(productListVoList);
        return ServerResponse.createBySuccess(pageResult);
    }

    /**
     * 前台获取商品详细信息
     * @param productId
     * @return
     */
    public ServerResponse<ProductDetailVo> getProductDetail(Integer productId) {
        //校验传入的productId是否正确
        if(productId == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //根据productId从数据库中取出product对象
        Product product = productMapper.selectByPrimaryKey(productId);
        if(product == null) {
            return ServerResponse.createByErrorMessage("产品已下架或删除");
        }
        //判断商品是否是在售状态
        if(product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()) {
            return ServerResponse.createByErrorMessage("产品已下架或删除");
        }
        //如果商品是在售状态，将其转换为vo对象传给前端展示
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }

    /**
     * 前台根据关键字查询商品列表
     * @param keyword 搜索关键字
     * @param categoryId 分类id
     * @param pageNum
     * @param pageSize
     * @param orderBy
     * @return
     */
    public ServerResponse<PageInfo> getProductByKeywordCategory(String keyword, Integer categoryId, int pageNum, int pageSize, String orderBy) {
        //校验传入的参数是否正确
        if(StringUtils.isBlank(keyword) && categoryId == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //创建一个category分类集合
        //如果传入的是一个高级分类，要调用递归算法，把所有子分类都查出来并加上其本身放入list中
        List<Integer> categoryIdList = new ArrayList<Integer>();

        //如果分类id非空
        if(categoryId != null) {
            //根据分类id查出该分类对象
            Category category = categoryMapper.selectByPrimaryKey(categoryId);
            //如果没有该分类对象，且keyword关键字也为空
            if(category == null && StringUtils.isBlank(keyword)) {
                //没有该分类，并且还没有关键字，这个时候返回一个空的结果集，不报错
                PageHelper.startPage(pageNum, pageSize);
                List<ProductListVo> productListVoList = Lists.newArrayList();
                //这里将空的voList直接放入PageInfo中，下面不需要再加上set方法
                //因为这里商品是空，也就是查询结果集没有变化，不需要将其从Pojo转成vo向前端展示，所以这里不需要重新加入set.list
                PageInfo pageInfo = new PageInfo(productListVoList);
                return ServerResponse.createBySuccess(pageInfo);
            }
            //查询其子分类，这里要取getData()，因为这里selectCategoryAndChildrenById返回的对象是一个ServerResponse，要取其中的data对象才是分类集合
            categoryIdList = iCategoryService.selectCategoryAndChildrenById(category.getId()).getData();
        }

        //如果关键字keyword非空
        if(StringUtils.isNotBlank(keyword)) {
            //将关键字加上%，以让其在sql语句中实现模糊查询
            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }
        //开始分页
        PageHelper.startPage(pageNum, pageSize);

        //排序处理：利用PageHelper自动实现
        //校验传入的orderBy参数是否正确
        if(StringUtils.isNotBlank(orderBy)) {
            //校验传入的orderBy是否在约定的排序顺序中
            if(Const.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)) {
                //将orderBy按照_下划线分割
                String[] orderByArray = orderBy.split("_");
                //PageHelper调用orderBy方法的参数的规则是price asc表示按照价格升序排序
                PageHelper.orderBy(orderByArray[0] + " " + orderByArray[1]);
            }
        }

        //正式开始搜索商品，根据keyword和ids
        List<Product> productList = productMapper.selectByNameAndCategoryIds(StringUtils.isBlank(keyword)?null:keyword, categoryIdList.size()==0?null:categoryIdList);
        //将product对象转成voList对象
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for(Product product : productList) {
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        //将查询结果放入pageInfo中，在此才开始分页
        PageInfo pageInfo = new PageInfo(productList);
        pageInfo.setList(productListVoList);

        return ServerResponse.createBySuccess(pageInfo);
    }
}
