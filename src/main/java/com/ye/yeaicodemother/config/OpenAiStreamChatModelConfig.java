package com.ye.yeaicodemother.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class OpenAiStreamChatModelConfig {

    @Bean("langchain4jAsyncTaskExecutor")
    public AsyncTaskExecutor langchain4jAsyncTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("my-LangChain4j-");
        taskExecutor.setCorePoolSize(6);           // 核心线程数（建议 ≥ 2）
        taskExecutor.setMaxPoolSize(10);            // 最大线程数
        taskExecutor.setQueueCapacity(100);         // 队列容量（防止内存溢出）
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略
        taskExecutor.initialize();                  // 必须初始化
        return taskExecutor;
    }
}