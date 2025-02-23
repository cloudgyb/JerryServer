package com.github.cloudgyb.jerry.servlet.filter;

import com.github.cloudgyb.jerry.servlet.ServletContextImpl;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;

import java.util.*;

/**
 * @author cloudgyb
 * @since 2025/2/23 14:15
 */
public class FilterRegistrationImpl implements FilterRegistration.Dynamic {
    final ServletContextImpl servletContext;
    final Filter filter;
    private final String name;
    private final FilterConfigImpl filterConfig;
    private final Set<String> servletNames;
    private final Set<String> urlPatterns;

    public FilterRegistrationImpl(ServletContextImpl servletContext, String name, Filter filter) {
        this.servletContext = servletContext;
        this.name = name;
        this.filter = filter;
        this.servletNames = new HashSet<>();
        this.urlPatterns = new HashSet<>();
        this.filterConfig = new FilterConfigImpl(this);
    }

    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletName) {
        servletNames.addAll(Arrays.asList(servletName));
        servletContext.addFilterMappingForServletName(servletName, filter);
    }

    @Override
    public Collection<String> getServletNameMappings() {
        return new HashSet<>(servletNames);
    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPattern) {
        urlPatterns.addAll(Arrays.asList(urlPattern));
        servletContext.addFilterMapping(urlPattern, filter);
    }

    @Override
    public Collection<String> getUrlPatternMappings() {
        return new HashSet<>(urlPatterns);
    }

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClassName() {
        return Filter.class.getName();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return filterConfig.setInitParameter(name, value);
    }

    @Override
    public String getInitParameter(String name) {
        return filterConfig.getInitParameter(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        HashSet<String> parametersInConflicted = new HashSet<>();
        initParameters.forEach((k, v) -> {
            boolean isExist = setInitParameter(k, v);
            if (isExist) {
                parametersInConflicted.add(k);
            }
        });
        return parametersInConflicted;
    }

    @Override
    public Map<String, String> getInitParameters() {
        return filterConfig.getInitParameters();
    }

    public FilterConfigImpl getFilterConfig() {
        return filterConfig;
    }
}
