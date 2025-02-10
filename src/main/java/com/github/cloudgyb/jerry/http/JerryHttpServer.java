package com.github.cloudgyb.jerry.http;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author cloudgyb
 * @since 2025/2/10 20:45
 */
public class JerryHttpServer {
    public static void main(String[] args) throws IOException {
        JerryHttpHandler jerryHttpHandler = new JerryHttpHandler();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(8888);
        HttpServer httpServer = HttpServer.create(inetSocketAddress, 512);
        Executor executor = httpServer.getExecutor();
        System.out.println(executor);
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.createContext("/", jerryHttpHandler);
        httpServer.start();
    }
}
