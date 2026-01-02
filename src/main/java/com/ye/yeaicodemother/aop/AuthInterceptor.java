package com.ye.yeaicodemother.aop;

import com.ye.yeaicodemother.annotation.AuthCheck;
import com.ye.yeaicodemother.exception.BusinessException;
import com.ye.yeaicodemother.exception.ErrorCode;
import com.ye.yeaicodemother.model.entity.User;
import com.ye.yeaicodemother.model.enums.UserRoleEnum;
import com.ye.yeaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 认证和授权拦截器 (AOP切面)
 * 使用 AOP 技术，在标记了 @AuthCheck 注解的方法执行前，进行用户身份认证和权限校验。
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 环绕通知，用于执行权限校验逻辑。
     * 在目标方法执行前后进行拦截，根据 @AuthCheck 注解的配置进行权限判断。
     *
     * @param joinPoint 切入点，代表被拦截的方法。
     * @param authCheck 应用到目标方法上的权限校验注解。
     * @return Object 目标方法执行后的返回值。
     * @throws Throwable 如果权限校验失败或目标方法执行出错，则抛出异常。
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1. 获取注解中定义的所需角色
        String mustRole = authCheck.mustRole();

        // 2. 获取当前请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 3. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 4. 解析注解中定义的角色枚举
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        // 5. 如果注解未指定特定角色（即为默认值或空），则直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        // 6. 检查当前用户的角色
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());

        // 7. 如果当前用户没有有效的角色，则拒绝访问
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 8. 如果要求是管理员角色，但当前用户不是管理员，则拒绝访问
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 9. 权限校验通过，继续执行目标方法
        return joinPoint.proceed();
    }
}