package com.ye.yeaicodemother.exception;

import com.ye.yeaicodemother.common.BaseResponse;
import com.ye.yeaicodemother.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 捕获并处理控制器层抛出的各类异常，统一返回错误响应格式。
 * 使用 @Hidden 注解隐藏此控制器，使其不出现在API文档中。
 */
@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常 (BusinessException)。
     * 当抛出 BusinessException 时，记录错误日志并返回包含错误码和消息的响应。
     *
     * @param e 抛出的 BusinessException 实例。
     * @return BaseResponse<?>, 包含错误码和错误消息的统一响应对象。
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理运行时异常 (RuntimeException)。
     * 当抛出 RuntimeException 时，记录错误日志并返回统一的系统错误响应。
     *
     * @param e 抛出的 RuntimeException 实例。
     * @return BaseResponse<?>, 包含默认系统错误码和消息的统一响应对象。
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}