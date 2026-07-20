package com.fs.service;

import com.fs.dto.UserLoginDTO;
import com.fs.entity.User;

public interface UserService {

    /***
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
