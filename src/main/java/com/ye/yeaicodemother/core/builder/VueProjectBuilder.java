package com.ye.yeaicodemother.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Vue 项目构建器
 * <p>
 * 负责对已生成的 Vue 项目目录执行本地构建流程（npm install + npm run build），
 * 验证是否能成功生成可部署的 dist 目录。
 * </p>
 */
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * 异步启动 Vue 项目构建任务
     * <p>
     * 使用虚拟线程（Virtual Thread）在后台执行构建流程，避免阻塞主线程。
     * 构建结果通过日志记录，不返回给调用方。
     * </p>
     *
     * @param projectPath 待构建的 Vue 项目根目录绝对路径（必须已存在且包含 package.json）
     */
    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis())
                .start(() -> {
                    try {
                        buildProject(projectPath);
                    } catch (Exception e) {
                        log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
                    }
                });
    }

    /**
     * 同步执行 Vue 项目完整构建流程
     * <p>
     * 流程包括：
     * <ol>
     *   <li>校验项目目录和 package.json 存在；</li>
     *   <li>执行 {@code npm install} 安装依赖（超时 5 分钟）；</li>
     *   <li>执行 {@code npm run build} 构建项目（超时 3 分钟）；</li>
     *   <li>验证 {@code dist} 目录是否生成。</li>
     * </ol>
     * </p>
     *
     * @param projectPath 项目根目录路径（必须为绝对路径）
     * @return {@code true} 表示构建成功，{@code false} 表示任一环节失败
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在：{}", projectPath);
            return false;
        }
        // 检查是否有 package.json 文件
        File packageJsonFile = new File(projectDir, "package.json");
        if (!packageJsonFile.exists()) {
            log.error("项目目录中没有 package.json 文件：{}", projectPath);
            return false;
        }
        log.info("开始构建 Vue 项目：{}", projectPath);
        // 执行 npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 执行失败：{}", projectPath);
            return false;
        }
        // 执行 npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 执行失败：{}", projectPath);
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists() || !distDir.isDirectory()) {
            log.error("构建完成但 dist 目录未生成：{}", projectPath);
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录：{}", projectPath);
        return true;
    }

    /**
     * 执行 npm install 命令
     *
     * @param projectDir 项目根目录
     * @return 是否成功执行（退出码为 0）
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand());
        return executeCommand(projectDir, command, 300); // 5分钟超时
    }

    /**
     * 执行 npm run build 命令
     *
     * @param projectDir 项目根目录
     * @return 是否成功执行（退出码为 0）
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand());
        return executeCommand(projectDir, command, 180); // 3分钟超时
    }

    /**
     * 根据操作系统类型构造可执行命令后缀
     * <p>
     * Windows 系统需使用 {@code npm.cmd}，其他系统（Linux/macOS）直接使用 {@code npm}。
     * </p>
     *
     * @return 带平台后缀的完整命令（如 "npm.cmd" 或 "npm"）
     */
    private String buildCommand() {
        if (isWindows()) {
            return "npm" + ".cmd";
        }
        return "npm";
    }

    /**
     * 检测当前操作系统是否为 Windows
     *
     * @return {@code true} 表示 Windows，否则为 Unix-like 系统
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * 在指定工作目录下执行系统命令，并设置超时控制
     *
     * @param workingDir     命令执行的工作目录
     * @param command        完整命令字符串（如 "npm install"）
     * @param timeoutSeconds 最大等待时间（秒），超时则强制终止进程
     * @return {@code true} 表示命令成功执行（退出码为 0），否则返回 {@code false}
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }

}