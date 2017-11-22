package com.mall.service.Impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mall.common.ServerResponse;
import com.mall.dao.CategoryMapper;
import com.mall.pojo.Category;
import com.mall.service.ICategoryService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Created by cq on 2017/11/3.
 */
@Service("iCategoryService")
public class CategoryServiceImpl implements ICategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    private Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    /**
     * 增加分类
     * @param categoryName 分类名
     * @param parentId
     * @return
     */
    public ServerResponse addCategory(String categoryName, Integer parentId) {
        //校验传入的分类名和parentId是否正确传入
        if(parentId == null || StringUtils.isBlank(categoryName)) {
            return ServerResponse.createByErrorMessage("添加品类参数错误");
        }
        Category category = new Category();
        category.setName(categoryName);
        category.setParentId(parentId);
        category.setStatus(true);//这个分类是可用的

        //将分类增加到数据库中持久化
        int rowCount = categoryMapper.insert(category);
        if(rowCount > 0) {
            return ServerResponse.createBySuccess("添加品类成功");
        }
        return ServerResponse.createByErrorMessage("添加品类失败");
    }

    /**
     * 更新分类名字
     * @param categoryId
     * @param categoryName
     * @return
     */
    public ServerResponse updateCategoryName(Integer categoryId, String categoryName) {
        //校验传入参数是否正确
        if(categoryId == null || StringUtils.isBlank(categoryName)) {
            return ServerResponse.createByErrorMessage("更新品类参数错误");
        }
        //将更新字段存入category对象中
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);

        //将category对象直接更新到数据库中
        int rowCount = categoryMapper.updateByPrimaryKeySelective(category);
        if(rowCount > 0) {
            return ServerResponse.createBySuccess("更新品类名字成功");
        }
        return ServerResponse.createByErrorMessage("更新品类名字失败");
    }

    /**
     * 获取当前分类的子分类，不递归，比如家电分类下的冰箱
     * @param categoryId
     * @return
     */
    public ServerResponse<List<Category>> getChildrenParallelCategory(Integer categoryId) {
        //将传入的categoryId作为parentId查询其分类下的第一层分类
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        //如果当前分类下没有子分类，不需要返回特别的信息，返回的时候就是null，但是可以将其信息打印到日志中供查看
        if(CollectionUtils.isEmpty(categoryList)) {
            logger.info("未找到当前分类的子分类");
        }
        return ServerResponse.createBySuccess(categoryList);
    }

    /**
     * 递归查询本结点的id及孩子结点的id
     * @param categoryId
     * @return
     */
    public  ServerResponse<List<Integer>> selectCategoryAndChildrenById(Integer categoryId) {
        Set<Category> categorySet = Sets.newHashSet();
        //调用递归函数，将set初始化放入参数中
        findChildrenCategory(categorySet, categoryId);
        //创建返回list
        List<Integer> categoryIdList = Lists.newArrayList();
        //如果id不为空，说明有返回结点，则将递归查到的所有id都加入到list中然后返回
        if(categoryId != null) {
            for(Category categoryItem : categorySet) {
                categoryIdList.add(categoryItem.getId());
            }
        }
        return ServerResponse.createBySuccess(categoryIdList);
    }
    //递归算法，算出子结点
    //set集合不允许重复值
    private Set<Category> findChildrenCategory(Set<Category> categorySet, Integer categoryId) {
        //根据id查category对象
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if(category != null) {
            //将查找出来的category对象加入set集合中
            categorySet.add(category);
        }
        //查找子结点，递归算法一定要有一个退出的条件
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        for(Category categoryItem : categoryList) {
            //递归调用
            findChildrenCategory(categorySet, categoryItem.getId());
        }
        //将set集合返回
        return categorySet;
    }
}
