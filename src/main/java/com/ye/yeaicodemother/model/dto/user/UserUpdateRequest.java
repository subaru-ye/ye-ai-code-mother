package com.ye.yeaicodemother.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户更新请求 DTO (Data Transfer Object)
 * 用于封装更新用户信息时提交的表单数据。
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * 用户在数据库中的唯一 id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像 URL
     */
    private String userAvatar;

    /**
     * 用户简介或个人说明
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    @Serial
    private static final long serialVersionUID = 1L;
}