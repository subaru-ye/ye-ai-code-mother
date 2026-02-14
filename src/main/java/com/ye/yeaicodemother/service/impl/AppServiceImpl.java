package com.ye.yeaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ye.yeaicodemother.constant.AppConstant;
import com.ye.yeaicodemother.core.AiCodeGeneratorFacade;
import com.ye.yeaicodemother.core.builder.VueProjectBuilder;
import com.ye.yeaicodemother.core.handler.StreamHandlerExecutor;
import com.ye.yeaicodemother.exception.BusinessException;
import com.ye.yeaicodemother.exception.ErrorCode;
import com.ye.yeaicodemother.exception.ThrowUtils;
import com.ye.yeaicodemother.model.dto.app.AppQueryRequest;
import com.ye.yeaicodemother.model.entity.App;
import com.ye.yeaicodemother.mapper.AppMapper;
import com.ye.yeaicodemother.model.entity.User;
import com.ye.yeaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.ye.yeaicodemother.model.enums.CodeGenTypeEnum;
import com.ye.yeaicodemother.model.vo.AppVO;
import com.ye.yeaicodemother.model.vo.UserVO;
import com.ye.yeaicodemother.service.AppService;
import com.ye.yeaicodemother.service.ChatHistoryService;
import com.ye.yeaicodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/subaru-ye">程序员Ye</a>
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以和自己的应用对话
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        }
        // 5. 在调用 AI 前，先保存用户消息到数据库中
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 调用 AI 生成代码（流式）
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 7. 收集 AI 响应的内容，并且在完成后保存记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum);
    }

    /**
     * 应用部署
     *
     * @param appId 应用 ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请重试");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 构建完成后，需要将构建后的文件复制到部署目录
            sourceDir = distDir;
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 9. 更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 返回可访问的 URL 地址
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }


    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }


    /**
     * 将数据库应用实体 (App) 转换为应用视图对象 (AppVO)。
     * 主要用于将应用信息脱敏后返回给前端，并关联查询创建者信息。
     *
     * @param app 数据库中的应用实体对象。
     * @return AppVO 脱敏后的应用视图对象；如果输入的 app 为 null，则返回 null。
     */
    @Override
    public AppVO getAppVO(App app) {
        // 1. 校验输入参数
        if (app == null) {
            return null;
        }

        // 2. 创建 VO 对象并复制基础属性
        // 将 App 实体的所有公共 getter/setter 属性值复制到 AppVO 中
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);

        // 3. 关联查询并设置创建者用户信息
        // 为了在前端显示应用由谁创建，需要查询并填充 UserVO 信息
        Long userId = app.getUserId(); // 获取应用所属用户的ID
        if (userId != null) {
            // 根据用户ID查询用户实体
            User user = userService.getById(userId);
            // 将用户实体转换为脱敏的用户VO（不包含密码等敏感信息）
            UserVO userVO = userService.getUserVO(user);
            // 将用户VO设置到应用VO中
            appVO.setUser(userVO);
        }

        // 4. 返回填充好信息的VO对象
        return appVO;
    }

    /**
     * 根据应用查询请求参数 (AppQueryRequest) 构建 MyBatis-Plus 的查询条件包装器 (QueryWrapper)。
     * 用于动态生成 SQL 查询语句，支持按ID、名称、生成类型、部署标识、优先级、创建者等多种条件进行筛选。
     *
     * @param appQueryRequest 包含查询条件、分页和排序信息的请求对象。
     * @return QueryWrapper<MyBatis-Plus 的查询条件包装器。
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        // 1. 校验请求参数是否为空
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 2. 从请求对象中提取查询条件
        // 从 AppQueryRequest 中获取各个可能的查询字段
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();       // 应用名称 (模糊查询)
        String cover = appQueryRequest.getCover();           // 应用封面 (模糊查询)
        String initPrompt = appQueryRequest.getInitPrompt(); // 应用初始化提示词 (模糊查询)
        String codeGenType = appQueryRequest.getCodeGenType(); // 代码生成类型 (精确查询)
        String deployKey = appQueryRequest.getDeployKey();   // 部署标识 (精确查询)
        Integer priority = appQueryRequest.getPriority();    // 优先级 (精确查询)
        Long userId = appQueryRequest.getUserId();          // 创建用户ID (精确查询)
        String sortField = appQueryRequest.getSortField();   // 排序字段
        String sortOrder = appQueryRequest.getSortOrder();   // 排序方向 (ascend/descend)

        // 3. 构建并返回查询条件包装器
        // 使用 QueryWrapper.create() 初始化，并链式调用条件方法
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 将数据库应用实体列表 (List<App>) 转换为应用视图对象列表 (List<AppVO>)。
     * 为了优化性能，这里采用批量查询用户信息的方式来避免 N+1 查询问题。
     *
     * @param appList 数据库中的应用实体对象列表。
     * @return List<AppVO> 脱敏后的应用视图对象列表；如果输入的 appList 为空或为 null，则返回空的 ArrayList。
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        // 1. 校验输入参数
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }

        // 2. 批量查询所有相关的用户信息，以避免对每个App都单独查询一次用户
        // 2.1. 收集所有应用关联的用户ID集合
        // 遍历应用列表，提取每个App的userId，并收集到一个Set中，以去除重复的ID
        Set<Long> userIds = appList.stream()
                .map(App::getUserId) // 提取每个App的userId字段
                .collect(Collectors.toSet()); // 收集为Set，自动去重

        // 2.2. 一次性查询所有用户，并将其转换为 Map<ID, UserVO> 的形式，方便后续快速查找
        // 调用userService的批量查询方法，一次性获取所有需要的用户实体
        // 然后将这些用户实体转换为脱敏的UserVO，并以用户ID为key存入Map中
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,             // Map的key是用户的ID
                        userService::getUserVO   // Map的value是该用户的脱敏VO对象
                ));

        // 3. 遍历应用列表，为每个应用设置其对应的用户信息，并最终收集为AppVO列表
        // 对 appList 中的每个 App 对象进行处理
        return appList.stream().map(app -> {
            // 3.1. 调用单个转换方法获取基本的AppVO（包含应用自身信息，但user字段可能为null）
            AppVO appVO = getAppVO(app);

            // 3.2. 从预先查询好的Map中根据app的userId快速获取对应的用户VO，并设置到AppVO中
            // 这样避免了再次数据库查询，提高了效率
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);

            // 3.3. 返回填充好用户信息的AppVO
            return appVO;
        }).collect(Collectors.toList()); // 将处理后的流收集为List<AppVO>
    }



}
