package com.lif314.gulimall.product.service.impl;

import com.lif314.gulimall.product.entity.*;
import com.lif314.gulimall.product.service.*;
import com.lif314.gulimall.product.vo.SkuItemSaleAttrVo;
import com.lif314.gulimall.product.vo.SkuItemVo;
import com.lif314.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.SkuInfoDao;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService imagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;


    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }


    /**
     * sku检索
     *
     * {
     * page: 1,//当前页码
     * limit: 10,//每页记录数
     * sidx: 'id',//排序字段
     * order: 'asc/desc',//排序方式
     * key: '华为',//检索关键字
     * catelogId: 0,
     * brandId: 0,
     * min: 0,
     * max: 0
     * }
     */
    @Override
    public PageUtils queryPageByConditions(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();

        // 模糊查询
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((w)->{ // and括起or条件
                w.eq("sku_id", key).or().like("sku_name", key);
            });
        }

        // 三级分类
        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id", catelogId);
        }

        // 品牌
        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id", brandId);
        }

        // 价格区间

        String min = (String) params.get("min");
        if(!StringUtils.isEmpty(min)){
            wrapper.ge("price", min);
        }

        String max = (String) params.get("max");
        if(!StringUtils.isEmpty(max)){
            try{
                BigDecimal bigDecimal = new BigDecimal(max);
                if(bigDecimal.compareTo(new BigDecimal("0")) > 0){
                    wrapper.le("price", max);
                }
            }catch(Exception e){

            }
        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 通过spuid查询出所有的skus信息
     * @param spuId
     * @return
     */
    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        return this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
    }

    /**
     * 查询商品详情
     * @param skuId 商品id
     * @return 商品详情
     */
    @Override
    public SkuItemVo item(Long skuId) {
        /**
         * 1、sku基本信息【标题、副标题、价格】pms_sku_info
         * 2、sku图片信息【每个sku_id对应了多个图片】pms_sku_images
         * 3、spu下所有sku销售属性组合【不只是当前sku_id所指定的商品】
         * 4、spu商品介绍
         * 5、spu规格与包装【参数信息】
         */
        SkuItemVo skuItemVo = new SkuItemVo();

        // 1、sku基本信息【标题、副标题、价格】pms_sku_info
        SkuInfoEntity skuInfo = getById(skuId);
        Long spuId = skuInfo.getSpuId();  // SpuId 信息
        skuItemVo.setInfo(skuInfo);

        // 2、sku图片信息【每个sku_id对应了多个图片】pms_sku_images
        List<SkuImagesEntity> images = imagesService.getImagesBySkuId(skuId);
        skuItemVo.setImages(images);

        // 3、spu下所有sku销售属性组合【不只是当前sku_id所指定的商品】
        List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBySpuId(spuId);
        skuItemVo.setSaleAttr(saleAttrVos);

        // 4、spu商品介绍
        SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(spuId);
        skuItemVo.setDesc(spuInfoDesc);

        // 5、spu规格与包装【参数信息】
        Long catalogId = skuInfo.getCatalogId(); // 三级分类信息
        List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
        skuItemVo.setGroupAttrs(attrGroupVos);

        return skuItemVo;
    }

}
