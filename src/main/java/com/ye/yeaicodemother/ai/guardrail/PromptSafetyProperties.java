package com.ye.yeaicodemother.ai.guardrail;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 绑定 Nacos 中 prompt-safety-config.yaml 的配置内容
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "prompt-safety")
@Slf4j
public class PromptSafetyProperties {

    private boolean enabled = true;
    private int maxLength = 1000;
    private List<String> sensitiveWords;
    private List<String> injectionPatterns;

    @PostConstruct
    public void logConfig() {
        log.info("✅ PromptSafetyProperties loaded: enabled={}, maxLength={}, sensitiveWords={}",
                enabled, maxLength, sensitiveWords);
    }

}