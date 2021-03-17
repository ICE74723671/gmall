package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.filter.IFilterConfig;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.vo.SearchResponseAttrVo;
import com.atguigu.gmall.search.vo.SearchResponseVo;
import io.swagger.models.auth.In;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * description:
 *
 * @author Ice on 2021/3/15 in 21:16
 */
@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParamVo paramVo) {
        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, buildDsl(paramVo));
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析结果集
            SearchResponseVo responseVo = parseResult(response);
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());
//            System.out.println(responseVo);
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析搜索结果集
     *
     * @param response
     * @return
     */
    private SearchResponseVo parseResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();

        SearchHits hits = response.getHits();
        //总命中记录数
        responseVo.setTotal(hits.getTotalHits());
        SearchHit[] hitsHits = hits.getHits();
        //goods数据
        List<Goods> goodsList = Stream.of(hitsHits).map(hitsHit -> {
            //获取内层的_source数据
            String goodJson = hitsHit.getSourceAsString();
            //反序列化为goods
            Goods goods = JSON.parseObject(goodJson, Goods.class);
            //获取高亮覆盖
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField title = highlightFields.get("title");
            goods.setTitle(title.getFragments()[0].toString());
            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        //聚合结果集的解析
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();

        //1.解析品牌聚合
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)) {
            List<BrandEntity> brands = buckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                Long brandId = bucket.getKeyAsNumber().longValue();
                //品牌ID
                brandEntity.setId(brandId);

                Map<String, Aggregation> brandAggMap = bucket.getAggregations().asMap();
                //解析品牌名称的子聚合
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandAggMap.get("brandNameAgg");
                List<? extends Terms.Bucket> brandNameBuckets = brandNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(brandNameBuckets)) {
                    brandEntity.setName(brandNameBuckets.get(0).getKeyAsString());
                }
                //解析品牌logo的子聚合
                ParsedStringTerms logoAgg = (ParsedStringTerms) brandAggMap.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoAggBuckets)) {
                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                }
                return brandEntity;
            }).collect(Collectors.toList());
            responseVo.setBrands(brands);
        }

        //2.解析分类聚合
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)) {
            List<CategoryEntity> categoryEntityList = categoryIdAggBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                //分类ID
                categoryEntity.setId(bucket.getKeyAsNumber().longValue());
                //解析分类名称子聚合
                ParsedStringTerms categoryNameAgg = bucket.getAggregations().get("categoryNameAgg");
                categoryEntity.setName(categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return categoryEntity;
            }).collect(Collectors.toList());
            responseVo.setCategories(categoryEntityList);
        }

        //3.解析规格参数聚合
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)) {
            List<SearchResponseAttrVo> filters = attrIdAggBuckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                //规格参数ID
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                //解析规格参数名称子聚合
                ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                //解析规格参数属性子聚合
                ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(attrValueAggBuckets)) {
                    List<String> attrValues = attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                    searchResponseAttrVo.setAttrValues(attrValues);
                }
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(filters);
        }

        return responseVo;
    }

    /**
     * 构建查询DSL语句
     */
    private SearchSourceBuilder buildDsl(SearchParamVo paramVo) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        String keyword = paramVo.getKeyword();
        if (StringUtils.isEmpty(keyword)) {
            //TODO，搜索条件为空可以显示广告
            return null;
        }

        //1.构建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1. 匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));

        //1.2 过滤
        //1.2.1 品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }
        //1.2.2 分类过滤
        Long cid = paramVo.getCid();
        if (cid != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("categoryId", cid));
        }
        //1.2.3 价格区间过滤
        Double priceForm = paramVo.getPriceForm();
        Double priceTo = paramVo.getPriceTo();
        if (priceForm != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (priceForm != null) {
                rangeQuery.gte(priceForm);
            }
            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }
        //1.2.4 是否有货
        Boolean store = paramVo.getStore();
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }

        //1.2.5 规格参数的过滤，比如props=5:高通-麒麟&props=6:骁龙865-硅谷1000
        List<String> props = paramVo.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(prop -> {
                String[] attrs = StringUtils.split(prop, ":");
                if (attrs != null && attrs.length == 2) {
                    String attrId = attrs[0];
                    String attrValueString = attrs[1];
                    String[] attrValues = StringUtils.split(attrValueString, "-");

                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrId));
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));

                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                }
            });
        }
        sourceBuilder.query(boolQueryBuilder);

        //2.构建排序 0-默认，得分降序；1-按价格升序；2-按价格降序；3-按创建时间降序；4-按销量降序
        Integer sort = paramVo.getSort();
        String field = "";
        SortOrder order = null;
        switch (sort) {
            case 1:
                field = "price";
                order = SortOrder.ASC;
                break;
            case 2:
                field = "price";
                order = SortOrder.DESC;
                break;
            case 3:
                field = "createTime";
                order = SortOrder.DESC;
                break;
            case 4:
                field = "sales";
                order = SortOrder.DESC;
                break;
            default:
                field = "_score";
                order = SortOrder.DESC;
                break;
        }
        sourceBuilder.sort(field, order);

        //3.构建分页
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);

        //4.构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style='color:red'>").postTags("</font>"));

        //5.构建聚合
        //5.1 构建品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));

        //5.2 构建分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));

        //5.3 构建规格参数聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));

        //6. 构建结果集过滤
        sourceBuilder.fetchSource(new String[]{"skuId", "title", "price", "defaultImage"}, null);

//        System.out.println(sourceBuilder.toString());
        return sourceBuilder;
    }
}
