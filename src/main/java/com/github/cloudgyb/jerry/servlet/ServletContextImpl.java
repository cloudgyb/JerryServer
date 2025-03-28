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
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The standard implement of <code>ServletContext</code>.
 *
 * @author geng
 * @since 2025/02/12 16:26:56
 */
public class ServletContextImpl implements ServletContext {
    private final static String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();
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
    private final Map<String, FilterRegistrationImpl> nameToFilterRegistrationMap;
    private final Map<String, Set<Filter>> servletNameToFilterMap;
    // WebListener info store
    private final Set<ServletContextAttributeListener> servletContextAttributeListeners;
    private final Set<ServletRequestListener> servletRequestListeners;
    private final Set<ServletRequestAttributeListener> servletRequestAttributeListeners;
    private final Set<HttpSessionAttributeListener> httpSessionAttributeListeners;
    private final Set<HttpSessionIdListener> httpSessionIdListeners;
    private final Set<HttpSessionListener> httpSessionListeners;
    // Encoding
    private String requestCharacterEncoding;
    private String responseCharacterEncoding;
    boolean initialized = false;
    private int sessionTimeout;
    // session manager
    final HttpSessionManager sessionManager;
    // init params
    private final Map<String, String> initParams;
    String webApplicationDisplayName;
    private final ClassLoader classLoader;

    public ServletContextImpl(String contextPath, ClassLoader classLoader) {
        this(contextPath, classLoader, DEFAULT_SESSION_TIMEOUT,
                DEFAULT_ENCODING, DEFAULT_ENCODING, null);
    }

    public ServletContextImpl(String contextPath, ClassLoader classLoader, int sessionTimeout) {
        this(contextPath, classLoader, sessionTimeout,
                DEFAULT_ENCODING, DEFAULT_ENCODING, null);

    }

