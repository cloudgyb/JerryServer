package com.github.cloudgyb.jerry.servlet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author geng
 * @since 2025/02/18 13:59:19
 */
public class HttpSessionManager implements Runnable {
    String SESSION_ID_KEY = "jsessionid";
    private final Map<String, HttpSessionImpl> sessionMap;
    private final ScheduledExecutorService sessionTimeoutCheckThread;

    public HttpSessionManager() {
        this.sessionMap = new ConcurrentHashMap<>();
        this.sessionTimeoutCheckThread = Executors.newSingleThreadScheduledExecutor();
        this.sessionTimeoutCheckThread.scheduleAtFixedRate(this, 2, 2, TimeUnit.SECONDS);
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
        System.out.println("Check Session timeout.");
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

    public void destroy() {
        sessionTimeoutCheckThread.shutdown();
        try {
            boolean b = sessionTimeoutCheckThread.awaitTermination(2, TimeUnit.SECONDS);
            if (b) {
                System.out.println("SessionTimeoutCheckThread terminated!");
            } else {
                sessionTimeoutCheckThread.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (HttpSessionImpl session : sessionMap.values()) {
            session.invalidate();
        }
    }
}
