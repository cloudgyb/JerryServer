package com.github.cloudgyb.jerry;

import com.github.cloudgyb.jerry.loader.WebAppClassLoader;
import com.github.cloudgyb.jerry.servlet.HttpSessionManager;
import com.github.cloudgyb.jerry.servlet.ServletContextImpl;
import junit.framework.TestCase;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cloudgyb
 * @since 2025/2/25 21:27
 */
public class SessionTimeoutTest extends TestCase {
    public void test() throws InterruptedException {
        ServletContextImpl servletContext = new ServletContextImpl("/", new WebAppClassLoader(new URL[0]));
        HttpSessionManager httpSessionManager = new HttpSessionManager(servletContext);
        httpSessionManager.createSession();
        httpSessionManager.createSession();
        httpSessionManager.createSession();
        httpSessionManager.createSession();
        Thread.sleep(5000);
        httpSessionManager.destroy();
    }

    public void test1() {
        Map<String, Integer> map = new ConcurrentHashMap<>();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);
        map.put("4", 4);
        for (Integer value : map.values()) {
            map.remove(value + "");
        }
    }
}
