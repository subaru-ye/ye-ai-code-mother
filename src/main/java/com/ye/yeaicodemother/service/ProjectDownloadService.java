package com.ye.yeaicodemother.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * ClassName:ProjectDownloadService
 */
public interface ProjectDownloadService {

    /**
     * 下载项目为压缩包
     *
     * @param projectPath
     * @param downloadFileName
     * @param response
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
