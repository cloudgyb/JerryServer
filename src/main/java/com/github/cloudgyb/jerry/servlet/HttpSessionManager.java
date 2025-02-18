package com.github.cloudgyb.jerry.servlet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author geng
 * @since 2025/02/18 13:59:19
 */
public class HttpSessionManager implements Runnable {
    String SESSION_ID_KEY = "jsessionid";
    private final Map<String, HttpSessionImpl> sessionMap;

    public HttpSessionManager() {
        this.sessionMap = new ConcurrentHashMap<>();
        new Thread(this).start();
    }

    public HttpSessionImpl getSession(String id) {
        return sessionMap.get(id);
    }

    public HttpSessionImpl createSession(ServletContextImpl servletContext) {
        HttpSessionImpl httpSession = new HttpSessionImpl(this, servletContext);
        sessionMap.put(httpSession.getId(), httpSession);
        return httpSession;
    }

    public void removeSession(String id) {
        sessionMap.remove(id);
    }

    @Override
    public void run() {
        for (HttpSessionImpl session : sessionMap.values()) {
            int maxInactiveInterval = session.getMaxInactiveInterval();
            if (maxInactiveInterval == -1) {
                return;
            }
            long lastAccessedTime = session.getLastAccessedTime();
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAccessedTime > maxInactiveInterval * 1000L) {
                session.invalidate();
            }
        }
    }
}
