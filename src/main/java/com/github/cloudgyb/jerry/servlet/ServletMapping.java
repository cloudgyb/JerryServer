package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.Servlet;
import java.util.regex.Pattern;

/**
 * @author cloudgyb
 * @since 2025/2/16 19:39
 */
public class ServletMapping {
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

    public static Pattern urlPatternToRegex(String urlPattern) {
        if (urlPattern.startsWith("/") && urlPattern.endsWith("/*")) {
            // 路径匹配：/path/*
            String base = urlPattern.replace("/*", "(/.*)?");
            return Pattern.compile("^" + base + "$");
        } else if (urlPattern.startsWith("*.")) {
            // 扩展名匹配：*.ext
            String ext = urlPattern.substring(2);
            return Pattern.compile("^.*\\." + ext + "$");
        } else if (urlPattern.equals("/")) {
            // 默认Servlet
            return Pattern.compile("^/.*");
        } else {
            // 精确匹配
            return Pattern.compile("^" + urlPattern + "$");
        }
    }
}
