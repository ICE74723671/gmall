package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SpuEntity;

import java.io.FileNotFoundException;
import java.util.Map;

/**
 * spu信息
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-03-08 14:58:56
 */
public interface SpuService extends IService<SpuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    PageResultVo querySpuByCidOrKeyPage(PageParamVo paramVo, Long cid);

    void bigSave(SpuVo spu) throws FileNotFoundException;


}

