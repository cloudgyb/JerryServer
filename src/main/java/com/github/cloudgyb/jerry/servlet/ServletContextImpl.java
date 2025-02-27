package com.github.cloudgyb.jerry.servlet;

import com.github.cloudgyb.jerry.servlet.filter.FilterChainImpl;
import com.github.cloudgyb.jerry.servlet.filter.FilterMapping;
import com.github.cloudgyb.jerry.servlet.filter.FilterRegistrationImpl;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author geng
 * @since 2025/02/12 16:26:56
 */
public class ServletContextImpl implements ServletContext {
    private final static int DEFAULT_SESSION_TIMEOUT = 24 * 3600; // one day
    private final String contextPath;
    private final Map<String, Object> attributes;
    // Servlet info store
    final List<ServletMapping> servletMappings;
    private final Map<String, Servlet> nameToServletMap;
    private final Map<String, ServletRegistrationImpl> nameToServletRegistrationMap;
    // Filter info store
    final List<FilterMapping> filterMappings;
    private final Map<String, Filter> nameToFilterMap;
    private final Map<String, FilterRegistration> nameToFilterRegistrationMap;
    private final Map<String, Set<Filter>> servletNameToFilterMap;
    // Encoding
    private String requestCharacterEncoding;
    private String responseCharacterEncoding;
    boolean initialized = false;
    private int sessionTimeout = DEFAULT_SESSION_TIMEOUT;
    // session manager
    final HttpSessionManager sessionManager;

    public ServletContextImpl(String contextPath) {
        this.contextPath = contextPath;
        this.attributes = new ConcurrentHashMap<>();
        this.servletMappings = new ArrayList<>();
        this.nameToServletMap = new HashMap<>();
        this.nameToServletRegistrationMap = new HashMap<>();
        this.filterMappings = new ArrayList<>();
        this.nameToFilterMap = new HashMap<>();
        this.nameToFilterRegistrationMap = new HashMap<>();
        this.servletNameToFilterMap = new HashMap<>();
        this.requestCharacterEncoding = StandardCharsets.UTF_8.toString();
        this.responseCharacterEncoding = StandardCharsets.UTF_8.toString();
        this.sessionManager = new HttpSessionManager();
    }

    public ServletContextImpl(String contextPath, int sessionTimeout) {
        this(contextPath);
        this.sessionTimeout = sessionTimeout;
    }

    public void init() {
        // For servlet init
        Collection<ServletRegistrationImpl> values = nameToServletRegistrationMap.values();
        ArrayList<ServletRegistrationImpl> servletRegistrations = new ArrayList<>(values);
        // Sort by loadOnStartup
        Collections.sort(servletRegistrations);
        // servlet init
        for (ServletRegistrationImpl servletRegistration : servletRegistrations) {
            Servlet servlet = servletRegistration.servlet;
            int loadOnStartup = servletRegistration.loadOnStartup;
            if (loadOnStartup >= 0) {
                ServletConfigImpl servletConfig = servletRegistration.servletConfig;
                try {
                    servlet.init(servletConfig);
                } catch (ServletException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        // Sort the servletMappings
        Collections.sort(servletMappings);

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
                String servletName = servlet.getServletConfig().getServletName();
                FilterChainImpl filterChain = createFilterChain(servletName, requestURI);
                filterChain.doFilter(request, response);
                if (!filterChain.isPassed()) {
                    return;
                }
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
        } catch (ServletException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                response.flushBuffer();
                response.getOutputStream().close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private FilterChainImpl createFilterChain(String servletName, String requestURI) {
        ArrayList<FilterMapping> matchedFilterMappings = new ArrayList<>();
        for (FilterMapping filterMapping : filterMappings) {
            boolean match = filterMapping.match(requestURI);
            if (match) {
                matchedFilterMappings.add(filterMapping);
            }
        }
        Collections.sort(matchedFilterMappings);
        Set<Filter> filters = servletNameToFilterMap.get(servletName);
        if (filters == null) {
            filters = Set.of();
        }
        ArrayList<Filter> objects = new ArrayList<>(filters);
        objects.addAll(matchedFilterMappings.stream().map(FilterMapping::getFilter).collect(Collectors.toList()));
        return new FilterChainImpl(objects);
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

    @SuppressWarnings("unchecked")
    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        try {
            Class<?> aClass = Class.forName(className);
            if (!Servlet.class.isAssignableFrom(aClass)) {
                throw new RuntimeException(aClass + " isn't a Servlet");
            }
            return addServlet(servletName, (Class<Servlet>) aClass);
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
            strings = annotation.value();
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
        nameToServletMap.put(servletName, servlet);
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
        return new HashMap<>(nameToServletRegistrationMap);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        try {
            Class<?> aClass = Class.forName(className);
            if (Filter.class.isAssignableFrom(aClass)) {
                return addFilter(filterName, (Class<Filter>) aClass);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        if (initialized) {
            throw new IllegalStateException("The servlet context is initialized!");
        }
        if (filterName == null || filterName.isEmpty()) {
            throw new IllegalArgumentException();
        }
        nameToFilterMap.put(filterName, filter);
        FilterRegistrationImpl filterRegistration = new FilterRegistrationImpl(this, filterName, filter);
        nameToFilterRegistrationMap.put(filterName, filterRegistration);
        WebFilter webFilterAnnotation = filter.getClass().getAnnotation(WebFilter.class);
        if (webFilterAnnotation != null) {
            String[] urlPattern = webFilterAnnotation.value();
            filterRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, urlPattern);
            urlPattern = webFilterAnnotation.urlPatterns();
            filterRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, urlPattern);

            String[] servletNames = webFilterAnnotation.servletNames();
            filterRegistration.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), false, servletNames);

            WebInitParam[] webInitParams = webFilterAnnotation.initParams();
            for (WebInitParam webInitParam : webInitParams) {
                filterRegistration.setInitParameter(webInitParam.name(), webInitParam.value());
            }
        }
        nameToFilterMap.put(filterName, filter);

        return filterRegistration;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        try {
            return addFilter(filterName, createFilter(filterClass));
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        try {
            return clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new ServletException("Create filter exception:" + e);
        }
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return nameToFilterRegistrationMap.get(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return new HashMap<>(nameToFilterRegistrationMap);
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
            throw new IllegalStateException("The ServletContext has already been initialized!");
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
            throw new IllegalStateException("The ServletContext has already been initialized!");
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

    void addServletMappings(Servlet servlet, String urlPattern) {
        servletMappings.add(new ServletMapping(servlet, urlPattern));
        Collections.sort(servletMappings);
    }

    public void addFilterMappingForServletName(String[] servletNames, Filter filter) {
        for (String servletName : servletNames) {
            Set<Filter> set = servletNameToFilterMap.putIfAbsent(servletName, new HashSet<>(Set.of(filter)));
            if (set != null) {
                set.add(filter);
            }
        }
    }

    public void addFilterMapping(String[] urlPatterns, Filter filter) {
        for (String urlPattern : urlPatterns) {
            filterMappings.add(new FilterMapping(filter, urlPattern));
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
