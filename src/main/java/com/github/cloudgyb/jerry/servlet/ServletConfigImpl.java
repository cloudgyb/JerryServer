package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author cloudgyb
 * @since 2025/2/16 17:10
 */
public class ServletConfigImpl implements ServletConfig {
    private final String servletName;
    private final ServletContext servletContext;
    private final Map<String, String> initParameters;

    public ServletConfigImpl(String servletName, ServletContext servletContext) {
        this.servletName = servletName;
        this.servletContext = servletContext;
        this.initParameters = new HashMap<>();
    }

    @Override
    public String getServletName() {
        return servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    boolean setInitParameter(String name, String value) {
        String s = initParameters.putIfAbsent(name, value);
        return s == null;
    }

    Map<String,String> getInitParameters() {
        return initParameters;
    }
}
