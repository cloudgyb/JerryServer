package com.github.cloudgyb.jerry.http;

import com.github.cloudgyb.jerry.servlet.DefaultServlet;
import com.github.cloudgyb.jerry.servlet.HelloServlet;
import com.github.cloudgyb.jerry.servlet.ServletContextImpl;
import com.github.cloudgyb.jerry.servlet.filter.TestFilter;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * @author cloudgyb
 * @since 2025/2/10 20:45
 */
public class JerryHttpServer {
    private static final String defaultContextPath = "/";
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
        ServletContextImpl servletContext = createDefaultServletContext();
        servletContextMap.put(servletContext.getContextPath(), servletContext);
        JerryHttpHandler jerryHttpHandler = new JerryHttpHandler(servletContext);
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.createContext(servletContext.getContextPath(), jerryHttpHandler);
        httpServer.start();
        System.out.printf("JerryServer is running! It listen at %s:%d%n",
                address.getHostString(), address.getPort());
    }

    public void stop() {
        httpServer.stop(2);
        servletContextMap.clear();
        System.out.println("JerryServer is stopped!");
    }

    private static ServletContextImpl createDefaultServletContext() {
        ServletContextImpl servletContext = new ServletContextImpl(defaultContextPath);
        ServletRegistration.Dynamic dynamic = servletContext.addServlet("default", DefaultServlet.class);
        dynamic.addMapping("/");

        servletContext.addServlet("HelloServlet", HelloServlet.class);
        servletContext.addFilter("TestFilter", TestFilter.class);
        try {
            servletContext.init();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return servletContext;
    }

    public ServletContext getServletContext(String contextPath) {
        return servletContextMap.get(contextPath);
    }
}
