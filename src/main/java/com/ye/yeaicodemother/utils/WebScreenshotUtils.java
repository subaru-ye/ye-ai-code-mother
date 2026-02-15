package com.ye.yeaicodemother.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.ye.yeaicodemother.exception.BusinessException;
import com.ye.yeaicodemother.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

/**
 * 网页截图工具类
 * 该类提供静态方法，利用 Selenium WebDriver (Chrome Headless) 对指定URL的网页进行截图，
 * 并对截图进行压缩以减小文件体积，方便存储和传输。
 */
@Slf4j
public class WebScreenshotUtils {

    // 全局唯一的 WebDriver 实例，用于执行浏览器操作
    private static volatile WebDriver webDriver=null;

    // 使用相对路径指向 resources 目录下的 ChromeDriver
    private static final String CHROME_DRIVER_PATH =
            System.getProperty("user.dir") + "/src/main/resources/web_drivers/chromedriver.exe";
    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 900;

/*    // 全局静态初始化驱动程序：
    static {
        // 定义默认的浏览器窗口尺寸
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        // 初始化 WebDriver
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }*/

    /**
     * 应用关闭时，销毁 WebDriver 资源。
     */
    @PreDestroy
    public static void destroy() {
        if (webDriver != null) {
            try {
                webDriver.quit();
            } catch (Exception e) {
                log.warn("关闭 WebDriver 时出现异常", e);
            } finally {
                webDriver = null;
            }
        }
    }

    /**
     * 生成网页截图并返回压缩后图片的文件路径。
     *
     * @param webUrl 要进行截图的网页完整 URL 地址。
     * @return 成功时返回压缩后图片的绝对文件路径 (String)；如果过程中发生任何错误，则返回 null。
     */
    public static String saveWebPageScreenshot(String webUrl) {
        // 1. 非空校验
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页截图失败，url为空");
            return null;
        }
        WebDriver driver = null;

        try {
            // 每次调用创建新的 WebDriver 实例
            driver = getWebDriver();

            // 2. 创建临时目录
            String rootPath = System.getProperty("user.dir") + "/tmp/screenshots/" + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            // 构造原始图片保存路径
            final String IMAGE_SUFFIX = ".png";
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;
            // 3. 访问网页
            driver.get(webUrl);
            // 等待网页加载
            waitForPageLoad(driver);
            // 4. 截图并保存原始图片
            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功：{}", imageSavePath);
            // 5. 压缩图片
            final String COMPRESS_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESS_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功：{}", compressedImagePath);
            // 6. 删除原始图片
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("网页截图失败：{}", webUrl, e);
            return null;
        }
    }

    /**
     * 初始化并配置一个无头模式的 Chrome WebDriver 实例。
     * 此方法负责下载和配置 ChromeDriver，设置必要的浏览器启动参数以适应
     * 服务器或 Docker 等无图形界面的环境。
     *
     * @return 初始化并配置好的 WebDriver 实例。
     */
    private static WebDriver getWebDriver() {
        try {
            // 检查 ChromeDriver 文件是否存在
            File driverFile = new File(CHROME_DRIVER_PATH);
            if (!driverFile.exists()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "ChromeDriver 未找到，请确保文件存在于: " + CHROME_DRIVER_PATH);
            }
            // 设置本地 ChromeDriver 路径
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
            // 配置 Chrome 选项及相关配置
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");  // 无头模式：后台运行不弹出窗口
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments(String.format("--window-size=%d,%d", DEFAULT_WIDTH, DEFAULT_HEIGHT));  // 设置窗口大小
            options.addArguments("--disable-extensions");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }

    /**
     * 将字节数组保存为图片文件。
     * 此方法用于将 WebDriver 截取的屏幕字节数据写入到指定的文件路径。
     *
     * @param imageBytes 包含图像数据的字节数组。
     * @param imagePath  目标图片文件的保存路径。
     */
    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败：{}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 对指定路径的原始图片进行压缩。
     *
     * @param originImagePath     原始图片的文件路径。
     * @param compressedImagePath 压缩后图片的期望保存路径。
     */
    private static void compressImage(String originImagePath, String compressedImagePath) {
        // 压缩图片质量
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("压缩图片失败：{} -> {}", originImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

    /**
     * 等待网页完全加载完毕。
     *
     * @param webDriver 当前操作的 WebDriver 实例。
     */
    private static void waitForPageLoad(WebDriver webDriver) {
        try {
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            // 等待 document.readyState 为 complete
            wait.until(driver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState").
                    equals("complete")
            );
            // 额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }
}