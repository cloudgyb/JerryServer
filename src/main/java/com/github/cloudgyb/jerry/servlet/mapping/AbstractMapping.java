package com.github.cloudgyb.jerry.servlet.mapping;

import java.util.regex.Pattern;

/**
 * @author cloudgyb
 * @since 2025/2/23 15:16
 */
public abstract class AbstractMapping implements Comparable<AbstractMapping> {
    protected int priority = 0;
    protected final String urlPattern;
    protected final Pattern pattern;

    public AbstractMapping(String urlPattern) {
        this.urlPattern = urlPattern;
        pattern = urlPatternToRegex(urlPattern);
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

    public boolean match(String path) {
        return pattern.matcher(path).matches();
    }


    @Override
    public int compareTo(AbstractMapping o) {
        int compare = Integer.compare(this.priority, o.priority);
        if (compare == 0) {
            return Integer.compare(o.urlPattern.length(), urlPattern.length());
        }
        return compare;
    }

}
