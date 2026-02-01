package com.ye.yeaicodemother.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ye.yeaicodemother.ai.model.HtmlCodeResult;
import com.ye.yeaicodemother.ai.model.MultiFileCodeResult;
import com.ye.yeaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存器
 * 负责将 AI 生成的代码结果（HTML单文件或多文件）保存到本地临时目录。
 */
@Deprecated
public class CodeFileSaver {

    // 文件保存根目录，位于项目根目录下的 tmp/code_output 文件夹
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存 HtmlCodeResult 到文件。
     * 将生成的 HTML 代码保存为 index.html 文件，并创建一个以业务类型和唯一ID命名的目录。
     *
     * @param result 包含 HTML 代码的 {@link HtmlCodeResult} 对象。
     * @return 返回保存文件的根目录 {@link File} 对象。
     */
    public static File saveHtmlCodeResult(HtmlCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        return new File(baseDirPath);
    }

    /**
     * 保存 MultiFileCodeResult
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
        return new File(baseDirPath);
    }

    /**
     * 构建唯一目录路径：tmp/code_output/bizType_雪花ID
     */
    private static String buildUniqueDir(String bizType) {
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 写入单个文件
     */
    private static void writeToFile(String dirPath, String filename, String content) {
        String filePath = dirPath + File.separator + filename;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}
