package com.lif314.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.lif314.common.constant.EsConstant;
import com.lif314.common.to.es.SkuEsModel;
import com.lif314.gulimall.search.config.GulimallElasticSearchConfig;
import com.lif314.gulimall.search.service.SearchService;
import com.lif314.gulimall.search.vo.SearchParam;
import com.lif314.gulimall.search.vo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service("SearchService")
public class SearchServiceImpl implements SearchService {

    @Autowired
    RestHighLevelClient client;

    // 在ES中进行检索
    @Override
    public SearchResult search(SearchParam searchParam) {
        // 1、动态构建出查询需要的DSL语句

        // 1、准备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);


        SearchResult result = null;
        try {
            // 2、执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

            // 3、分析检索响应
            result = buildSearchResult(response, searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 构建检索请求
     * 模糊匹配，过滤(按照属性、分类、品牌、价格区间、库存); 排序、分页；高亮，聚合分析
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        /**
         * 模糊匹配，过滤(按照属性、分类、品牌、价格区间、库存)
         */
        // 构建bool query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 1.1 bool-must  模糊匹配 skuTitle
        if (!StringUtils.isEmpty(param.getKeyword())) {
            // 搜索查询
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        // 1.2 bool-filter 按照属性、分类、品牌、价格区间、库存
        if (param.getCatalog3Id() != null) {
            // 三级分类
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // 1.2 bool-filter 按照品牌id查询
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            // 品牌Id查询
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // TODO 1.2 bool-filter 按照属性查询
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {
                // 每一组属性查询构建一个nestedQuery
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                // attrs=1_5寸:6存&attrs=2_166:87
                String[] s = attrStr.split("_");
                String attrId = s[0]; // 检索属性id
                String[] attrValues = s[1].split(":");  // 这个属性的检索值
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                // 每一个属性必须生成一个nested查询 -- 不参与评分
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        // 1.2 bool-filter 按照是否有库存进行查询 0无库存  1有库存 默认为1
        boolQuery.filter(QueryBuilders.termsQuery("hasStock", param.getHasStock() == 1));
        // 1.2 bool-filter 按照价格区间 1_500/_500/500_
        if (StringUtils.isNotEmpty(param.getSkuPrice())) {
            // 1_500/_500/500_   组装range  gte/lte
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            // 分割参数
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                // 区间查询
                rangeQuery.gte(s[0]).lte(s[1]);
            } else if (s.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    // 小于
                    rangeQuery.lte(s[0]);
                } else {
                    // 大于
                    rangeQuery.gte(s[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }

        // 模糊匹配加入查询条件
        sourceBuilder.query(boolQuery);

        /**
         * 排序、分页；高亮
         */
        // 2.1 排序
        if(StringUtils.isNotEmpty(param.getSort())){
            /**
             * 排序：sort=saleCount_asc  sort=hotScore_asc  sort=skuPrice_asc
             *      sort=saleCount_desc  sort=hotScore_desc sort=skuPrice_desc
             */
            String sort = param.getSort();
            String[] s = sort.split("_");
            // 顺序
            SortOrder sortOrder = s[1].equalsIgnoreCase("asc")?SortOrder.ASC:SortOrder.DESC;
            sourceBuilder.sort(s[0], sortOrder);
        }
        // 2.2 分页
        // pageNum:  1 from: 0 size:5 [0,1,2,3,4]
        // from = (pageNum-1)*size
        sourceBuilder.from((param.getPageNum()-1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 2.3 高亮
        if(StringUtils.isNotEmpty(param.getKeyword())){
            // 只有模糊查询才有高亮
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }

        /**
         * 聚合分析
         */
        // 3.1 品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        // 品牌聚合子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);

        // 3.2 分类聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(2);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);

        // 3.3 属性聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        NestedAggregationBuilder attr_id_agg = attr_agg.subAggregation(AggregationBuilders.terms("attr_id_agg").field("attrs.attrId"));
        // 聚合分析出当前attr_id对应的属性名字和对应的所有可能属性值attrValues
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(attr_agg);

        return new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
    }

    /**
     * 分析检索结果--封装数据
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();

        // 1. 返回查询到的所有商品
        // 总记录数
        SearchHits hits = response.getHits();
        List<SkuEsModel> skuEsModelList = new ArrayList<>();
        if(hits.getHits() != null && hits.getHits().length > 0){
            for (SearchHit hit : hits.getHits()) {
                // 获取每一条记录
                String sourceAsString = hit.getSourceAsString();   // _source
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);

                // 判断高亮信息
                if(StringUtils.isNotEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String newTitle = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(newTitle);
                }
                skuEsModelList.add(skuEsModel);
            }
        }
        result.setProducts(skuEsModelList);


        // 所有聚合信息
        Aggregations aggregations = response.getAggregations();
        // 2. 当前商品涉及的分类信息
        ParsedLongTerms catalog_agg = aggregations.get("catalog_agg");
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        List<SearchResult.CatalogVo> catalogVos  =  new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            // 获取分类id和名字
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            catalogVo.setCatalogId(bucket.getKeyAsNumber().longValue());
            // 获取分类名字
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        // 3. 当前所有商品涉及到的品牌信息
        ParsedLongTerms brand_agg = aggregations.get("brand_agg");
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            // 获取品牌的id 名字  图片
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            // id
            long brandId = bucket.getKeyAsNumber().longValue();
            // 图片
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            // 名字
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();

            brandVo.setBrandId(brandId);
            brandVo.setBrandImg(brandImg);
            brandVo.setBrandName(brandName);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);


        // 4. 当前商品涉及的所有属性信息
        ParsedNested attr_agg = aggregations.get("attr_agg");
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg ");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo =  new SearchResult.AttrVo();
            // 属性id
            long attr_id = bucket.getKeyAsNumber().longValue();
            // 属性名字
            String attr_name = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            // 属性所有值
            List<String> attr_values = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map((item) -> {
                return item.getKeyAsString();
            }).collect(Collectors.toList());

            // 设置
            attrVo.setAttrId(attr_id);
            attrVo.setAttrName(attr_name);
            attrVo.setAttrValue(attr_values);
            // 加入集合
            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);

        // ===========以上是聚合信息获取========
        // 5. 分页信息
        // 5.1 页码
        result.setPageNum(param.getPageNum());
        // 5.2 总记录数
        long totalHits = hits.getTotalHits().value;
        result.setTotal(totalHits);
        // 5.3 总页码
        int totalPages = (totalHits%EsConstant.PRODUCT_PAGESIZE == 0? (int) totalHits/EsConstant.PRODUCT_PAGESIZE : ((int)totalHits/EsConstant.PRODUCT_PAGESIZE + 1));
        result.setTotalPages(totalPages);

        return result;
    }

}
