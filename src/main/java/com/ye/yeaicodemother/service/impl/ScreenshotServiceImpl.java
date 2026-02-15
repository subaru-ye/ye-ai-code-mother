package com.ye.yeaicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.ye.yeaicodemother.exception.ErrorCode;
import com.ye.yeaicodemother.exception.ThrowUtils;
import com.ye.yeaicodemother.manager.CosManager;
import com.ye.yeaicodemother.service.ScreenshotService;
import com.ye.yeaicodemother.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * 网页截图服务实现类
 */
@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private CosManager cosManager;

    /**
     * 生成指定网页的截图，并将其上传到对象存储。
     *
     * @param webUrl 需要截图的网页完整 URL。
     * @return 成功时返回截图在对象存储上的公开访问 URL (String)；
     *         如果参数错误、截图生成失败或上传失败，则抛出相应业务异常。
     */
    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        // 1. 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "网页URL不能为空");
        log.info("开始生成网页截图，URL: {}", webUrl);

        // 2. 生成本地截图
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.OPERATION_ERROR, "本地截图生成失败");

        // 3. 执行上传和清理操作，并确保清理逻辑始终被执行
        try {
            // 3.1. 上传截图到对象存储
            String cosUrl = uploadScreenshotToCos(localScreenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.OPERATION_ERROR, "截图上传对象存储失败");
            log.info("网页截图生成并上传成功: {} -> {}", webUrl, cosUrl);
            return cosUrl; // 返回上传成功的URL
        } finally {
            // 3.2. 无论上传成功与否，都尝试清理本地临时文件
            cleanupLocalFile(localScreenshotPath);
        }
    }

    /**
     * 将本地截图文件上传到对象存储（COS）。
     *
     * @param localScreenshotPath 本地截图文件的完整路径。
     * @return 成功时返回对象存储中该文件的访问 URL (String)；如果本地文件不存在或上传失败，则返回 null。
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }
        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("截图文件不存在: {}", localScreenshotPath);
            return null;
        }
        // 生成 COS 对象键
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String cosKey = generateScreenshotKey(fileName);
        return cosManager.uploadFile(cosKey, screenshotFile);
    }

    /**
     * 生成截图文件在对象存储中的唯一键（Key）。
     *
     * @param fileName 文件名（不含路径）。
     * @return 对象存储中的完整键（路径+文件名）。
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("/screenshots/%s/%s", datePath, fileName);
    }

    /**
     * 清理本地文件
     *
     * @param localFilePath 本地文件路径
     */
    private void cleanupLocalFile(String localFilePath) {
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            File parentDir = localFile.getParentFile();
            FileUtil.del(parentDir);
            log.info("本地截图文件已清理: {}", localFilePath);
        }
    }
}
