package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.*;

import java.util.*;

/**
 * @author cloudgyb
 * @since 2025/2/16 10:57
 */
public class ServletRegistrationImpl implements ServletRegistration.Dynamic {
    private final Set<String> mappings = new HashSet<>();
    private final String servletName;
    final Servlet servlet;
    final ServletConfigImpl servletConfig;
    private final ServletContext servletContext;

    public ServletRegistrationImpl(String servletName, Servlet servlet, ServletContext servletContext) {
        this.servletName = servletName;
        this.servlet = servlet;
        this.servletContext = servletContext;
        this.servletConfig = new ServletConfigImpl(servletName, servletContext);
    }

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        mappings.addAll(Arrays.asList(urlPatterns));
        return Set.of();
    }

    @Override
    public Collection<String> getMappings() {
        return mappings;
    }

    @Override
    public String getRunAsRole() {
        return "";
    }

    @Override
    public String getName() {
        return servletName;
    }

    @Override
    public String getClassName() {
        return servlet.getClass().getName();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return servletConfig.setInitParameter(name, value);
    }

    @Override
    public String getInitParameter(String name) {
        return servletConfig.getInitParameter(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        HashSet<String> conflictParams = new HashSet<>();
        initParameters.forEach((k, v) -> {
            boolean b = servletConfig.setInitParameter(k, v);
            if (!b) {
                conflictParams.add(k);
            }
        });
        return conflictParams;
    }

    @Override
    public Map<String, String> getInitParameters() {
        return servletConfig.getInitParameters();
    }

    @Override
    public void setLoadOnStartup(int loadOnStartup) {

    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement constraint) {
        return Set.of();
    }

    @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfig) {

    }

    @Override
    public void setRunAsRole(String roleName) {

    }

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {

    }
}
