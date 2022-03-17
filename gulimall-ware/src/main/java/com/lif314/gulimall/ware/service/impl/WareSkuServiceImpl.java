package com.lif314.gulimall.ware.service.impl;

import com.lif314.common.to.SkuHasStockTo;
import com.lif314.common.utils.R;
import com.lif314.common.exception.NoStockException;
import com.lif314.gulimall.ware.feign.ProductFeignService;
import com.lif314.gulimall.ware.vo.SkuItemLockTo;
import com.lif314.gulimall.ware.vo.SkuWareHasStock;
import com.lif314.gulimall.ware.vo.WareSkuLockVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.ware.dao.WareSkuDao;
import com.lif314.gulimall.ware.entity.WareSkuEntity;
import com.lif314.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {


    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    /**
     * {
     * page: 1,//当前页码
     * limit: 10,//每页记录数
     * sidx: 'id',//排序字段
     * order: 'asc/desc',//排序方式
     * wareId: 123,//仓库id
     * skuId: 123//商品id
     * }
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId) && !"0".equalsIgnoreCase(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId) && !"0".equalsIgnoreCase(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 采购完成的入库
     */
    @Transactional
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 判断如果还没有这个库存记录，则需要新增
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            // 新增记录
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStockLocked(0);
            // 远程查询sku的名称   skuInfo
            // 我们没有必要因为没有仓库名字就让事务回滚，所以可以忽略没有仓库名的情况
            try {
                R info = productFeignService.info(skuId);
                if (info.getCode() == 0) {
                    Map<String, Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {
                // 有错误不用管

                /**
                 * 有错误无需回滚
                 */
                // TODO 异常出现不回滚 -- 高级篇
            }
            wareSkuDao.insert(wareSkuEntity);
        } else {
            // 更新操作
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    /**
     * 远程查询是否有库存
     */
    @Override
    public List<SkuHasStockTo> getSkuHasStock(List<Long> skuIds) {

        List<SkuHasStockTo> skuHasStockTos = skuIds.stream().map((skuId) -> {
            SkuHasStockTo skuHasStockTo = new SkuHasStockTo();
            skuHasStockTo.setSkuId(skuId);
            // 每一个商品可能在不同仓库中，我们需要查的是总库存量
            // 总库存量=库存总量-锁定的库存(下单未发货)
            Long totalStock = baseMapper.getTotalStock(skuId);
            skuHasStockTo.setHasStock(totalStock != null && totalStock > 0);
            return skuHasStockTo;
        }).collect(Collectors.toList());

        return skuHasStockTos;
    }

    /**
     * 为某个订单锁定库存
     *
     * 默认只要时运行时以后长都会回滚
     */
    // 出现该异常就要回滚
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        // TODO 按照下单地址，找到一个就近的仓库，锁定库存

        // 1、找到每个商品在哪个仓库中都有库存
        // 先考虑单个库存的信息
        List<SkuItemLockTo> locks = vo.getLocks();
        List<SkuWareHasStock> hasStocks = locks.stream().map((item) -> {
            // 获取有存库的仓库，然后锁定
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            // 查询该商品在哪些仓库有库存
            List<Long> wareIds = wareSkuDao.listWatrIdHasStock(skuId);
            stock.setWareIds(wareIds);
            return stock;
        }).collect(Collectors.toList());

        Boolean allLock = true;
        // 2、锁定库存
        for (SkuWareHasStock hasStock : hasStocks) {
            // 该商品默认锁失败
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareIds();
            Integer num = hasStock.getNum();
            if (wareIds == null && wareIds.size() == 0) {
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                // 锁定库存 -- 成功返回1，否则就是0
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, num);
                if(count == 1){
                    skuStocked = true;
                    break;
                }else{
                    // 锁失败：重试下一个仓库
                    continue;
                }
            }
            if(skuStocked == false){
                // 所有都没有锁住
                throw new NoStockException(skuId);
            }

        }


        // 全部锁成功
        return true;
    }


}
