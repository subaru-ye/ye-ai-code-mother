package com.ye.yeaicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户视图对象 (View Object)
 * 用于封装返回给前端的用户信息，通常用于列表展示等场景，不包含敏感信息如密码。
 */
@Data
public class UserVO implements Serializable {

    /**
     * 用户在数据库中的唯一 id
     */
    private Long id;
    
    /**
     * 用户登录账号
     */
    private String userAccount;

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

    /**
     * 用户记录的创建时间
     */
    private LocalDateTime createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}