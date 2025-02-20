package com.github.cloudgyb.jerry.http;

import com.github.cloudgyb.jerry.servlet.DefaultServlet;
import com.github.cloudgyb.jerry.servlet.HelloServlet;
import com.github.cloudgyb.jerry.servlet.ServletContextImpl;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.ServletRegistration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author cloudgyb
 * @since 2025/2/10 20:45
 */
public class JerryHttpServer {
    private static final String contextPath = "/";

    public static void main(String[] args) throws IOException {
        ServletContextImpl servletContext = new ServletContextImpl(contextPath);
        ServletRegistration.Dynamic dynamic = servletContext.addServlet("default", DefaultServlet.class);
        dynamic.addMapping("/");

        servletContext.addServlet("/hello", HelloServlet.class);

        JerryHttpHandler jerryHttpHandler = new JerryHttpHandler(servletContext);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(8888);
        HttpServer httpServer = HttpServer.create(inetSocketAddress, 512);
        Executor executor = httpServer.getExecutor();
        System.out.println(executor);
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.createContext(contextPath, jerryHttpHandler);
        httpServer.start();
    }
}
