package com.github.cloudgyb.jerry.servlet.filter;

import jakarta.servlet.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * FilterChain 简单实现
 *
 * @author cloudgyb
 * @since 2025/2/23 14:07
 */
public class FilterChainImpl implements FilterChain {
    private final Filter[] filters;
    private int index = 0;
    private final Servlet servlet;

    public FilterChainImpl(Filter[] filters, Servlet servlet) {
        this.filters = filters;
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (index < filters.length) {
            Filter filter = filters[index++];
            filter.doFilter(request, response, this);
            return;
        }
        servlet.service(request, response);
    }
}
