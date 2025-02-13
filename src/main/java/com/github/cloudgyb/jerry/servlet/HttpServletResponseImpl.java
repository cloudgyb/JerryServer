package com.github.cloudgyb.jerry.servlet;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * @author cloudgyb
 * @since 2025/2/12 21:52
 */
public class HttpServletResponseImpl implements HttpServletResponse {
    private boolean isCommit = false;
    private final HttpExchange httpExchange;
    private final Headers responseHeaders;

    public HttpServletResponseImpl(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
        this.responseHeaders = httpExchange.getResponseHeaders();
    }

    @Override
    public void addCookie(Cookie cookie) {
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
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        responseHeaders.add("Content-Type", "text/html; charset=utf-8");
        httpExchange.sendResponseHeaders(sc, bytes.length);
        httpExchange.getResponseBody().write(bytes);
        httpExchange.getResponseBody().flush();
    }

    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, "");
    }

    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {

    }

    @Override
    public void setDateHeader(String name, long date) {

    }

    @Override
    public void addDateHeader(String name, long date) {

    }

    @Override
    public void setHeader(String name, String value) {

    }

    @Override
    public void addHeader(String name, String value) {

    }

    @Override
    public void setIntHeader(String name, int value) {

    }

    @Override
    public void addIntHeader(String name, int value) {

    }

    @Override
    public void setStatus(int sc) {

    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public String getHeader(String name) {
        return "";
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return List.of();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return List.of();
    }

    @Override
    public String getCharacterEncoding() {
        return "";
    }

    @Override
    public String getContentType() {
        return "";
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return null;
    }

    @Override
    public void setCharacterEncoding(String encoding) {

    }

    @Override
    public void setContentLength(int len) {

    }

    @Override
    public void setContentLengthLong(long len) {

    }

    @Override
    public void setContentType(String type) {

    }

    @Override
    public void setBufferSize(int size) {

    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() {

    }

    @Override
    public void resetBuffer() {

    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setLocale(Locale loc) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }
}
