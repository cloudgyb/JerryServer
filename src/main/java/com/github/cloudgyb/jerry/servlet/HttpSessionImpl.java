package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The standard implement of HttpSession interface.
 *
 * @author geng
 * @since 2025/02/12 14:38:45
 */
public class HttpSessionImpl implements HttpSession {
    private ServletContextImpl servletContext;
    private HttpSessionManager sessionManager;
    private final long creationTime;
    private volatile long lastAccessedTime;
    private final String id;
    private volatile boolean isNew = true;
    private final Map<String, Object> attributes;
    private volatile int maxInactiveInterval;
    private boolean invalidated = false;

    public HttpSessionImpl(HttpSessionManager sessionManager, ServletContextImpl servletContext) {
        this.sessionManager = sessionManager;
        this.servletContext = servletContext;
        this.attributes = new ConcurrentHashMap<>();
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
        Object oldValue = attributes.get(name);
        attributes.put(name, value);
        HttpSessionBindingEvent event = new HttpSessionBindingEvent(
                this, name, oldValue != null ? oldValue : value);
        Consumer<HttpSessionAttributeListener> consumer = oldValue == null ?
                (l) -> l.attributeAdded(event) :
                (l) -> l.attributeReplaced(event);
        servletContext.httpSessionAttributeListeners().forEach(consumer);
    }

    @Override
    public void removeAttribute(String name) {
        Object value = attributes.remove(name);
        HttpSessionBindingEvent event = new HttpSessionBindingEvent(this, name, value);
        servletContext.httpSessionAttributeListeners()
                .forEach(l -> l.attributeRemoved(event));
    }

    @Override
    public void invalidate() {
        invalidated = true;
        Set<String> attrNames = attributes.keySet();
        attrNames.forEach(this::removeAttribute);
        attributes.clear();
        sessionManager.removeSession(getId());
        sessionManager = null;
        servletContext = null;
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
