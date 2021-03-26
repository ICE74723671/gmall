package com.atguigu.gmall.pms.mapper;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchyInUncheckedCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CategoryMapperTest {

    @Autowired
    private CategoryMapper categoryMapper;

    @Test
    void queryLvl2CatesWithSubsByPid() {
        System.out.println(this.categoryMapper.queryLvl2CatesWithSubsByPid(1l));
    }
}
