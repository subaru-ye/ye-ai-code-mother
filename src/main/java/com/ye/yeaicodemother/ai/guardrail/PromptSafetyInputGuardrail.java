package com.ye.yeaicodemother.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 基于 Nacos 动态配置的 Prompt 安全输入护轨
 */
@Slf4j
@Component
public class PromptSafetyInputGuardrail implements InputGuardrail {

    private final PromptSafetyProperties properties;

    // volatile 保证多线程下可见性
    private volatile List<Pattern> compiledInjectionPatterns = List.of();

    public PromptSafetyInputGuardrail(PromptSafetyProperties properties) {
        this.properties = properties;
        recompilePatterns();
    }

    /**
     * 重新编译正则表达式（在构造或配置刷新后调用）
     */
    private void recompilePatterns() {
        try {
            this.compiledInjectionPatterns = properties.getInjectionPatterns() == null ?
                    List.of() :
                    properties.getInjectionPatterns().stream()
                            .map(Pattern::compile)
                            .collect(Collectors.toList());
            log.info("✅ Prompt 安全规则加载成功 | 敏感词数量: {}, 正则数量: {}",
                    properties.getSensitiveWords() != null ? properties.getSensitiveWords().size() : 0,
                    compiledInjectionPatterns.size());
        } catch (Exception e) {
            log.error("⚠️ 正则表达式编译失败，使用空规则集", e);
            this.compiledInjectionPatterns = List.of();
        }
    }

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        if (!properties.isEnabled()) {
            return success(); // 护轨关闭
        }

        String input = userMessage.singleText();

        // 1. 长度校验
        if (input.length() > properties.getMaxLength()) {
            return fatal("输入内容过长，不要超过 " + properties.getMaxLength() + " 字");
        }

        // 2. 空值校验
        if (input.trim().isEmpty()) {
            return fatal("输入内容不能为空");
        }

        String lowerInput = input.toLowerCase();

        // 3. 敏感词检查（防御空列表）
        List<String> sensitiveWords = properties.getSensitiveWords();
        if (sensitiveWords != null) {
            for (String word : sensitiveWords) {
                if (word != null && lowerInput.contains(word.toLowerCase())) {
                    return fatal("输入包含不当内容，请修改后重试");
                }
            }
        }

        // 4. 注入模式检查
        for (Pattern pattern : compiledInjectionPatterns) {
            if (pattern.matcher(input).find()) {
                return fatal("检测到恶意输入，请求被拒绝");
            }
        }

        return success();
    }
}