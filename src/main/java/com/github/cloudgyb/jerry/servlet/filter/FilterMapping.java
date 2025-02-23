package com.github.cloudgyb.jerry.servlet.filter;


import com.github.cloudgyb.jerry.servlet.mapping.AbstractMapping;
import jakarta.servlet.Filter;

/**
 * @author cloudgyb
 * @since 2025/2/23 15:14
 */
public class FilterMapping extends AbstractMapping implements Comparable<AbstractMapping> {
    final Filter filter;

    public FilterMapping(Filter filter, String urlPattern) {
        super(urlPattern);
        this.filter = filter;
    }

    public Filter getFilter() {
        return filter;
    }
}
