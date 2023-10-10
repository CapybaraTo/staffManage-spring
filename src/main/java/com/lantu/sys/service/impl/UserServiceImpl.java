package com.lantu.sys.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lantu.sys.entity.User;
import com.lantu.sys.mapper.UserMapper;
import com.lantu.sys.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.TimeoutUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Capybara
 * @since 2023-09-26
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> login(User user) {
        // 根据用户名查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername,user.getUsername());
        User loginUser = this.baseMapper.selectOne(wrapper);
        // 结果不为空，并且密码和传入密码匹配，则生成token，并将用户信息存入redis
        if(loginUser != null && passwordEncoder.matches(user.getPassword(),loginUser.getPassword())){
            // 暂时用UUID, 终极方案是jwt
            String key = "user:" + UUID.randomUUID();

            // 存入redis
            loginUser.setPassword(null);
            redisTemplate.opsForValue().set(key,loginUser,30, TimeUnit.MINUTES);

            // 返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token",key);
            return data;
        }
        return null;
    }


//    @Override
//    public Map<String,Object> login(User user){
//        //首先是查询，根据用户名和密码做一个查询
//        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(User::getUsername,user.getUsername());
//        wrapper.eq(User::getPassword,user.getPassword());
//        User loginUser = this.baseMapper.selectOne(wrapper);
//        //结果不为空则需要生成一个token,并将我们的用户信息存入redis
//        if(loginUser != null){
//            //暂时用uuid,终极方案是jwt
//            String key = "user:" + UUID.randomUUID();
//            //存入redis查询token
//            loginUser.setPassword(null);  //后面还需加密处理
//            redisTemplate.opsForValue().set(key,loginUser,30, TimeUnit.MINUTES);//默认登录后是永久有效，但一般登录是有时效的.30min
//
//            //返回数据
//            Map<String,Object> data = new HashMap<>();
//            data.put("token",key);
//            return data;
//        }
//        return null;
//
//    }

    @Override
    public Map<String,Object> getUserInfo(String token){
        //从redis查询token
        Object obj = redisTemplate.opsForValue().get(token);
        //反序列化
        User user = JSON.parseObject(JSON.toJSONString(obj),User.class);
        if(obj != null){
            Map<String,Object> data = new HashMap<>();
            data.put("name",user.getUsername());
            data.put("avatar",user.getAvatar());
            //角色  能单表查询就单表查询  关联查询效率非常低
            List<String> roleList = this.getBaseMapper().getRoleNameByUserId(user.getId());
            data.put("roles",roleList);
            return data;

        }
        return null;
    }

    @Override
    public void logout(String token) {
        redisTemplate.delete(token);
    }
}
