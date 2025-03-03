package com.github.cloudgyb.jerry.servlet;

import com.github.cloudgyb.jerry.ServerInfo;
import com.github.cloudgyb.jerry.http.MimeTypes;
import com.github.cloudgyb.jerry.servlet.filter.FilterChainImpl;
import com.github.cloudgyb.jerry.servlet.filter.FilterConfigImpl;
import com.github.cloudgyb.jerry.servlet.filter.FilterMapping;
import com.github.cloudgyb.jerry.servlet.filter.FilterRegistrationImpl;
import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Servlet Context 实现类
 *
 * @author geng
 * @since 2025/02/12 16:26:56
 */
public class ServletContextImpl implements ServletContext {
    private final static int DEFAULT_SESSION_TIMEOUT = 24 * 3600; // one day
    private final String contextPath;
    private final Logger logger;
    private final Map<String, Object> attributes;
    // Servlet info store
    final List<ServletMapping> servletMappings;
    private final Map<String, Servlet> nameToServletMap;
    private final Map<String, ServletRegistrationImpl> nameToServletRegistrationMap;
    // Filter info store
    final List<FilterMapping> filterMappings;
    private final Map<String, Filter> nameToFilterMap;
    private final Map<String, FilterRegistrationImpl> nameToFilterRegistrationMap;
    private final Map<String, Set<Filter>> servletNameToFilterMap;
    // Encoding
    private String requestCharacterEncoding;
    private String responseCharacterEncoding;
    boolean initialized = false;
    private int sessionTimeout = DEFAULT_SESSION_TIMEOUT;
    // session manager
    final HttpSessionManager sessionManager;
    // init params
    private final Map<String, String> initParams;
    String webApplicationDisplayName = null;

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
        this.initParams = new HashMap<>();
        this.logger = LoggerFactory.getLogger(contextPath);
    }

    public ServletContextImpl(String contextPath, int sessionTimeout) {
        this(contextPath);
        this.sessionTimeout = sessionTimeout;
    }

    public void init() throws ServletException {
        // 1. For servlet init
        Collection<ServletRegistrationImpl> values = nameToServletRegistrationMap.values();
        ArrayList<ServletRegistrationImpl> servletRegistrations = new ArrayList<>(values);
        // Sort by loadOnStartup
        Collections.sort(servletRegistrations);
        // servlet init
        for (ServletRegistrationImpl servletRegistration : servletRegistrations) {
            Servlet servlet = servletRegistration.servlet;
            int loadOnStartup = servletRegistration.loadOnStartup;
            if (loadOnStartup >= 0) {
                initServlet(servletRegistration, servlet);
            }
            // add mappings to servletMappings
            Collection<String> mappings = servletRegistration.getMappings();
            for (String mapping : mappings) {
                servletMappings.add(new ServletMapping(servletRegistration, servlet, mapping));
            }
        }
        // Sort the servletMappings
        Collections.sort(servletMappings);

        //2. For Filter init
        Collection<FilterRegistrationImpl> filterRegistrations = nameToFilterRegistrationMap.values();
        for (FilterRegistrationImpl filterRegistration : filterRegistrations) {
            FilterConfigImpl filterConfig = filterRegistration.getFilterConfig();
            Filter filter = filterRegistration.getFilter();
            filter.init(filterConfig);
        }
        // Sort the filterMappings
        Collections.sort(filterMappings);

        initialized = true;
    }

    private static void initServlet(ServletRegistrationImpl servletRegistration, Servlet servlet) {
        ServletConfigImpl servletConfig = servletRegistration.servletConfig;
        try {
            servlet.init(servletConfig);
            servletRegistration.initialized = true;
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    public void process(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        try {
            Servlet servlet = null;
            ServletRegistrationImpl servletRegistration = null;
            for (ServletMapping servletMapping : servletMappings) {
                boolean match = servletMapping.match(requestURI);
                if (match) {
                    servlet = servletMapping.servlet;
                    servletRegistration = servletMapping.servletRegistration;
                    break;
                }
            }
            if (servlet != null) {
                if (!servletRegistration.initialized) {
                    initServlet(servletRegistration, servlet);
                }
                String servletName = servletRegistration.getName();
                FilterChainImpl filterChain = createFilterChain(servletName, requestURI);
                filterChain.doFilter(request, response);
            } else {
                String resBody404 = "404 NOT FOUND!";
                try {
                    response.sendError(404, resBody404);
                    response.getOutputStream().close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Throwable e) {
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
        return new FilterChainImpl(objects.toArray(new Filter[0]), nameToServletMap.get(servletName));
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
        if (file == null) {
            return null;
        }
        int period = file.lastIndexOf('.');
        if (period < 0) {
            return null;
        }
        String extension = file.substring(period + 1);
        if (extension.isEmpty()) {
            return null;
        }
        return MimeTypes.get(extension);
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
        if (logger.isInfoEnabled()) {
            logger.info(msg);
        }
    }

    @Override
    public void log(String message, Throwable throwable) {
        if (logger.isErrorEnabled()) {
            logger.error(message, throwable);
        }
    }

    @Override
    public String getRealPath(String path) {
        return "";
    }

    @Override
    public String getServerInfo() {
        return ServerInfo.serverNameAndVersion;
    }

    @Override
    public String getInitParameter(String name) {
        if (name == null) {
            throw new NullPointerException("The name argument cannot be null!");
        }
        return initParams.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        HashSet<String> strings = new HashSet<>(initParams.keySet());
        return Collections.enumeration(strings);
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        if (initialized) {
            throw new IllegalStateException("The ServletContext has already been initialized!");
        }
        if (name == null) {
            throw new NullPointerException("The name argument cannot be null!");
        }
        String s = initParams.putIfAbsent(name, value);
        return s == null;
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
        return webApplicationDisplayName;
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
        Class<? extends Servlet> servletClass = servlet.getClass();
        WebServlet annotation = servletClass.getAnnotation(WebServlet.class);
        if (annotation != null) {
            // urlPattern
            String[] urlPatterns = annotation.urlPatterns();
            if (urlPatterns != null && urlPatterns.length > 0) {
                servletRegistration.addMapping(urlPatterns);
            }
            urlPatterns = annotation.value();
            if (urlPatterns != null && urlPatterns.length > 0) {
                servletRegistration.addMapping(urlPatterns);
            }
            // initParams
            WebInitParam[] webInitParams = annotation.initParams();
            if (webInitParams != null) {
                for (WebInitParam webInitParam : webInitParams) {
                    servletRegistration.setInitParameter(webInitParam.name(), webInitParam.value());
                }
            }
            // Is async supported?
            boolean isAsyncSupported = annotation.asyncSupported();
            servletRegistration.setAsyncSupported(isAsyncSupported);
        }
        // multipart config
        MultipartConfig multipartConfigAnn = servletClass.getAnnotation(MultipartConfig.class);
        if (multipartConfigAnn != null) {
            MultipartConfigElement multipartConfig = new MultipartConfigElement(multipartConfigAnn.location(), multipartConfigAnn.maxFileSize(),
                    multipartConfigAnn.maxRequestSize(), multipartConfigAnn.fileSizeThreshold());
            servletRegistration.setMultipartConfig(multipartConfig);
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
            if (urlPattern != null && urlPattern.length != 0) {
                filterRegistration.addMappingForUrlPatterns(
                        EnumSet.of(DispatcherType.REQUEST), false, urlPattern
                );
            }
            urlPattern = webFilterAnnotation.urlPatterns();
            if (urlPattern != null && urlPattern.length != 0) {
                filterRegistration.addMappingForUrlPatterns(
                        EnumSet.of(DispatcherType.REQUEST), false, urlPattern
                );
            }

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
