package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

import java.util.*;

/**
 * @author geng
 * @since 2025/02/12 14:38:45
 */
public class HttpSessionImpl implements HttpSession {
    private final ServletContext servletContext;
    private final HttpSessionManager sessionManager;
    private final long creationTime;
    private volatile long lastAccessedTime;
    private final String id;
    private volatile boolean isNew = true;
    private final Map<String, Object> attributes;
    private volatile int maxInactiveInterval;
    private boolean invalidated = false;

    public HttpSessionImpl(HttpSessionManager sessionManager, ServletContext servletContext) {
        this.sessionManager = sessionManager;
        this.servletContext = servletContext;
        this.attributes = new HashMap<>();
        this.id = UUID.randomUUID().toString().replace("-", "");
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
        this.maxInactiveInterval = servletContext.getSessionTimeout() * 60;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
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
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public void invalidate() {
        invalidated = true;
        attributes.clear();
        sessionManager.removeSession(getId());
    }

    @Override
    public boolean isNew() {
        if (invalidated) {
            throw new IllegalStateException("The session is invalidated!");
        }
        return isNew;
    }

    void changeToOld() {
        isNew = false;
    }
}
