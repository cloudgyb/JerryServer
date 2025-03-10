package com.github.cloudgyb.jerry.http;

import com.github.cloudgyb.jerry.servlet.HttpServletRequestImpl;
import com.github.cloudgyb.jerry.servlet.HttpServletResponseImpl;
import com.github.cloudgyb.jerry.servlet.ServletContextImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

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
    public void handle(HttpExchange exchange) {
        //Thread.currentThread().setContextClassLoader(servletContext.getClassLoader());
        try {
            HttpServletRequestImpl httpServletRequest = new HttpServletRequestImpl(exchange, servletContext);
            HttpServletResponseImpl httpServletResponse = new HttpServletResponseImpl(exchange, httpServletRequest);
            servletContext.process(httpServletRequest, httpServletResponse);
        }finally {
            //Thread.currentThread().setContextClassLoader(null);
        }
    }
}
