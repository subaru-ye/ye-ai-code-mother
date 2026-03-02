package com.ye.yeaicodemother.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.ye.yeaicodemother.model.enums.CodeGenTypeEnum;

/**
 * 缓存 key 生成工具类
 */
public class CacheKeyUtils {

    /**
     * 生成 AI 代码生成服务的缓存键
     * 格式: {@code appId_codeGenTypeValue}
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return 缓存键
     */
    public static String buildAiServiceCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        if (codeGenType == null) {
            throw new IllegalArgumentException("codeGenType 不能为空");
        }
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * 根据对象生成缓存key (JSON + MD5)
     *
     * @param obj 要生成key的对象
     * @return MD5哈希后的缓存key
     */
    public static String generateKey(Object obj) {
        if (obj == null) {
            return DigestUtil.md5Hex("null");
        }
        // 先转 JSON，再转 MD5
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }
}