package com.example.demo.dao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.List;

/**
 * JSON工具类，基于FastJson[4](@ref)
 */
public class JsonUtil {

    /**
     * 对象转JSON字符串
     */
    public static String toJsonString(Object obj) {
        return JSON.toJSONString(obj, SerializerFeature.PrettyFormat,
                SerializerFeature.WriteDateUseDateFormat);
    }

    /**
     * JSON字符串转对象
     */
    public static <T> T parseObject(String jsonStr, Class<T> clazz) {
        return JSON.parseObject(jsonStr, clazz);
    }

    /**
     * JSON字符串转列表
     */
    public static <T> List<T> parseArray(String jsonStr, Class<T> clazz) {
        return JSON.parseArray(jsonStr, clazz);
    }
}
