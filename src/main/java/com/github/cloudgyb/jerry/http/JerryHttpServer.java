package com.github.cloudgyb.jerry.http;

import com.github.cloudgyb.jerry.servlet.ServletContextFactory;
import com.github.cloudgyb.jerry.servlet.ServletContextImpl;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Http Server 实现类
 *
 * @author cloudgyb
 * @since 2025/2/10 20:45
 */
public class JerryHttpServer {
    private static final Logger logger = LoggerFactory.getLogger(JerryHttpServer.class);
    private final InetSocketAddress address;
    private final HttpServer httpServer;
    private final Map<String, ServletContextImpl> servletContextMap = new HashMap<>();

    public JerryHttpServer(InetSocketAddress address) throws IOException {
        this.address = address;
        this.httpServer = HttpServer.create(address, 512);
    }

    public JerryHttpServer(int port) throws IOException {
        this(new InetSocketAddress(port));
    }

    public JerryHttpServer(String host, int port) throws IOException {
        this(new InetSocketAddress(host, port));
    }

    public void start() {
        ServletContextImpl servletContext;
        try {
            servletContext = ServletContextFactory.create(Paths.get("D:\\IdeaProjects\\TestServlet\\target\\Test.war"));
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        servletContextMap.put(servletContext.getContextPath(), servletContext);
        JerryHttpHandler jerryHttpHandler = new JerryHttpHandler(servletContext);
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.createContext(servletContext.getContextPath(), jerryHttpHandler);
        httpServer.start();
        if (logger.isInfoEnabled()) {
            logger.info("The JerryServer is running! It is listening at {}:{}",
                    address.getHostString(), address.getPort());
        }
    }

    public void stop() {
        httpServer.stop(2);
        servletContextMap.forEach((k, servletContext) -> {
            logger.debug("Destroy ServletContext: {}", k);
            servletContext.destroy();
        });
        servletContextMap.clear();
        if (logger.isInfoEnabled()) {
            logger.info("The JerryServer is stopped!");
        }
    }

    public ServletContext getServletContext(String contextPath) {
        return servletContextMap.get(contextPath);
    }
}
