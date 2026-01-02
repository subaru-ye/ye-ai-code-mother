package com.ye.yeaicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.ye.yeaicodemother.annotation.AuthCheck;
import com.ye.yeaicodemother.common.BaseResponse;
import com.ye.yeaicodemother.common.DeleteRequest;
import com.ye.yeaicodemother.common.ResultUtils;
import com.ye.yeaicodemother.constant.UserConstant;
import com.ye.yeaicodemother.exception.BusinessException;
import com.ye.yeaicodemother.exception.ErrorCode;
import com.ye.yeaicodemother.exception.ThrowUtils;
import com.ye.yeaicodemother.model.dto.user.*;
import com.ye.yeaicodemother.model.entity.User;
import com.ye.yeaicodemother.model.vo.LoginUserVO;
import com.ye.yeaicodemother.model.vo.UserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.ye.yeaicodemother.service.UserService;

import java.util.List;

/**
 * 用户 控制层。
 *
 * @author <a href="https://github.com/subaru-ye">程序员Ye</a>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 1. 校验请求对象是否为空
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 提取请求参数
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        // 3. 调用服务层进行注册
        long result = userService.userRegister(userAccount, userPassword, checkPassword);

        // 4. 返回成功响应，携带新用户ID
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     * 接收用户登录请求，验证账号密码后返回登录结果和用户信息。
     *
     * @param userLoginRequest 用户登录请求体，包含账号和密码。
     * @param request          HTTP请求对象，用于处理用户会话。
     * @return BaseResponse<LoginUserVO> 包含登录成功后用户信息的统一响应。
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 1. 校验请求对象是否为空
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 提取请求参数
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        // 3. 调用服务层进行登录逻辑处理
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);

        // 4. 返回成功响应，携带登录用户信息
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户的信息
     * 从请求中获取登录态，然后返回脱敏后的用户信息。
     *
     * @param request HTTP 请求对象，用于获取用户会话。
     * @return BaseResponse<LoginUserVO> 包含当前登录用户信息的统一响应。
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        // 1. 从请求中获取当前登录的用户实体
        User loginUser = userService.getLoginUser(request);

        // 2. 将用户实体转换为脱敏的视图对象
        LoginUserVO loginUserVO = userService.getLoginUserVO(loginUser);

        // 3. 返回成功响应，携带脱敏后的用户信息
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户登出
     * 接收登出请求，清除用户登录状态。
     *
     * @param request HTTP 请求对象，用于处理用户会话。
     * @return BaseResponse<Boolean> 包含登出操作结果的统一响应。
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        // 1. 校验请求对象是否为空
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        // 2. 调用服务层执行登出逻辑
        boolean result = userService.userLogout(request);

        // 3. 返回成功响应，携带操作结果
        return ResultUtils.success(result);
    }

    /**
     * 创建用户（仅管理员）
     * 管理员可以添加新用户，新用户将使用默认密码。
     *
     * @param userAddRequest 用户添加请求体。
     * @return BaseResponse<Long> 包含新创建用户ID的统一响应。
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        // 1. 校验请求参数
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 将请求参数复制到用户实体
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);

        // 3. 设置默认密码并加密
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);

        // 4. 保存用户到数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 5. 返回新创建用户的ID
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户详细信息（仅管理员）
     * 管理员可以查看任意用户的完整信息。
     *
     * @param id 用户的唯一标识ID。
     * @return BaseResponse<User> 包含用户完整信息的统一响应。
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        // 1. 校验ID参数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 2. 从数据库查询用户
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        // 3. 返回用户信息
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取用户脱敏信息
     * 返回给前端用户的基本信息（脱敏后）。
     *
     * @param id 用户的唯一标识ID。
     * @return BaseResponse<UserVO> 包含用户脱敏信息的统一响应。
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        // 1. 调用管理员接口获取用户信息（此接口会校验权限）
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();

        // 2. 将用户实体转换为脱敏的VO对象
        UserVO userVO = userService.getUserVO(user);

        // 3. 返回脱敏后的用户信息
        return ResultUtils.success(userVO);
    }

    /**
     * 删除用户（仅管理员）
     * 管理员可以删除指定ID的用户。
     *
     * @param deleteRequest 包含要删除用户ID的请求体。
     * @return BaseResponse<Boolean> 包含删除操作结果的统一响应。
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        // 1. 校验请求参数
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 执行删除操作
        boolean b = userService.removeById(deleteRequest.getId());

        // 3. 返回操作结果
        return ResultUtils.success(b);
    }

    /**
     * 更新用户信息（仅管理员）
     * 管理员可以更新指定ID的用户信息。
     *
     * @param userUpdateRequest 包含更新信息的用户请求体。
     * @return BaseResponse<Boolean> 包含更新操作结果的统一响应。
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 1. 校验请求参数
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 将请求参数复制到用户实体
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);

        // 3. 执行更新操作
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 4. 返回操作成功标志
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户列表（脱敏，仅管理员）
     * 管理员可以分页查看所有用户的基本信息（脱敏后）。
     *
     * @param userQueryRequest 查询请求参数，包含筛选条件和分页信息。
     * @return BaseResponse<Page < UserVO>> 包含分页用户脱敏信息列表的统一响应。
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 1. 校验请求参数
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 提取分页参数
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();

        // 3. 构建查询条件并执行分页查询
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));

        // 4. 数据脱敏：将查询到的用户实体列表转换为VO列表
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());

        // 5. 构建并返回包含VO列表的分页响应
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}

