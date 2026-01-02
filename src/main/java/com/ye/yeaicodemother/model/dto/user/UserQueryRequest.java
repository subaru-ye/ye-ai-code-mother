package com.ye.yeaicodemother.model.dto.user;

import com.ye.yeaicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户查询请求 DTO (Data Transfer Object)
 * 继承了分页请求参数，用于封装用户列表查询时的筛选条件和分页信息。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户在数据库中的唯一 id
     */
    private Long id;

    /**
     * 用户昵称（用于模糊查询）
     */
    private String userName;

    /**
     * 用户账号（用于模糊查询）
     */
    private String userAccount;

    /**
     * 用户简介或个人说明（用于模糊查询）
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban（用于精确查询）
     */
    private String userRole;

    @Serial
    private static final long serialVersionUID = 1L;
}