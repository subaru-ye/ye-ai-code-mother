package com.ye.yeaicodemother.service;

/**
 * ScreenshotService
 */
public interface ScreenshotService {

    /**
     * 生成指定网页的截图，并将其上传到对象存储。
     *
     * @param webUrl 需要截图的网页完整 URL。
     * @return 成功时返回截图在对象存储上的公开访问 URL (String)；
     *         如果参数错误、截图生成失败或上传失败，则抛出相应业务异常。
     */
    String generateAndUploadScreenshot(String webUrl);
}
