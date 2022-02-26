package com.lif314.gulimall.product.service.impl;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.BrandDao;
import com.lif314.gulimall.product.entity.BrandEntity;
import com.lif314.gulimall.product.service.BrandService;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        // 获取key id或者名字
        String key = (String) params.get("key");
        // 封装查询条件
       QueryWrapper<BrandEntity> wrapper =  new QueryWrapper<BrandEntity>();
        if(!StringUtils.isEmpty(key)){
            wrapper.eq("brand_id",key).or().like("name", key);
        }
        // 加入查询条件
        IPage<BrandEntity> page = this.page(
                new Query<BrandEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

}
