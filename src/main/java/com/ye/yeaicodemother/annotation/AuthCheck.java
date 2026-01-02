package com.ye.yeaicodemother.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于方法级别的权限校验。
 * 标记在需要进行权限检查的控制器方法上。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 定义访问该方法所必需的角色。
     * 如果不设置或设置为空字符串，则可能表示无特殊角色要求或需要登录即可。
     */
    String mustRole() default "";
}