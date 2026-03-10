package com.ye.yeaicodemother.aop;

import com.ye.yeaicodemother.annotation.AuthCheck;
import com.ye.yeaicodemother.exception.BusinessException;
import com.ye.yeaicodemother.exception.ErrorCode;
import com.ye.yeaicodemother.innerservice.InnerUserService;
import com.ye.yeaicodemother.model.entity.User;
import com.ye.yeaicodemother.model.enums.UserRoleEnum;
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
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User loginUser = InnerUserService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限，直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        // 以下的代码：必须有这个权限才能通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有权限，直接拒绝
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求必须有管理员权限，但当前登录用户没有
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过普通用户的权限校验，放行
        return joinPoint.proceed();
    }
}

























