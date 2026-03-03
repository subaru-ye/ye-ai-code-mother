package com.ye.yeaicodemother.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户更新本人信息请求 DTO
 * 仅允许更新个人资料类字段（不包含角色、密码等敏感信息）。
 */
@Data
public class UserUpdateMyRequest implements Serializable {

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

    @Serial
    private static final long serialVersionUID = 1L;
}

