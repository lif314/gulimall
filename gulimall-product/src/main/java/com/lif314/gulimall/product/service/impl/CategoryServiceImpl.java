package com.lif314.gulimall.product.service.impl;

import com.lif314.gulimall.product.service.CategoryBrandRelationService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.CategoryDao;
import com.lif314.gulimall.product.entity.CategoryEntity;
import com.lif314.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    // 注入dao来查询数据库表 -- 也可以使用泛型
//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // [1] 查出所有分类 -- 在dao中查询该表

        // 使用泛型 -- baseMapper即对应的dao
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);//查询所有 没有查询条件null

        // [2] 组装成父子树形结构
        // 找到一级分类 -- 父分类id为0
        List<CategoryEntity> menuTree = categoryEntities.stream().filter( (categoryEntity) -> {
            // 过滤条件
            return categoryEntity.getParentCid().longValue() == 0;
            // 一级分类收集为集合
        }).map((menu) -> {
            // 保存每一个菜单的子分类
            menu.setChildren(getChildrens(menu, categoryEntities));
            return menu;
        }).sorted((menu1, menu2) -> {
            // 菜单排序
            return (menu1.getSort() == null?0:menu1.getSort()) - (menu2.getSort() == null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return menuTree;
    }

    /**
     * 删除菜单那
     * @param asList id数组
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO：检查菜单是否被引用

        // 逻辑删除-show_status
        baseMapper.deleteBatchIds(asList);
    }

    // [2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        // 逆序转换
        Collections.reverse(paths);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新
     */
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        //级联更新
        if(!StringUtils.isEmpty(category.getName())){
            categoryBrandRelationService.updateCategoryName(category.getCatId(), category.getName());

            // TODO:级联更新
        }
    }

    // 递归查询并收集路径信息 225,25,2
    private List<Long> findParentPath(Long catelogId, List<Long> paths ){
        // 获取当前分类的id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        // 如果存在父分类，则需要递归查询
        if(byId.getParentCid()!=0){
            //递归查找父节点
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    /**
     * 递归查找所有菜单的子菜单
     * @param root 当前菜单
     * @param all 所有菜单
     * @return 子菜单
     */
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            // 过滤 当菜单的父id等于root菜单的id则为root菜单的子菜单
            return categoryEntity.getParentCid().longValue() == root.getCatId().longValue();  // 注意此处应该用longValue()来比较，否则会出先bug，因为parentCid和catId是long类型
        }).map(categoryEntity -> {
            // 1 找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            // 2 菜单的排序
            return (menu1.getSort() == null?0:menu1.getSort()) - (menu2.getSort() == null?0:menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

}
