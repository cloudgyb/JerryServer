package com.github.cloudgyb.jerry.http;

import com.github.cloudgyb.jerry.servlet.HttpServletRequestImpl;
import com.github.cloudgyb.jerry.servlet.HttpServletResponseImpl;
import com.github.cloudgyb.jerry.servlet.ServletContextImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * @author cloudgyb
 * @since 2025/2/10 20:51
 */
public class JerryHttpHandler implements HttpHandler {
    private final ServletContextImpl servletContext;

    public JerryHttpHandler(ServletContextImpl servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpServletRequestImpl httpServletRequest = new HttpServletRequestImpl(exchange, servletContext);
        HttpServletResponseImpl httpServletResponse = new HttpServletResponseImpl(exchange);

    }
}
