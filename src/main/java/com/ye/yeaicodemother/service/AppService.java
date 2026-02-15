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
     * 通过对话生成应用代码
     *
     * @param appId 应用 ID
     * @param message 提示词
     * @param loginUser 登录用户
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

    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 创建应用
     *
     * @param appAddRequest
     * @param loginUser
     * @return
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
