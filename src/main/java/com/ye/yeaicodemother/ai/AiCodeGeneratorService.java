package com.ye.yeaicodemother.ai;

import com.ye.yeaicodemother.ai.model.HtmlCodeResult;
import com.ye.yeaicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成服务接口（由 LangChain4j 动态代理实现）
 * <p>
 * 该接口通过 {@link dev.langchain4j.service.AiServices} 自动生成实现类，
 * 支持同步返回、响应式流（Flux）和带工具调用的 Token 流（TokenStream）三种模式。
 * 所有方法均绑定专属系统提示词（System Prompt），确保模型按指定格式输出。
 * </p>
 */
public interface AiCodeGeneratorService {

    /**
     * 同步生成单文件 HTML 代码
     *
     * @param userMessage 用户自然语言需求
     * @return 包含 HTML 代码及元信息的结果对象
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 同步生成多文件项目代码（如 HTML + CSS + JS）
     *
     * @param userMessage 用户自然语言需求
     * @return 包含多个文件路径与内容的结果对象
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 流式生成 HTML 代码
     *
     * @param userMessage 用户自然语言需求
     * @return 字符串片段的响应式流
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 流式生成多文件代码
     *
     * @param userMessage 用户自然语言需求
     * @return 字符串片段的响应式流
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

    /**
     * 流式生成 Vue 项目（支持工具调用）
     * <p>
     * - 将 appId 作为对话记忆 ID，实现会话隔离
     * - 返回 {@link TokenStream}，可监听：
     *   • 模型文本输出（onPartialResponse）
     *   • 工具调用请求（onPartialToolExecutionRequest）
     *   • 工具执行结果（onToolExecuted）
     *   • 完整响应（onCompleteResponse）
     * </p>
     *
     * @param appId       应用 ID，用于隔离不同用户的对话上下文
     * @param userMessage 用户自然语言需求
     * @return 支持工具调用的 Token 流
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}

