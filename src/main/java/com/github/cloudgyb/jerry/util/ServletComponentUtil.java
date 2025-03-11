package com.github.cloudgyb.jerry.util;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;

import java.util.EventListener;

/**
 * Servlet component util
 *
 * @author cloudgyb
 * @since 2025/3/11 21:42
 */
public class ServletComponentUtil {
    /**
     * 判断给定的类是否是Servlet类
     * 本方法通过两个条件来判断：
     * 1. 该类是否可以被赋值为Servlet类型，即该类是否实现了Servlet接口
     * 2. 该类是否使用了@WebServlet注解，表明它是一个Servlet组件
     *
     * @param aClass 要检查的类
     * @return 如果该类是一个Servlet类，则返回true；否则返回false
     */
    public static boolean isServletClass(Class<?> aClass) {
        return Servlet.class.isAssignableFrom(aClass) &&
                aClass.isAnnotationPresent(WebServlet.class);
    }

    /**
     * 判断给定的类是否是一个 Filter 类
     * 本方法用于检查一个类是否被标记为 WebFilter，并且是否可以被赋值给 Filter 类型
     * 这主要用于识别那些在 Web 应用程序中被用作过滤器的类
     *
     * @param aClass 待检查的类
     * @return 如果该类是过滤器类且被标记为 WebFilter，则返回 true；否则返回 false
     */
    public static boolean isFilterClass(Class<?> aClass) {
        // 检查给定的类是否可以被赋值给Filter类型，且是否被@WebFilter注解标记
        return Filter.class.isAssignableFrom(aClass) &&
                aClass.isAnnotationPresent(WebFilter.class);
    }

    public static boolean isWebListenerClass(Class<?> aClass) {
        return EventListener.class.isAssignableFrom(aClass) &&
                aClass.isAnnotationPresent(WebListener.class);

    }
}
