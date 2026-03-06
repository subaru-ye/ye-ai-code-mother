package com.ye.yeaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.ye.yeaicodemother.model.dto.app.AppAddRequest;
import com.ye.yeaicodemother.model.dto.app.AppQueryRequest;
import com.ye.yeaicodemother.model.entity.App;
import com.ye.yeaicodemother.model.entity.User;
import com.ye.yeaicodemother.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/subaru-ye">程序员Ye</a>
 */
public interface AppService extends IService<App> {

    /**
     * 与指定 AI 应用进行对话以生成代码（流式响应）
     * <p>
     * 该方法实现完整的“用户提问 → AI 生成 → 流式返回 → 历史记录”闭环，核心流程如下：
     * 1. 校验参数合法性（appId、message）；
     * 2. 查询并验证应用归属权（仅创建者可操作）；
     * 3. 解析应用预设的代码生成类型（HTML / 多文件 / Vue 项目）；
     * 4. 持久化用户输入消息到聊天历史；
     * 5. 调用统一 AI 代码生成门面服务，获取 SSE 流；
     * 6. 在流传输过程中/结束后，自动保存 AI 响应到聊天历史。
     * </p>
     *
     * @param appId     应用 ID，标识要交互的 AI 应用实例
     * @param message   用户自然语言提示词
     * @param loginUser 当前登录用户，用于权限校验与消息归属
     * @return Flux<String> 流式响应，每个元素为 JSON 字符串，包含 AI 文本、工具调用、错误等事件
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 应用部署
     *
     * @param appId 应用 ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 创建应用
     * <p>
     * 该方法完成以下核心流程：
     * 1. 校验用户输入的初始化 Prompt 是否有效；
     * 2. 基于 Prompt 自动生成应用名称（截取前12个字符）；
     * 3. 调用 AI 路由服务，智能判断应使用哪种代码生成类型（HTML / 多文件 / Vue 项目）；
     * 4. 持久化应用元数据到数据库。
     * </p>
     *
     * @param appAddRequest 用户提交的应用创建请求，必须包含非空的 initPrompt
     * @param loginUser     当前登录用户，用于绑定应用归属
     * @return 新创建应用的唯一 ID（数据库主键）
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 将数据库应用实体 (App) 转换为应用视图对象 (AppVO)。
     * 主要用于将应用信息脱敏后返回给前端，并关联查询创建者信息。
     *
     * @param app 数据库中的应用实体对象。
     * @return AppVO 脱敏后的应用视图对象；如果输入的 app 为 null，则返回 null。
     */
    AppVO getAppVO(App app);

    /**
     * 将数据库应用实体列表 (List<App>) 转换为应用视图对象列表 (List<AppVO>)。
     * 为了优化性能，这里采用批量查询用户信息的方式来避免 N+1 查询问题。
     *
     * @param appList 数据库中的应用实体对象列表。
     * @return List<AppVO> 脱敏后的应用视图对象列表；如果输入的 appList 为空或为 null，则返回空的 ArrayList。
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据应用查询请求参数 (AppQueryRequest) 构建 MyBatis-Plus 的查询条件包装器 (QueryWrapper)。
     * 用于动态生成 SQL 查询语句，支持按ID、名称、生成类型、部署标识、优先级、创建者等多种条件进行筛选。
     *
     * @param appQueryRequest 包含查询条件、分页和排序信息的请求对象。
     * @return QueryWrapper<MyBatis-Plus 的查询条件包装器。
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
}
