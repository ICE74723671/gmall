package com.atguigu.gmall.search.pojo;

import io.swagger.models.auth.In;
import lombok.Data;

import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/15 in 21:05
 */
@Data
public class SearchParamVo {

    private String keyword; //搜索框中的搜索条件

    private List<Long> brandId; //品牌id，可以多选要用集合

    private Long cid; //分类id(手机)

    private List<String> props; //规格参数检索

    private Integer sort = 0; //排序字段：0-默认，得分降序；1-按价格升序；2-按价格降序；3-按创建时间降序；4-按销量降序

    //价格区间
    private Double priceForm;
    private Double priceTo;

    private Integer pageNum = 1;//页数

    private final Integer pageSize = 20;//每页记录数，固定不改

    private Boolean store;//是否有货
}
