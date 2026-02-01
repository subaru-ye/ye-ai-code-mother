package com.ye.yeaicodemother;

import com.ye.yeaicodemother.ai.AiCodeGeneratorService;
import com.ye.yeaicodemother.ai.model.HtmlCodeResult;
import com.ye.yeaicodemother.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个程序员鱼皮的工作记录小工具");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("做个登录页面，不超过20行代码");
        Assertions.assertNotNull(multiFileCode);
    }

}
