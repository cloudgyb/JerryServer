package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.Servlet;

import java.util.regex.Pattern;

/**
 * @author cloudgyb
 * @since 2025/2/16 19:39
 */
public class ServletMapping {
    private final Servlet servlet;
    private final String urlPattern;
    private final Pattern pattern;

    public ServletMapping(Servlet servlet, String urlPattern) {
        this.servlet = servlet;
        this.urlPattern = urlPattern;
        pattern = Pattern.compile(convertAntPathToRegex(urlPattern));
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

    public static String convertAntPathToRegex(String antPath) {
        StringBuilder regex = new StringBuilder();
        regex.append("^"); // Start of string anchor

        int length = antPath.length();
        for (int i = 0; i < length; i++) {
            char c = antPath.charAt(i);

            switch (c) {
                case '*':
                    // Check for '**'
                    if (i < length - 1 && antPath.charAt(i + 1) == '*') {
                        regex.append(".*");
                        i++; // Skip the next '*'
                    } else {
                        regex.append("[^/]*");
                    }
                    break;
                case '?':
                    regex.append("[^/]");
                    break;
                case '.':
                case '$':
                case '^':
                case '+':
                case '|':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '\\':
                    // Escape special regex characters
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }

        regex.append("$"); // End of string anchor
        return regex.toString();
    }

    public static void main(String[] args) {
        // Test cases
        System.out.println(convertAntPathToRegex("/foo/*"));    // ^/foo/[^/]*$
        System.out.println(convertAntPathToRegex("/foo/**"));   // ^/foo/.*$
        System.out.println(convertAntPathToRegex("/foo/?"));    // ^/foo/[^/]$
        System.out.println(convertAntPathToRegex("/foo/*/bar")); // ^/foo/[^/]*/bar$
        System.out.println(convertAntPathToRegex("/foo/."));    // ^/foo/\.$
        System.out.println(convertAntPathToRegex("*.action"));

    }
}
