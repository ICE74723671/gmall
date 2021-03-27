package com.atguigu.gmall.ums.service.impl;


import com.atguigu.gmall.ums.feign.ServiceSmsClient;
import com.atguigu.gmall.ums.listener.ConsumerListener;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ServiceSmsClient serviceSmsClient;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        switch (type) {
            case 1:
                queryWrapper.eq("username", data);
                break;
            case 2:
                queryWrapper.eq("phone", data);
                break;
            case 3:
                queryWrapper.eq("email", data);
                break;
            default:
                return null;
        }
        return this.count(queryWrapper) == 0;
    }

    @Override
    public void verificationCode(String phone) {
        serviceSmsClient.verificationCode(phone);
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        //核对验证码
        if (!StringUtils.equals((String) redisTemplate.opsForValue().get("code:regist:" + userEntity.getPhone()), code)) {
            System.out.println("验证码不正确");
            return;
        }
        //生成盐
        String salt = UUID.randomUUID().toString().substring(0, 6);
        userEntity.setSalt(salt);

        //密码加盐加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() + salt));

        //设置用户参数
        userEntity.setLevelId(1L);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(1);
        userEntity.setCreateTime(new Date());
        this.save(userEntity);

        //删除验证码
        Set keys = redisTemplate.keys("code:regist:" + userEntity.getPhone());
        if (!CollectionUtils.isEmpty(keys)) {
            redisTemplate.delete("code:regist:" + userEntity.getPhone());
        }
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username", loginName)
                .or().eq("phone", loginName)
                .or().eq("email", loginName);
        List<UserEntity> userEntities = baseMapper.selectList(wrapper);

        for (UserEntity userEntity : userEntities) {
            String pwd = userEntity.getPassword();
            //明文加密
            String upwd = DigestUtils.md5Hex(password + userEntity.getSalt());
            if (StringUtils.equals(pwd, upwd)) {
                return userEntity;
            }
        }

        return null;
    }

}