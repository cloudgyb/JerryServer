package com.github.cloudgyb.jerry.servlet;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 会话管理器
 *
 * @author geng
 * @since 2025/02/18 13:59:19
 */
public class HttpSessionManager implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    String SESSION_ID_KEY = "jsessionid";
    private final Map<String, HttpSessionImpl> sessionMap;
    private final ScheduledExecutorService sessionTimeoutCheckThread;

    public HttpSessionManager() {
        this.sessionMap = new ConcurrentHashMap<>();
        this.sessionTimeoutCheckThread = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            int count = 0;

            @Override
            public Thread newThread(@Nonnull Runnable r) {
                return new Thread(r, "HttpSessionManagerThread-" + count++);
            }
        });
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
        if (logger.isDebugEnabled()) {
            logger.debug("Checking timeout sessions.");
        }
        for (HttpSessionImpl session : sessionMap.values()) {
            int maxInactiveInterval = session.getMaxInactiveInterval();
            if (maxInactiveInterval <= 0) { // 永不过时
                return;
            }
            long lastAccessedTime = session.getLastAccessedTime();
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAccessedTime > maxInactiveInterval * 1000L) {
                session.invalidate();
                if (logger.isDebugEnabled()) {
                    logger.debug("Clearing timeout session(id:{})", session.getId());
                }
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
