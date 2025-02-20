package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.Servlet;

import java.util.regex.Pattern;

/**
 * @author cloudgyb
 * @since 2025/2/16 19:39
 */
public class ServletMapping implements Comparable<ServletMapping> {
    private int priority = 0;
    final Servlet servlet;
    private final String urlPattern;
    private final Pattern pattern;

    public ServletMapping(Servlet servlet, String urlPattern) {
        this.servlet = servlet;
        this.urlPattern = urlPattern;
        pattern = urlPatternToRegex(urlPattern);
    }

    public boolean match(String path) {
        return pattern.matcher(path).matches();
    }

    public String getServletName() {
        return servlet.getServletConfig().getServletName();
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Pattern urlPatternToRegex(String urlPattern) {
        if (urlPattern.startsWith("/") && urlPattern.endsWith("/*")) {
            // 路径匹配：/path/*
            priority = 0;
            String base = urlPattern.replace("/*", "(/.*)?");
            return Pattern.compile("^" + base + "$");
        } else if (urlPattern.startsWith("*.")) {
            // 扩展名匹配：*.ext
            priority = 1;
            String ext = urlPattern.substring(2);
            return Pattern.compile("^.*\\." + ext + "$");
        } else if (urlPattern.equals("/")) {
            // 默认Servlet
            priority = Integer.MAX_VALUE;
            return Pattern.compile("^/.*");
        } else {
            // 精确匹配
            priority = Integer.MIN_VALUE;
            return Pattern.compile("^" + urlPattern + "$");
        }
    }

    @Override
    public int compareTo(ServletMapping o) {
        int compare = Integer.compare(this.priority, o.priority);
        if (compare == 0) {
            return Integer.compare(o.urlPattern.length(), urlPattern.length());
        }
        return compare;
    }
}
