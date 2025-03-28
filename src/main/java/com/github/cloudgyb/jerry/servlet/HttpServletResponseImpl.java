package com.github.cloudgyb.jerry.servlet;

import com.github.cloudgyb.jerry.ServerInfo;
import com.github.cloudgyb.jerry.servlet.buffer.OutputBuffer;
import com.github.cloudgyb.jerry.util.DateUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Standard implement for HttpServletResponse interface
 *
 * @author cloudgyb
 * @since 2025/2/12 21:52
 */
public class HttpServletResponseImpl implements HttpServletResponse {
    private final static String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";
    private static final Logger log = LoggerFactory.getLogger(HttpServletResponseImpl.class);
    boolean isCommit = false;
    private final HttpExchange httpExchange;
    private final HttpServletRequestImpl requestImpl;
    private final Headers responseHeaders;
    private int statusCode = HttpServletResponse.SC_OK;
    private String characterEncoding;
    private String contentType = null;
    private long contentLength = -1;
    private int bufferSize = 4096;
    private final OutputBuffer outputBuffer;
    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private Locale locale = Locale.getDefault();

    public HttpServletResponseImpl(HttpExchange httpExchange, HttpServletRequestImpl requestImpl) {
        this.httpExchange = httpExchange;
        this.requestImpl = requestImpl;
        this.responseHeaders = httpExchange.getResponseHeaders();
        this.outputBuffer = new OutputBuffer(bufferSize, httpExchange.getResponseBody());
        this.outputBuffer.setBufferFlushLister(this::commit);
        this.characterEncoding = requestImpl.getServletContext().getResponseCharacterEncoding();
    }

    void end() throws IOException {
        if (isCommit)
            return;
        int count = outputBuffer.getCount();
        if (count == 0) {
            contentLength = -1; // Content-Length is 0
        } else {
            contentLength = count;
        }
        commit();
        outputBuffer.close();
        httpExchange.close();
    }

    private void commit() {
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
            // Methods to write data were not called.
            if (outputStream == null && writer == null) {
                CL = -1; // Content-Length is 0
            } else if (CL == -1) {
                CL = 0; // Use chunked encoding
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
        checkIfCommitted();
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
        checkIfCommitted();
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
        return Objects.requireNonNullElse(characterEncoding, DEFAULT_CHARACTER_ENCODING);
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called!");
        }
        if (outputStream == null) {
            this.outputStream = new ServletOutputStreamImpl(this.outputBuffer);
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called!");
        }
        if (writer == null) {
            OutputStreamWriter out = new OutputStreamWriter(
                    outputBuffer,
                    Charset.forName(getCharacterEncoding())
            );
            writer = new PrintWriter(out, false);
        }
        return writer;
    }

    @Override
    public void setCharacterEncoding(String encoding) {
        if (isCommit || writer != null) { //Return if the response has already been committed or getWriter has been called.
            log.warn("The encoding setting will have no effect once " +
                    "the response is committed or getWriter() has been called.");
            return;
        }
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
        checkIfCommitted();
        outputBuffer.setSize(size);
        bufferSize = size;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public void flushBuffer() throws IOException {
        outputBuffer.flush();
    }

    @Override
    public void resetBuffer() {
        checkIfCommitted();
        outputBuffer.clear();
    }

    @Override
    public boolean isCommitted() {
        return isCommit;
    }

    @Override
    public void reset() {
        checkIfCommitted();
        outputBuffer.clear();
        statusCode = 200;
        responseHeaders.clear();
    }

    @Override
    public void setLocale(Locale loc) {
        if (isCommit) {
            log.warn("The locale setting will have no effect once the response is committed.");
            return;
        }
        locale = loc;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    private void checkIfCommitted() {
        if (isCommit) {
            throw new IllegalStateException("The response has already been committed!");
        }
    }
}
