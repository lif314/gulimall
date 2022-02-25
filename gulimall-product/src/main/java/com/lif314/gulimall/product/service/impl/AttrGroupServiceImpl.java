package com.lif314.gulimall.product.service.impl;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.AttrGroupDao;
import com.lif314.gulimall.product.entity.AttrGroupEntity;
import com.lif314.gulimall.product.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        //如果没有选中三级分类，则查询指定的数据，id传0
        if(catelogId == 0){
            /**
             * Query里面就有个方法getPage()，传入map，将map解析为mybatis-plus的IPage对象
             * 自定义PageUtils类用于传入IPage对象，得到其中的分页信息
             * AttrGroupServiceImpl extends ServiceImpl，其中ServiceImpl的父类中有方法
             * page(IPage, Wrapper)。对于wrapper而言，没有条件的话就是查询所有
             * queryPage()返回前还会return new PageUtils(page);，把page对象解析好页码信
             * 息，就封装为了响应数据
             */
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    new QueryWrapper<AttrGroupEntity>()
            );
            // 返回分类数据
            return new PageUtils(page);
        }else{
            /**
             * 按照三级分类数据查询
             *
             * 前端会返回key，作为检索条件：如果key不是空的，则要使用key或者id进行查询
             * select * from pms_attr_group where catelog_id=? and (attr_group_id=key or attr_group_name like %key%)
             */
            String key = (String) params.get("key");
            QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId);
            if(!StringUtils.isEmpty(key)){
                wrapper.and((obj)->{
                    obj.eq("attr_group_id", key).or().like("attr_group_name", key);
                });
            }

            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params)
                    , wrapper);
            return  new PageUtils(page);
        }
    }

}
