package com.ye.yeaicodemother.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录请求 DTO (Data Transfer Object)
 * 用于封装用户登录时提交的账号和密码信息。
 */
@Data
public class UserLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户登录账号
     */
    private String userAccount;

    /**
     * 用户登录密码
     */
    private String userPassword;
}