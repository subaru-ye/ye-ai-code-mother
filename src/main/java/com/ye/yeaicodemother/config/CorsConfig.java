package com.ye.yeaicodemother.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置类
 * 用于配置全局的CORS（跨源资源共享）策略，允许前端应用与后端API进行跨域通信。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 配置CORS映射规则。
     * 定义了允许跨域请求的路径、源、方法、请求头等。
     *
     * @param registry CORS注册器，用于添加和管理CORS映射。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        registry.addMapping("/**")
                // 允许发送 Cookie
                .allowCredentials(true)
                // 放行哪些域名（必须用 patterns，否则 * 会和 allowCredentials 冲突）
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }
}