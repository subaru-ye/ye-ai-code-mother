package com.ye.yeaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ye.yeaicodemother.constant.UserConstant;
import com.ye.yeaicodemother.exception.BusinessException;
import com.ye.yeaicodemother.exception.ErrorCode;
import com.ye.yeaicodemother.exception.ThrowUtils;
import com.ye.yeaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.ye.yeaicodemother.model.entity.App;
import com.ye.yeaicodemother.model.entity.ChatHistory;
import com.ye.yeaicodemother.mapper.ChatHistoryMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import com.ye.yeaicodemother.model.entity.User;
import com.ye.yeaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.ye.yeaicodemother.service.AppService;
import com.ye.yeaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/subaru-ye">程序员Ye</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

    /**
     * 加载对话历史到到 AI 聊天记忆（ChatMemory）中，用于恢复上下文
     *
     * @param appId        应用 ID，标识要加载历史的会话
     * @param chatMemory   目标聊天记忆容器（如 MessageWindowChatMemory），用于注入历史消息
     * @param maxCount     最多加载的消息条数（建议 ≤ 窗口大小，避免 token 超限）
     * @return 实际成功加载并注入记忆的消息条数
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            // 避免把“当前用户刚发的消息”当作历史上下文重复传给 AI
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);

            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }

            // LangChain 要求消息按时间正序传入，否则上下文逻辑混乱
            historyList = historyList.reversed();

            // 清空现有记忆，防止重复或残留数据干扰
            chatMemory.clear();

            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            for (ChatHistory history : historyList) {
                String messageType = history.getMessageType();
                String content = history.getMessage();
                // 安全映射：仅处理已知消息类型
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(messageType)) {
                    chatMemory.add(UserMessage.from(content));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(messageType)) {
                    chatMemory.add(AiMessage.from(content));
                    loadedCount++;
                }
            }

            return loadedCount;

        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

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
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 基础参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");

        // 消息类型白名单校验
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);

        // 构造实体并保存
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();

        return this.save(chatHistory);
    }

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
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }

        // 提取查询参数
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 游标：最后一条记录的创建时间
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

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
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 权限校验：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");

        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);

        // 执行分页查询
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    /**
     * 根据应用 ID 关联的对话历史记录（物理删除）
     *
     * @param appId 应用 ID
     * @throws BusinessException 应用 ID 非法时抛出
     */
    @Override
    public void deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        this.remove(queryWrapper);
    }

}
