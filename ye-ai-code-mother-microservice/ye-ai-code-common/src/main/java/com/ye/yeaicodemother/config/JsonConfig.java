package com.ye.yeaicodemother.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Spring MVC JSON 配置类
 * 用于配置 Jackson JSON 序列化和反序列化的行为。
 */
@JsonComponent
public class JsonConfig {

    /**
     * 配置 ObjectMapper Bean，解决前端 JavaScript 处理 Long 型 ID 时精度丢失的问题。
     * 该配置会将 Java 的 Long 类型序列化为 JSON 字符串，而不是数字，以避免 JS Number 类型的精度限制。
     *
     * @param builder Jackson2ObjectMapperBuilder，用于构建 ObjectMapper。
     * @return ObjectMapper 配置后的 ObjectMapper 实例。
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // 1. 使用 builder 构建 ObjectMapper 实例
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // 2. 创建一个自定义模块来添加序列化器
        SimpleModule module = new SimpleModule();

        // 3. 为 Long 类和 long 基本类型添加 ToStringSerializer
        // 这样可以将 Long 值序列化为字符串，避免前端精度丢失
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 4. 将自定义模块注册到 ObjectMapper 中
        objectMapper.registerModule(module);

        // 5. 返回配置完成的 ObjectMapper
        return objectMapper;
    }
}