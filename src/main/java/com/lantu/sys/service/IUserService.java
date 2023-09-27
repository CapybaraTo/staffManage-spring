package com.lantu.sys.service;

import com.lantu.sys.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Capybara
 * @since 2023-09-26
 */
public interface IUserService extends IService<User> {

    Map<String, Object> login(User user);
}
