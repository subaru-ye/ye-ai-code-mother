package com.ye.yeaicodemother.task;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.ye.yeaicodemother.model.entity.App;
import com.ye.yeaicodemother.service.AppService;
import com.ye.yeaicodemother.service.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时补全应用封面任务
 */
@Slf4j
@Component
public class AppCoverReplenishTask {

    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;

    @Resource
    private AppService appService;

    @Resource
    private ScreenshotService screenshotService;

    /**
     * 每 10 分钟执行一次
     * 扫描已部署但无封面的应用，异步生成封面
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 10分钟
    public void replenishMissingCovers() {
        log.info("开始检查缺失封面的应用...");

        // 查询条件：deployKey 非空且非空字符串，cover 为空或空字符串
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where("isDelete = ?", 0)
                .and("deployKey IS NOT NULL AND deployKey != ''")
                .and("(cover IS NULL OR cover = '')");

        List<App> appsWithoutCover = appService.list(queryWrapper);

        if (appsWithoutCover.isEmpty()) {
            log.info("暂无缺失封面的应用");
            return;
        }

        log.info("发现 {} 个应用缺少封面，开始异步生成...", appsWithoutCover.size());

        for (App app : appsWithoutCover) {
            Thread.startVirtualThread(() -> {
                try {
                    String appUrl = buildAppUrl(app.getDeployKey());
                    log.debug("为 appId={} 构造封面访问地址: {}", app.getId(), appUrl);
                    String coverUrl = screenshotService.generateAndUploadScreenshot(appUrl);

                    if (StrUtil.isNotBlank(coverUrl)) {
                        App updateApp = new App();
                        updateApp.setId(app.getId());
                        updateApp.setCover(coverUrl);
                        boolean updated = appService.updateById(updateApp);

                        if (updated) {
                            log.info("成功补全应用封面，appId={}, 封面URL={}", app.getId(), coverUrl);
                        } else {
                            log.warn("更新封面失败（数据库未修改），appId={}", app.getId());
                        }
                    } else {
                        log.warn("生成封面返回空，appId={}", app.getId());
                    }
                } catch (Exception e) {
                    log.error("补全封面时发生异常，appId={}", app.getId(), e);
                }
            });
        }
    }

    /**
     * 根据 deployKey 构造完整访问 URL
     */
    private String buildAppUrl(String deployKey) {
        return String.format("%s/%s/", deployHost, deployKey);
    }
}