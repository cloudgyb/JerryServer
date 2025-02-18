package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author geng
 * @since 2025/02/12 16:26:56
 */
public class ServletContextImpl implements ServletContext {
    private final static int DEFAULT_SESSION_TIMEOUT = 24 * 3600; // one day
    private final String contextPath;
    private final Map<String, Object> attributes;
    private final Map<String, Servlet> nameToServletMap;
    private final Map<String, ServletRegistration> nameToServletRegistrationMap;
    final List<ServletMapping> servletMappings;
    private String requestCharacterEncoding;
    private String responseCharacterEncoding;
    boolean initialized = false;
    private int sessionTimeout = DEFAULT_SESSION_TIMEOUT;
    final HttpSessionManager sessionManager;

    public ServletContextImpl(String contextPath) {
        this.contextPath = contextPath;
        this.attributes = new HashMap<>();
        this.nameToServletMap = new HashMap<>();
        this.nameToServletRegistrationMap = new HashMap<>();
        this.requestCharacterEncoding = StandardCharsets.UTF_8.toString();
        this.responseCharacterEncoding = StandardCharsets.UTF_8.toString();
        this.servletMappings = new ArrayList<>();
        this.sessionManager = new HttpSessionManager();
    }

    public ServletContextImpl(String contextPath, int sessionTimeout) {
        this(contextPath);
        this.sessionTimeout = sessionTimeout;
    }

    public void init(Set<Class<? extends Servlet>> servletClasses) {
        for (Class<? extends Servlet> servletClass : servletClasses) {
            WebServlet annotation = servletClass.getAnnotation(WebServlet.class);
            if (annotation == null) {
                continue;
            }
            String servletName = annotation.name() == null || annotation.name().isEmpty() ?
                    servletClass.getSimpleName() : annotation.name();
            addServlet(servletName, servletClass);
        }
        initialized = true;
    }

    public void process(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        try {
            Servlet servlet = null;
            for (ServletMapping servletMapping : servletMappings) {
                boolean match = servletMapping.match(requestURI);
                if (match) {
                    servlet = servletMapping.servlet;
                    break;
                }
            }
            if (servlet != null) {
                try {
                    servlet.service(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                String resBody404 = "404 NOT FOUND!";
                try {
                    response.sendError(404, resBody404);
                    response.getOutputStream().close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            try {
                response.getOutputStream().close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public ServletContext getContext(String uriPath) {
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 6;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 6;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 1;
    }

    @Override
    public String getMimeType(String file) {
        return "";
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return Set.of();
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public void log(String msg) {

    }

    @Override
    public void log(String message, Throwable throwable) {

    }

    @Override
    public String getRealPath(String path) {
        return "";
    }

    @Override
    public String getServerInfo() {
        return "";
    }

    @Override
    public String getInitParameter(String name) {
        return "";
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return null;
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object object) {
        attributes.put(name, object);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public String getServletContextName() {
        return "";
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        try {
            Class<?> aClass = Class.forName(className);
            if (!Servlet.class.isAssignableFrom(aClass)) {
                throw new RuntimeException(aClass + " isn't a Servlet");
            }
            return addServlet(servletName, (Class<? extends Servlet>) aClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        ServletRegistrationImpl servletRegistration =
                new ServletRegistrationImpl(servletName, servlet, this);
        nameToServletRegistrationMap.put(servletName, servletRegistration);
        WebServlet annotation = servlet.getClass().getAnnotation(WebServlet.class);
        if (annotation != null) {
            String[] strings = annotation.urlPatterns();
            if (strings != null) {
                servletRegistration.addMapping(strings);
            }
            WebInitParam[] webInitParams = annotation.initParams();
            if (webInitParams != null) {
                for (WebInitParam webInitParam : webInitParams) {
                    servletRegistration.setInitParameter(webInitParam.name(), webInitParam.value());
                }
            }
        }
        try {
            servlet.init(servletRegistration.servletConfig);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        nameToServletMap.put(servletName, servlet);
        Collection<String> mappings = servletRegistration.getMappings();
        for (String mapping : mappings) {
            servletMappings.add(new ServletMapping(servlet, mapping));
        }
        return servletRegistration;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        try {
            Constructor<? extends Servlet> constructor = servletClass.getConstructor();
            Servlet servlet = constructor.newInstance();
            return addServlet(servletName, servlet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        try {
            return clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return nameToServletRegistrationMap.get(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return nameToServletRegistrationMap;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Map.of();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return Set.of();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return Set.of();
    }

    @Override
    public void addListener(String className) {

    }

    @Override
    public <T extends EventListener> void addListener(T t) {

    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {

    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void declareRoles(String... roleNames) {

    }

    @Override
    public String getVirtualServerName() {
        return "";
    }

    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        if (initialized) {
            throw new IllegalStateException("Servlet Context initialized");
        }
        this.sessionTimeout = sessionTimeout;
    }

    @Override
    public String getRequestCharacterEncoding() {
        return requestCharacterEncoding;
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        if (initialized) {
            throw new IllegalStateException("Servlet Context initialized");
        }
        requestCharacterEncoding = encoding;
    }

    @Override
    public String getResponseCharacterEncoding() {
        return responseCharacterEncoding;
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        if (initialized) {
            throw new IllegalStateException("Servlet Context initialized");
        }
        responseCharacterEncoding = encoding;
    }
}
