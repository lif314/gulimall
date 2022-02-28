package com.lif314.gulimall.ware.service.impl;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.ware.dao.WareSkuDao;
import com.lif314.gulimall.ware.entity.WareSkuEntity;
import com.lif314.gulimall.ware.service.WareSkuService;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    /**
     * {
     *    page: 1,//当前页码
     *    limit: 10,//每页记录数
     *    sidx: 'id',//排序字段
     *    order: 'asc/desc',//排序方式
     *    wareId: 123,//仓库id
     *    skuId: 123//商品id
     * }
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();

        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId) && !"0".equalsIgnoreCase(wareId)){
            wrapper.eq("ware_id", wareId);
        }

        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId) && !"0".equalsIgnoreCase(skuId)){
            wrapper.eq("sku_id",skuId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

}