    public ServletContextImpl(String contextPath, ClassLoader classLoader,
                              int sessionTimeout,
                              String requestCharacterEncoding, String responseCharacterEncoding,
                              String webApplicationDisplayName) {
        if (contextPath == null || contextPath.isEmpty()) {
            throw new IllegalArgumentException("contextPath cannot be null or empty!");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be null!");
        }
        if (requestCharacterEncoding == null || requestCharacterEncoding.isBlank()) {
            throw new IllegalArgumentException("requestCharacterEncoding cannot be null or blank!");
        }
        if (responseCharacterEncoding == null || responseCharacterEncoding.isBlank()) {
            throw new IllegalArgumentException("responseCharacterEncoding cannot be null or blank!");
        }
        this.contextPath = contextPath;
        this.classLoader = classLoader;
        this.attributes = new ConcurrentHashMap<>();
        this.servletMappings = new ArrayList<>();
        this.nameToServletMap = new HashMap<>();
        this.nameToServletRegistrationMap = new HashMap<>();
        this.filterMappings = new ArrayList<>();
        this.nameToFilterRegistrationMap = new HashMap<>();
        this.servletNameToFilterMap = new HashMap<>();
        this.requestCharacterEncoding = requestCharacterEncoding;
        this.responseCharacterEncoding = responseCharacterEncoding;
        this.sessionManager = new HttpSessionManager(this);
        this.initParams = new HashMap<>();
        this.sessionTimeout = sessionTimeout;
        this.webApplicationDisplayName = webApplicationDisplayName;
        this.logger = LoggerFactory.getLogger(contextPath);
        this.servletContextAttributeListeners = new HashSet<>();
        this.servletRequestListeners = new HashSet<>();
        this.servletRequestAttributeListeners = new HashSet<>();
        this.httpSessionAttributeListeners = new HashSet<>();
        this.httpSessionIdListeners = new HashSet<>();
        this.httpSessionListeners = new HashSet<>();
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
        ServletRequestEvent requestEvent = new ServletRequestEvent(this, request);
        servletRequestListeners.forEach(l -> l.requestInitialized(requestEvent));

        String requestURI = request.getRequestURI();
        requestURI = requestURI.replaceFirst(contextPath, "");
        if (requestURI.isEmpty()) {
            requestURI = "/";
        }
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
            logger.error("process request exception: ", e);
        } finally {
            try {
                if (response instanceof HttpServletResponseImpl) {
                    HttpServletResponseImpl resp = (HttpServletResponseImpl) response;
                    resp.end();
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            servletRequestListeners.forEach(l -> l.requestDestroyed(requestEvent));
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
    public URL getResource(String path) {
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
    public void setAttribute(String name, Object value) {
        Object oldValue = attributes.get(name);
        attributes.put(name, value);
        ServletContextAttributeEvent event = new ServletContextAttributeEvent(
                this, name, oldValue == null ? value : oldValue);
        Consumer<ServletContextAttributeListener> consumer =
                oldValue == null ? (l) -> l.attributeAdded(event) :
                        (l) -> l.attributeReplaced(event);
        servletContextAttributeListeners.forEach(consumer);
    }

    @Override
    public void removeAttribute(String name) {
        Object value = attributes.remove(name);
        ServletContextAttributeEvent event = new ServletContextAttributeEvent(
                this, name, value);
        servletContextAttributeListeners.forEach(l -> l.attributeRemoved(event));
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
    @SuppressWarnings("unchecked")
    public void addListener(Class<? extends EventListener> listenerClass) {
        try {
            if (ServletContextAttributeListener.class.isAssignableFrom(listenerClass)) {
                ServletContextAttributeListener listener =
                        createListener((Class<ServletContextAttributeListener>) listenerClass);
                servletContextAttributeListeners.add(listener);
            } else if (ServletRequestListener.class.isAssignableFrom(listenerClass)) {
                ServletRequestListener listener =
                        createListener((Class<ServletRequestListener>) listenerClass);
                servletRequestListeners.add(listener);
            } else if (ServletRequestAttributeListener.class.isAssignableFrom(listenerClass)) {
                ServletRequestAttributeListener listener =
                        createListener((Class<ServletRequestAttributeListener>) listenerClass);
                servletRequestAttributeListeners.add(listener);
            } else if (HttpSessionAttributeListener.class.isAssignableFrom(listenerClass)) {
                HttpSessionAttributeListener listener =
                        createListener((Class<HttpSessionAttributeListener>) listenerClass);
                httpSessionAttributeListeners.add(listener);
            } else if (HttpSessionIdListener.class.isAssignableFrom(listenerClass)) {
                HttpSessionIdListener listener =
                        createListener((Class<HttpSessionIdListener>) listenerClass);
                httpSessionIdListeners.add(listener);
            } else if (HttpSessionListener.class.isAssignableFrom(listenerClass)) {
                HttpSessionListener listener =
                        createListener((Class<HttpSessionListener>) listenerClass);
                httpSessionListeners.add(listener);
            }
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        try {
            return clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new ServletException("Create listener exception:" + e);
        }
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        if (System.getSecurityManager() != null) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            ClassLoader parent = classLoader;
            while (parent != null) {
                if (parent == tccl) {
                    break;
                }
                parent = parent.getParent();
            }
            if (parent == null) {
                System.getSecurityManager().checkPermission(new RuntimePermission("getClassLoader"));
            }
        }
        return classLoader;
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

    Set<ServletRequestAttributeListener> servletRequestAttributeListeners() {
        return servletRequestAttributeListeners;
    }

    Set<HttpSessionAttributeListener> httpSessionAttributeListeners() {
        return httpSessionAttributeListeners;
    }

    Set<HttpSessionIdListener> httpSessionIdListeners() {
        return httpSessionIdListeners;
    }

    Set<HttpSessionListener> httpSessionListeners() {
        return httpSessionListeners;
    }


    public void destroy() {
        attributes.keySet().forEach(this::removeAttribute);
        sessionManager.destroy();
        for (FilterRegistrationImpl registration : nameToFilterRegistrationMap.values()) {
            registration.getFilter().destroy();
        }
        nameToFilterRegistrationMap.clear();
        for (ServletRegistrationImpl registration : nameToServletRegistrationMap.values()) {
            registration.servlet.destroy();
        }
        nameToServletRegistrationMap.clear();
        servletNameToFilterMap.clear();
        servletMappings.clear();
        filterMappings.clear();
        servletContextAttributeListeners.clear();
        servletRequestListeners.clear();
        servletRequestAttributeListeners.clear();
        httpSessionAttributeListeners.clear();
        httpSessionIdListeners.clear();
        httpSessionListeners.clear();
    }
}
