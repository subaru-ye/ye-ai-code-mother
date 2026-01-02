package com.ye.yeaicodemother.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户添加请求 DTO (Data Transfer Object)
 * 用于封装管理员添加新用户时提交的表单数据。
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户头像 URL
     */
    private String userAvatar;

    /**
     * 用户简介或个人说明
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private String userRole;

    @Serial
    private static final long serialVersionUID = 1L;
}