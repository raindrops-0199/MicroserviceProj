package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;

public interface ApUserService extends IService<ApUser> {

    /**
     * app登录功能
     * @param dto LoginDto
     * @return 通用返回类ResponseResult
     */
    public ResponseResult login(LoginDto dto);
}
