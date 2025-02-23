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
    private final List<Filter> filters;
    private int index = -1;

    public FilterChainImpl(List<Filter> filterList) {
        filters = new ArrayList<>();
        filters.addAll(filterList);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        index++;
        if (index >= filters.size()) {
            return;
        }

        Filter filter = filters.get(index);
        filter.doFilter(request, response, this);
    }

    public boolean isPassed() {
        return index >= filters.size();
    }

}
