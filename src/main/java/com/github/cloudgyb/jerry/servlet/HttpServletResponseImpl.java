package com.github.cloudgyb.jerry.servlet;

import com.github.cloudgyb.jerry.ServerInfo;
import com.github.cloudgyb.jerry.util.DateUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;

/**
 * @author cloudgyb
 * @since 2025/2/12 21:52
 */
public class HttpServletResponseImpl implements HttpServletResponse {
    boolean isCommit = false;
    private final HttpExchange httpExchange;
    private final HttpServletRequestImpl requestImpl;
    private final Headers responseHeaders;
    private int statusCode = HttpServletResponse.SC_OK;
    private String characterEncoding = "utf-8";
    private String contentType = null;
    private long contentLength = -1;
    private final ServletOutputStream outputStream;
    private Locale locale = Locale.getDefault();

    public HttpServletResponseImpl(HttpExchange httpExchange, HttpServletRequestImpl requestImpl) {
        this.httpExchange = httpExchange;
        this.requestImpl = requestImpl;
        this.responseHeaders = httpExchange.getResponseHeaders();
        this.outputStream = new ServletOutputStreamImpl(this.httpExchange, this);
    }

    void commit() {
        if (isCommit)
            return;
        try {
            responseHeaders.set("Server", ServerInfo.serverNameAndVersion);
            long CL = contentLength;
            String contentLen = responseHeaders.getFirst("Content-Length");
            if (contentLen != null && !contentLen.isEmpty()) {
                try {
                    responseHeaders.remove("Content-Length");
                    CL = Long.parseLong(contentLen);
                } catch (Exception ignore) {
                }
            }
            if (contentType != null) {
                responseHeaders.set("Content-Type", contentType);
            }
            HttpSession session = requestImpl.getSession(false);
            if (session != null && session.isNew()) {
                ServletContextImpl servletContext = (ServletContextImpl) requestImpl.getServletContext();
                Cookie sessionCookie = new Cookie(servletContext.sessionManager.SESSION_ID_KEY, session.getId());
                sessionCookie.setPath(servletContext.getContextPath());
                sessionCookie.setDomain(requestImpl.getServerName());
                sessionCookie.setHttpOnly(true);
                addCookie(sessionCookie);
            }
            httpExchange.sendResponseHeaders(statusCode, CL);
            isCommit = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addCookie(Cookie cookie) {
        if (isCommit) {
            return;
        }
        responseHeaders.add("Set-Cookie", CookieEncoder.encode(cookie));
    }

    @Override
    public boolean containsHeader(String name) {
        return responseHeaders.containsKey(name);
    }

    @Override
    public String encodeURL(String url) {
        return url;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        statusCode = sc;
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        responseHeaders.set("Content-Type", "text/html; charset=utf-8");
        contentLength = bytes.length;
        commit();
        httpExchange.getResponseBody().write(bytes);
        httpExchange.getResponseBody().flush();
        httpExchange.getResponseBody().close();
        httpExchange.close();
    }

    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, "");
    }

    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
        statusCode = sc;
        responseHeaders.set("Location", location);
        httpExchange.sendResponseHeaders(sc, -1);
        httpExchange.getRequestBody().close();
        isCommit = true;
    }

    @Override
    public void setDateHeader(String name, long date) {
        if (isCommit) {
            return;
        }
        if (name == null || name.isEmpty()) {
            return;
        }
        responseHeaders.set(name, DateUtil.getDateRFC5322(date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        if (isCommit) {
            return;
        }
        if (name == null || name.isEmpty()) {
            return;
        }
        responseHeaders.add(name, DateUtil.getDateRFC5322(date));
    }

    @Override
    public void setHeader(String name, String value) {
        if (isCommit) {
            return;
        }
        if (name == null || name.isEmpty()) {
            return;
        }
        if (value == null) {
            responseHeaders.remove(name);
            return;
        }
        responseHeaders.set(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        if (isCommit) {
            return;
        }
        responseHeaders.add(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        if (name == null) {
            return;
        }
        if (isCommit) {
            return;
        }
        responseHeaders.set(name, "" + value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (name == null) {
            return;
        }
        if (isCommit) {
            return;
        }
        responseHeaders.add(name, value + "");
    }

    @Override
    public void setStatus(int sc) {
        if (isCommit) {
            return;
        }
        statusCode = sc;
    }

    @Override
    public int getStatus() {
        return statusCode;
    }

    @Override
    public String getHeader(String name) {
        return responseHeaders.getFirst(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return responseHeaders.get(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return responseHeaders.keySet();
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(outputStream, true, Charset.forName(getCharacterEncoding()));
    }

    @Override
    public void setCharacterEncoding(String encoding) {
        characterEncoding = encoding;
    }

    @Override
    public void setContentLength(int len) {
        setContentLengthLong(len);
    }

    @Override
    public void setContentLengthLong(long len) {
        if (isCommit) {
            return;
        }
        contentLength = len;
    }

    @Override
    public void setContentType(String type) {
        contentType = type;
    }

    @Override
    public void setBufferSize(int size) {
        if (isCommit) {
            throw new IllegalStateException("The response is commited!");
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (!isCommit) {
            commit();
        }
        httpExchange.getResponseBody().flush();
    }

    @Override
    public void resetBuffer() {
        if (isCommit)
            throw new IllegalStateException();
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCommitted() {
        return isCommit;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setLocale(Locale loc) {
        if (isCommit) {
            return;
        }
        locale = loc;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }
}
