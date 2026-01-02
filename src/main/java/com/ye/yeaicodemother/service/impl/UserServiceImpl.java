package com.ye.yeaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ye.yeaicodemother.exception.BusinessException;
import com.ye.yeaicodemother.exception.ErrorCode;
import com.ye.yeaicodemother.model.dto.user.UserQueryRequest;
import com.ye.yeaicodemother.model.entity.User;
import com.ye.yeaicodemother.mapper.UserMapper;
import com.ye.yeaicodemother.model.enums.UserRoleEnum;
import com.ye.yeaicodemother.model.vo.LoginUserVO;
import com.ye.yeaicodemother.model.vo.UserVO;
import com.ye.yeaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.ye.yeaicodemother.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author <a href="https://github.com/subaru-ye">程序员Ye</a>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 2. 检查是否重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        // 3. 加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     * 对用户原始密码进行加密。
     * 通过在原始密码前拼接固定的盐值 (SALT) 来增加密码的复杂度，然后使用MD5算法进行哈希加密。
     *
     * @param userPassword 用户输入的原始密码。
     * @return String 加密后的密码字符串（MD5哈希值）。
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "ye";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 用户实体 (User) 转换为登录用户视图对象 (LoginUserVO)。
     * 主要用于将用户信息脱敏后返回给前端。
     *
     * @param user 数据库中的用户实体对象。
     * @return LoginUserVO 脱敏后的用户视图对象；如果输入的 user 为 null，则返回 null。
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        // 1. 校验输入参数
        if (user == null) {
            return null;
        }

        // 2. 创建 VO 对象并复制属性
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);

        // 3. 返回转换后的 VO 对象
        return loginUserVO;
    }

    /**
     * 获取当前登录的用户信息
     * 首先从 Session 中获取用户信息，然后根据需要（例如需要最新数据）再次从数据库查询。
     *
     * @param request HTTP 请求对象，用于获取用户会话（Session）。
     * @return User 当前登录的用户实体对象。
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 从 Session 中获取已登录的用户信息
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;

        // 2. 校验 Session 中的用户信息是否有效
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 3. 从数据库查询最新的用户信息
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            // 数据库中找不到该用户，说明 Session 信息已失效
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 4. 返回从数据库获取的最新用户信息
        return currentUser;
    }

    /**
     * 用户实体 (User) 转换为用户视图对象 (UserVO)。
     * 主要用于将用户信息脱敏后返回给前端。
     *
     * @param user 数据库中的用户实体对象。
     * @return UserVO 脱敏后的用户视图对象；如果输入的 user 为 null，则返回 null。
     */
    @Override
    public UserVO getUserVO(User user) {
        // 1. 校验输入参数
        if (user == null) {
            return null;
        }

        // 2. 创建 VO 对象并复制属性
        UserVO userVO = new UserVO();
        // 使用工具类将 User 对象的公共属性值复制到 UserVO 对象中
        BeanUtil.copyProperties(user, userVO);

        // 3. 返回转换后的 VO 对象
        return userVO;
    }

    /**
     * 用户实体列表 (List<User>) 转换为用户视图对象列表 (List<UserVO>)。
     *
     * @param userList 数据库中的用户实体对象列表。
     * @return List<UserVO> 脱敏后的用户视图对象列表；如果输入的 userList 为空或为 null，则返回空的 ArrayList。
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        // 1. 校验输入参数
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }

        // 2. 遍历用户列表，逐个转换为 VO 对象，并收集结果
        return userList.stream()
                // 对列表中的每个 User 对象调用 getUserVO 方法进行转换
                .map(this::getUserVO)
                // 将转换后的流收集为 List<UserVO>
                .collect(Collectors.toList());
    }

    /**
     * 用户登录
     * 验证用户账号和密码，成功后将用户信息存入 session，并返回脱敏的用户信息。
     *
     * @param userAccount  用户输入的登录账号。
     * @param userPassword 用户输入的登录密码（明文）。
     * @param request      HTTP 请求对象，用于获取和设置用户会话（Session）。
     * @return LoginUserVO 登录成功后的用户视图对象，包含用户的基本信息。
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验输入参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        // 2. 对用户输入的密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 3. 查询数据库验证用户账号和加密后的密码
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        // 用户不存在或密码错误
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 4. 记录用户的登录状态到 Session 中
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        // 5. 获取并返回脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    /**
     * 用户登出
     * 清除 HTTP 请求 Session 中的用户登录信息。
     *
     * @param request HTTP 请求对象，用于获取和操作用户会话。
     * @return boolean 操作是否成功。
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1. 检查 Session 中是否存在登录态
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }

        // 2. 从 Session 中移除登录态信息
        request.getSession().removeAttribute(USER_LOGIN_STATE);

        // 3. 返回操作成功标志
        return true;
    }

    /**
     * 根据用户查询请求参数 (UserQueryRequest) 构建 MyBatis-Plus 的查询条件包装器 (QueryWrapper)。
     * 用于动态生成 SQL 查询语句。
     *
     * @param userQueryRequest 包含查询条件、分页和排序信息的请求对象。
     * @return QueryWrapper<MyBatis-Plus 的查询条件包装器。
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 1. 校验请求参数是否为空
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 2. 从请求对象中提取查询条件
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        // 3. 构建并返回查询条件包装器
        // 使用 QueryWrapper.create() 初始化，并链式调用条件方法
        return QueryWrapper.create()
                // 精确匹配 id
                .eq("id", id)
                // 精确匹配 userRole
                .eq("userRole", userRole)
                // 模糊匹配 userAccount (如果参数不为 null)
                .like("userAccount", userAccount)
                // 模糊匹配 userName (如果参数不为 null)
                .like("userName", userName)
                // 模糊匹配 userProfile (如果参数不为 null)
                .like("userProfile", userProfile)
                // 根据 sortField 和 sortOrder 添加排序规则
                // 如果 sortOrder 为 "ascend" 则升序，否则 (默认为 "descend") 降序
                .orderBy(sortField, "ascend".equals(sortOrder));
    }
}
