package com.github.cloudgyb.jerry.servlet.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author cloudgyb
 * @since 2025/2/23 14:26
 */
public class FilterConfigImpl implements FilterConfig {
    private final FilterRegistrationImpl filterRegistration;
    private final Map<String, String> initParameters;

    public FilterConfigImpl(FilterRegistrationImpl filterRegistration) {
        this.filterRegistration = filterRegistration;
        this.initParameters = new HashMap<>();
    }

    @Override
    public String getFilterName() {
        return filterRegistration.getName();
    }

    @Override
    public ServletContext getServletContext() {
        return filterRegistration.servletContext;
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
        String s = initParameters.get(name);
        if (s != null) {
            return false;
        }
        initParameters.put(name, value);
        return true;
    }

    Map<String, String> getInitParameters() {
        return initParameters;
    }
}
