package com.atguigu.gmall.search.repository;

import com.atguigu.gmall.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * description:
 *
 * @author Ice on 2021/3/15 in 18:17
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {

}
