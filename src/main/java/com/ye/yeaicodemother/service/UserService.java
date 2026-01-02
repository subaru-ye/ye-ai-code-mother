package com.ye.yeaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.ye.yeaicodemother.model.dto.user.UserQueryRequest;
import com.ye.yeaicodemother.model.entity.User;
import com.ye.yeaicodemother.model.vo.LoginUserVO;
import com.ye.yeaicodemother.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author <a href="https://github.com/subaru-ye">程序员Ye</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 对用户原始密码进行加密。
     * 通过在原始密码前拼接固定的盐值 (SALT) 来增加密码的复杂度，然后使用MD5算法进行哈希加密。
     *
     * @param userPassword 用户输入的原始密码。
     * @return String 加密后的密码字符串（MD5哈希值）。
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户实体 (User) 转换为登录用户视图对象 (LoginUserVO)。
     * 主要用于将用户信息脱敏后返回给前端。
     *
     * @param user 数据库中的用户实体对象。
     * @return LoginUserVO 脱敏后的用户视图对象；如果输入的 user 为 null，则返回 null。
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录的用户信息
     * 首先从 Session 中获取用户信息，然后根据需要（例如需要最新数据）再次从数据库查询。
     *
     * @param request HTTP 请求对象，用于获取用户会话（Session）。
     * @return User 当前登录的用户实体对象。
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户实体 (User) 转换为用户视图对象 (UserVO)。
     * 主要用于将用户信息脱敏后返回给前端。
     *
     * @param user 数据库中的用户实体对象。
     * @return UserVO 脱敏后的用户视图对象；如果输入的 user 为 null，则返回 null。
     */
    UserVO getUserVO(User user);

    /**
     * 用户实体列表 (List<User>) 转换为用户视图对象列表 (List<UserVO>)。
     *
     * @param userList 数据库中的用户实体对象列表。
     * @return List<UserVO> 脱敏后的用户视图对象列表；如果输入的 userList 为空或为 null，则返回空的 ArrayList。
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户登录
     * 验证用户账号和密码，成功后将用户信息存入 session，并返回脱敏的用户信息。
     *
     * @param userAccount  用户输入的登录账号。
     * @param userPassword 用户输入的登录密码（明文）。
     * @param request      HTTP 请求对象，用于获取和设置用户会话（Session）。
     * @return LoginUserVO 登录成功后的用户视图对象，包含用户的基本信息。
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户登出
     * 清除 HTTP 请求 Session 中的用户登录信息。
     *
     * @param request HTTP 请求对象，用于获取和操作用户会话。
     * @return boolean 操作是否成功。
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 根据用户查询请求参数 (UserQueryRequest) 构建 MyBatis-Plus 的查询条件包装器 (QueryWrapper)。
     * 用于动态生成 SQL 查询语句。
     *
     * @param userQueryRequest 包含查询条件、分页和排序信息的请求对象。
     * @return QueryWrapper<MyBatis-Plus 的查询条件包装器。
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}
