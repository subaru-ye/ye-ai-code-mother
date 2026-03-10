package com.ye.yeaicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.ye.yeaicodemother.exception.BusinessException;
import com.ye.yeaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.ye.yeaicodemother.model.entity.ChatHistory;
import com.ye.yeaicodemother.model.entity.User;
import com.ye.yeaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/subaru-ye">程序员Ye</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 向指定应用的对话历史中添加一条消息记录
     * <p>
     * 该方法用于持久化用户或 AI 的对话内容，是聊天流的核心写入入口。
     * 支持的消息类型由 {@link ChatHistoryMessageTypeEnum} 严格限定（如 "user"、"ai"），
     * 确保数据规范性和前端渲染一致性。
     * </p>
     *
     * @param appId       应用 ID，标识消息所属的 AI 应用
     * @param message     消息内容（如用户提问或 AI 生成的代码片段）
     * @param messageType 消息类型，必须为预定义枚举值（如 "user" / "ai"）
     * @param userId      发送者用户 ID（AI 消息也关联创建者用户，便于权限隔离）
     * @return 是否保存成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用 ID 关联的对话历史记录（物理删除）
     *
     * @param appId 应用 ID
     * @throws BusinessException 应用 ID 非法时抛出
     */
    boolean deleteByAppId(Long appId);

    /**
     * 分页查询指定应用的对话历史（游标分页）
     * <p>
     * 采用基于 createTime 的游标分页，避免传统 offset 分页在实时插入场景下的数据跳变问题。
     * 仅允许应用创建者或系统管理员访问，保障数据隔离安全。
     * </p>
     *
     * @param appId           应用 ID，用于限定查询范围
     * @param pageSize        每页记录数，限制在 1~50 之间，防止过大查询拖垮数据库
     * @param lastCreateTime  游标：上一页最后一条记录的创建时间，用于获取更早的历史消息
     * @param loginUser       当前登录用户，用于权限校验
     * @return 分页结果，按 createTime 降序排列（最新消息在前）
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 加载对话历史到到 AI 聊天记忆（ChatMemory）中，用于恢复上下文
     *
     * @param appId        应用 ID，标识要加载历史的会话
     * @param chatMemory   目标聊天记忆容器（如 MessageWindowChatMemory），用于注入历史消息
     * @param maxCount     最多加载的消息条数（建议 ≤ 窗口大小，避免 token 超限）
     * @return 实际成功加载并注入记忆的消息条数
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 构建查询条件
     * <p>
     * 支持多维度过滤（ID、消息内容、类型、应用、用户） + 游标分页 + 自定义排序。
     * 核心设计亮点：
     * 1. 使用 createTime 作为游标字段，实现高效、无跳变的分页（适用于对话历史等时序数据）；
     * 2. 所有参数均为可选，空值自动忽略，避免 SQL 注入风险；
     * 3. 默认按创建时间倒序排列（最新消息在前），符合聊天场景 UX。
     * </p>
     *
     * @param chatHistoryQueryRequest 查询条件封装对象，支持分页、过滤、排序
     * @return 构建好的 QueryWrapper，可直接用于数据库查询
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
