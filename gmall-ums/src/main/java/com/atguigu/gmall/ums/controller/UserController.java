package com.atguigu.gmall.ums.controller;

import java.util.List;

import com.baomidou.mybatisplus.extension.api.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import javax.annotation.PostConstruct;
import javax.validation.constraints.PastOrPresent;

/**
 * 用户表
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-03-26 09:42:59
 */
@Api(tags = "用户表 管理")
@RestController
@RequestMapping("ums/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("query")
    public ResponseVo<UserEntity> queryUser(@RequestParam("loginName") String loginName, @RequestParam("password") String password) {
        UserEntity userEntity = userService.queryUser(loginName, password);
        return ResponseVo.ok(userEntity);
    }

    /**
     * 用户注册
     *
     * @param userEntity
     * @param code
     * @return
     */
    @PostMapping("register")
    public ResponseVo register(UserEntity userEntity, @RequestParam("code") String code) {
        userService.register(userEntity, code);
        return ResponseVo.ok();
    }

    /**
     * 发送短信验证码
     *
     * @param phone
     * @return
     */
    @PostMapping("code")
    public ResponseVo verificationCode(@RequestParam("phone") String phone) {
        userService.verificationCode(phone);
        return ResponseVo.ok();
    }

    @GetMapping("check/{data}/{type}")
    public Boolean checkData(@PathVariable("data") String data, @PathVariable("type") Integer type) {
        return userService.checkData(data, type);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryUserByPage(PageParamVo paramVo) {
        PageResultVo pageResultVo = userService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<UserEntity> queryUserById(@PathVariable("id") Long id) {
        UserEntity user = userService.getById(id);

        return ResponseVo.ok(user);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody UserEntity user) {
        userService.save(user);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody UserEntity user) {
        userService.updateById(user);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids) {
        userService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
