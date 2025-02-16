package com.github.cloudgyb.jerry.servlet;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.*;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;

/**
 * @author cloudgyb
 * @since 2025/2/10 21:17
 */
public class HttpServletRequestImpl implements HttpServletRequest {
    private static final String NO_CHECK_AUTH_TYPE = "no-check";
    private final HttpExchange httpExchange;
    private final Headers requestHeaders;
    private final ServletContext servletContext;
    private final ServletInputStream servletInputStream;
    private String authType = NO_CHECK_AUTH_TYPE;
    private Cookie[] cookies;
    private final String requestId;
    private final Map<String, String[]> parameterMap;
    private final Map<String, Object> attributeMap;
    private final boolean isSecure;
    private DispatcherType dispatcherType = DispatcherType.REQUEST;
    private String characterEncoding;

    public HttpServletRequestImpl(HttpExchange httpExchange, ServletContext servletContext) {
        this.httpExchange = httpExchange;
        this.requestHeaders = httpExchange.getRequestHeaders();
        this.servletInputStream = new ServletInputStreamImpl(httpExchange.getRequestBody());
        this.servletContext = servletContext;
        this.parameterMap = new HashMap<>();
        this.attributeMap = new HashMap<>();
        this.requestId = UUID.randomUUID().toString();
        this.isSecure = false;
        parseParameters();
    }

    private void parseParameters() {
        String query = httpExchange.getRequestURI().getQuery();
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        String[] split = query.split("&");
        for (String s : split) {
            String[] split1 = s.split("=");
            String k = split1[0];
            String v = "";
            if (split1.length > 1) {
                v = split1[1];
            }
            String[] values = parameterMap.putIfAbsent(k, new String[]{v});
            if (values != null) {
                String[] newValues = new String[values.length + 1];
                System.arraycopy(values, 0, newValues, 0, values.length);
                newValues[values.length] = v;
                parameterMap.put(k, newValues);
            }
        }
    }

    @Override
    public String getAuthType() {
        if (Objects.equals(authType, NO_CHECK_AUTH_TYPE)) {
            determineAuthType();
        }
        return authType;
    }

    /**
     * 仅支持 Basic 和 Digest 认证方式
     */
    private void determineAuthType() {
        List<String> authorization = this.requestHeaders.get("Authorization");
        authType = null;
        if (authorization != null && !authorization.isEmpty()) {
            String auth = authorization.get(0);
            if (auth.startsWith("Basic ")) {
                authType = BASIC_AUTH;
            } else if (auth.startsWith("Digest ")) {
                authType = DIGEST_AUTH;
            }
        }
    }

    @Override
    public Cookie[] getCookies() {
        if (cookies == null) {
            parseCookies();
        }
        return cookies;
    }

    private void parseCookies() {
        List<String> cookieHeaders = this.requestHeaders.get("Cookie");
        ArrayList<Cookie> cookieList = new ArrayList<>();
        for (String cookieHeader : cookieHeaders) {
            cookieHeader = cookieHeader.trim();
            if (cookieHeader.isEmpty()) {
                continue;
            }
            String[] split = cookieHeader.split(";");
            for (String s : split) {
                s = s.trim();
                String[] split1 = s.split("=");
                cookieList.add(new Cookie(split1[0], split1[1]));
            }
        }
        cookies = cookieList.toArray(new Cookie[0]);
    }

    @Override
    public long getDateHeader(String s) {
        String first = this.requestHeaders.getFirst(s);
        if (first == null) {
            return -1;
        }
        try {
            return Long.parseLong(first);
        } catch (NumberFormatException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public String getHeader(String s) {
        return this.requestHeaders.getFirst(s);
    }

    @Override
    public Enumeration<String> getHeaders(String s) {
        List<String> headers = requestHeaders.get(s);
        return headers == null ? null : Collections.enumeration(headers);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> headerNames = requestHeaders.keySet();
        return Collections.enumeration(headerNames);
    }

    @Override
    public int getIntHeader(String s) {
        String first = this.requestHeaders.getFirst(s);
        if (first == null) {
            return -1;
        }
        return Integer.parseInt(first);
    }

    @Override
    public String getMethod() {
        return httpExchange.getRequestMethod();
    }

    @Override
    public String getPathInfo() {
        return httpExchange.getRequestURI().getPath();
    }

    @Override
    public String getPathTranslated() {
        return getPathInfo();
    }

    @Override
    public String getContextPath() {
        return httpExchange.getHttpContext().getPath();
    }

    @Override
    public String getQueryString() {
        return httpExchange.getRequestURI().getQuery();
    }

    @Override
    public String getRemoteUser() {
        return "";
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return "";
    }

    @Override
    public String getRequestURI() {
        return httpExchange.getRequestURI().toString();
    }

    @Override
    public StringBuffer getRequestURL() {
        String s = httpExchange.getRequestURI().toString();
        int i = s.indexOf("?");
        s = s.substring(0, i);
        return new StringBuffer(s);
    }

    @Override
    public String getServletPath() {
        return httpExchange.getRequestURI().getPath();
    }

    @Override
    public HttpSession getSession(boolean b) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return "";
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String s, String s1) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        String requestMethod = httpExchange.getRequestMethod();
        if (!"POST".equals(requestMethod)) {
            return Collections.emptyList();
        }
        String contentType = requestHeaders.getFirst("Content-Type");
        if (contentType == null) {
            return Collections.emptyList();
        }
        if (contentType.startsWith("multipart/form-data")) {
            InputStream requestBody = httpExchange.getRequestBody();

        }
        return Collections.emptyList();
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String s) {
        return attributeMap.get(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributeMap.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
        try {
            Charset.forName(encoding);
        } catch (Exception e) {
            throw new UnsupportedEncodingException(e.getMessage());
        }
        characterEncoding = encoding;
    }

    @Override
    public int getContentLength() {
        try {
            return httpExchange.getRequestBody().available();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getContentLengthLong() {
        return getContentLength();
    }

    @Override
    public String getContentType() {
        return requestHeaders.getFirst("Content-Type");
    }

    @Override
    public ServletInputStream getInputStream() {
        return servletInputStream;
    }

    @Override
    public String getParameter(String s) {
        String[] params = parameterMap.get(s);
        return params == null ? null : params[0];
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String s) {
        return parameterMap.get(s);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public String getProtocol() {
        return httpExchange.getProtocol();
    }

    @Override
    public String getScheme() {
        return httpExchange.getRequestURI().getScheme();
    }

    @Override
    public String getServerName() {
        return httpExchange.getRequestURI().getHost();
    }

    @Override
    public int getServerPort() {
        return httpExchange.getRequestURI().getPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()));
    }

    @Override
    public String getRemoteAddr() {
        return httpExchange.getRemoteAddress().toString();
    }

    @Override
    public String getRemoteHost() {
        return httpExchange.getRemoteAddress().getHostName();
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributeMap.put(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        attributeMap.remove(s);
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return httpExchange.getRequestURI().getPort();
    }

    @Override
    public String getLocalName() {
        return httpExchange.getHttpContext().getServer().getAddress().getHostName();
    }

    @Override
    public String getLocalAddr() {
        return httpExchange.getHttpContext().getServer().getAddress().getHostString();
    }

    @Override
    public int getLocalPort() {
        return httpExchange.getHttpContext().getServer().getAddress().getPort();
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return dispatcherType;
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    @Override
    public String getProtocolRequestId() {
        return "";
    }

    @Override
    public ServletConnection getServletConnection() {
        return null;
    }
}
