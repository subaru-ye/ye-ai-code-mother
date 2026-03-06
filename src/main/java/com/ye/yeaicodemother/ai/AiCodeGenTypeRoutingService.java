package com.ye.yeaicodemother.ai;

import com.ye.yeaicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI代码生成类型智能路由服务
 * 使用结构化输出直接返回枚举类型
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     *
     * @param userPrompt 用户输入的需求描述
     * @return 推荐的代码生成类型
     */
    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userPrompt);

    /**
     * 根据用户提供的应用功能描述生成应用名称
     *
     * @param userPrompt 应用的功能 / 使用场景描述
     * @return 生成的应用名称（不包含多余说明或标点）
     */
    @SystemMessage(fromResource = "prompt/app-naming-system-prompt.txt")
    String generateAppName(String userPrompt);
}
