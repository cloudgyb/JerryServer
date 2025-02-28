package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.*;

import java.util.*;

/**
 * @author cloudgyb
 * @since 2025/2/16 10:57
 */
public class ServletRegistrationImpl implements ServletRegistration.Dynamic, Comparable<ServletRegistrationImpl> {
    private static final Set<String> mappedUrlPatterns = new HashSet<>();
    private final Set<String> mappings = new HashSet<>();
    private final String servletName;
    final Servlet servlet;
    final ServletConfigImpl servletConfig;
    private final ServletContextImpl servletContext;
    int loadOnStartup = -1;
    MultipartConfigElement multipartConfigElement;
    boolean asyncSupported = false;
    boolean initialized = false;

    public ServletRegistrationImpl(String servletName, Servlet servlet, ServletContextImpl servletContext) {
        this.servletName = servletName;
        this.servlet = servlet;
        this.servletContext = servletContext;
        this.servletConfig = new ServletConfigImpl(servletName, servletContext);
    }

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        if (urlPatterns == null || urlPatterns.length == 0) {
            throw new IllegalArgumentException("urlPatterns cannot be empty!");
        }
        if (servletContext.initialized) {
            throw new IllegalStateException("The ServletContext from which this ServletRegistration " +
                    "was obtained has already been initialized!");
        }
        HashSet<String> mappedUrlPatternSet = new HashSet<>();
        for (String urlPattern : urlPatterns) {
            boolean added = mappedUrlPatterns.add(urlPattern);
            if (added) {
                mappings.add(urlPattern);
            } else {
                if (!mappings.contains(urlPattern)) {
                    mappedUrlPatternSet.add(urlPattern);
                }
            }
        }
        return mappedUrlPatternSet;
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
        this.loadOnStartup = loadOnStartup;
    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement constraint) {
        return Set.of();
    }

    @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfig) {
        if (multipartConfig == null) {
            throw new IllegalStateException("multipartConfig cannot be null!");
        }
        if (servletContext.initialized) {
            throw new IllegalStateException("The ServletContext from which this ServletRegistration was obtained " +
                    "has already been initialized");
        }
        multipartConfigElement = multipartConfig;
    }

    @Override
    public void setRunAsRole(String roleName) {

    }

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        if (servletContext.initialized) {
            throw new IllegalStateException("The ServletContext from which this dynamic Registration was obtained " +
                    "has already been initialized!");
        }
        asyncSupported = isAsyncSupported;
    }

    @Override
    public int compareTo(ServletRegistrationImpl o) {
        return Integer.compare(this.loadOnStartup, o.loadOnStartup);
    }
}
