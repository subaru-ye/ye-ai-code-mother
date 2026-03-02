package com.ye.yeaicodemother.core;

import cn.hutool.json.JSONUtil;
import com.ye.yeaicodemother.ai.AiCodeGeneratorService;
import com.ye.yeaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.ye.yeaicodemother.ai.model.HtmlCodeResult;
import com.ye.yeaicodemother.ai.model.MultiFileCodeResult;
import com.ye.yeaicodemother.ai.model.message.AiResponseMessage;
import com.ye.yeaicodemother.ai.model.message.ToolExecutedMessage;
import com.ye.yeaicodemother.ai.model.message.ToolRequestMessage;
import com.ye.yeaicodemother.constant.AppConstant;
import com.ye.yeaicodemother.core.builder.VueProjectBuilder;
import com.ye.yeaicodemother.exception.BusinessException;
import com.ye.yeaicodemother.exception.ErrorCode;
import com.ye.yeaicodemother.model.enums.CodeGenTypeEnum;
import com.ye.yeaicodemother.core.parser.CodeParserExecutor;
import com.ye.yeaicodemother.core.saver.CodeFileSaverExecutor;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 统一入口：根据类型生成并保存代码（同步，已放弃使用）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取相应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     * <p>
     * 支持三种生成模式：
     * - HTML：纯文本流，生成完成后解析保存
     * - MULTI_FILE：多文件文本流，生成完成后解析保存
     * - VUE_PROJECT：支持工具调用的 Token 流，实时推送 AI 响应、工具请求、执行结果等事件
     * </p>
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型（HTML / MULTI_FILE / VUE_PROJECT）
     * @param appId           应用 ID，用于隔离用户会话和输出目录
     * @return Flux<String> 流式响应，每个元素为 JSON 字符串，表示不同类型的事件消息
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        // 基础校验
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }

        // 根据 appId 和类型获取带独立记忆的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        // 根据不同类型执行
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId);
            }
        };
    }

    /**
     * 通用流式代码处理方法（无工具调用）
     * <p>
     * 将 Flux<String> 的文本片段流聚合成完整代码，在流结束时进行解析与持久化。
     * 注意：此方法**不会实时推送代码内容**，仅在 onComplete 时触发保存。
     * </p>
     *
     * @param codeStream  AI 返回的原始文本片段流
     * @param codeGenType 代码生成类型，用于选择对应的解析器和保存策略
     * @param appId       应用 ID，用于确定输出目录
     * @return 空 Flux
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            // 实时收集 AI 输出的每个文本片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            // 当 AI 流结束时，执行完整代码的解析与保存
            try {
                String completeCode = codeBuilder.toString();
                // 解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，路径为：{}", savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 将 TokenStream 转换为标准 Flux<String>，并封装各类事件为统一 JSON 消息
     * <p>
     * 支持三类事件实时推送：
     * 1. AI 文本输出（AiResponseMessage）
     * 2. 工具调用请求（ToolRequestMessage）
     * 3. 工具执行结果（ToolExecutedMessage）
     * 最终在 onComplete 时触发 Vue 项目构建。
     * </p>
     *
     * @param tokenStream LangChain4j 提供的高级流式响应对象（支持工具调用回调）
     * @param appId       应用 ID，用于构建项目路径
     * @return Flux<String> 每个元素为 JSON 字符串，前端可按类型区分处理
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {
        return Flux.create(sink -> tokenStream
                // 1. 接收模型生成的文本片段（如 "正在创建..."）
                .onPartialResponse((String partialResponse) -> {
                    AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                    sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                })
                // 2. 接收模型发起的工具调用请求（如 write_file("App.vue", "...")）
                .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                    ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                    sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                })
                // 3. 接收工具执行后的返回结果（如 {"status": "success"}）
                .onToolExecuted((ToolExecution toolExecution) -> {
                    ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                    sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                })
                // 4. 整个对话完成，执行 Vue 项目构建（同步阻塞）
                .onCompleteResponse((ChatResponse response) -> {
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                    vueProjectBuilder.buildProject(projectPath);
                    sink.complete();
                })
                // 5. 异常处理
                .onError((Throwable error) -> {
                    error.printStackTrace();
                    sink.error(error);
                })
                // 启动流式监听
                .start());
    }

}
