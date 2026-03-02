package com.ye.yeaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ye.yeaicodemother.ai.guardrail.PromptSafetyInputGuardrail;
import com.ye.yeaicodemother.ai.tools.ToolManager;
import com.ye.yeaicodemother.exception.BusinessException;
import com.ye.yeaicodemother.exception.ErrorCode;
import com.ye.yeaicodemother.model.enums.CodeGenTypeEnum;
import com.ye.yeaicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 代码生成服务工厂类
 * <p>
 * 负责根据 appId 和代码生成类型（HTML / 多文件 / Vue 项目）动态创建并缓存 {@link AiCodeGeneratorService} 实例。
 * 每个 appId 对应独立的对话记忆（基于 Redis），确保多用户会话隔离。
 * </p>
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    /**
     * 全局共享的非流式聊天模型
     */
    @Resource
    private ChatModel chatModel;

    /**
     * 默认流式聊天模型
     */
    @Resource
    private StreamingChatModel openAiStreamingChatModel;

    /**
     * 推理流式聊天模型
     */
    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    /**
     * 基于 Redis 的聊天记忆存储，用于持久化多轮对话上下文
     */
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 聊天历史服务，用于从数据库加载历史消息到内存
     */
    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 工具管理器，提供所有可用的 AI 工具
     */
    @Resource
    private ToolManager toolManager;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取默认类型（HTML）的 AI 服务实例（带缓存）
     * <p>
     * 此方法主要用于兼容旧逻辑或简单场景。
     * </p>
     *
     * @param appId 应用 ID，用于隔离不同用户的对话上下文
     * @return 对应的 AI 服务实例
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 和代码生成类型获取 AI 服务实例（带缓存）
     * <p>
     * 若缓存中不存在，则创建新实例并加入缓存。
     * </p>
     *
     * @param appId       应用 ID，用于对话隔离
     * @param codeGenType 代码生成类型（HTML / MULTI_FILE / VUE_PROJECT）
     * @return 对应配置的 AI 服务实例
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 构建缓存键：格式为 "appId_codeGenTypeValue"
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型枚举
     * @return 缓存唯一键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * 创建新的 AI 代码生成服务实例
     * <p>
     * - 为每个 appId 初始化独立的 {@link MessageWindowChatMemory}
     * - 从数据库加载最近 20 条历史消息到记忆中
     * - 根据 codeGenType 选择不同的模型和功能配置：
     *   • VUE_PROJECT：启用推理模型 + 工具调用 + 幻觉防护
     *   • HTML / MULTI_FILE：使用默认流式模型，无工具调用
     * </p>
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return 新构建的 AI 服务代理实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            // Vue 项目生成使用推理模型
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(toolManager.getAllTools())
                    // 处理工具调用幻觉问题
                    .hallucinatedToolNameStrategy(toolExecutionRequest ->
                            ToolExecutionResultMessage.from(toolExecutionRequest,
                                    "Error: there is no tool called " + toolExecutionRequest.name())
                    )
                    .inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                    .build();
            // HTML 和多文件生成使用默认模型
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                    .build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * Spring Bean：提供一个默认的全局 AI 服务实例（appId=0）
     * <p>
     * 主要用于非用户上下文场景（如系统初始化、测试等），
     * 实际业务中应优先使用 {@link #getAiCodeGeneratorService(long, CodeGenTypeEnum)}。
     * </p>
     *
     * @return 默认 AI 服务实例
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }
}

