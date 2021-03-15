package com.atguigu.elasticsearchdemo.repository;

import com.atguigu.elasticsearchdemo.pojo.User;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * description:
 *
 * @author Ice on 2021/3/13 in 8:37
 */
public interface ElasticSearchRepository extends ElasticsearchRepository<User, Long> {

    List<User> findByAgeBetween(Integer age1, Integer age2);
}
