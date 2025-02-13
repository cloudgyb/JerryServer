package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.http.Cookie;

import java.util.Map;
import java.util.Set;

/**
 * @author cloudgyb
 * @since 2025/2/12 22:03
 */
public class CookieEncoder {

    public static String encode(Cookie cookie) {
        StringBuilder sb = new StringBuilder();
        sb.append(cookie.getName()).append("=").append(cookie.getValue());
        Map<String, String> attributes = cookie.getAttributes();
        Set<String> keySet = attributes.keySet();
        for (String k : keySet) {
            sb.append("; ");
            if (k.equals("Secure") || k.equals("HttpOnly")) {
                sb.append(k);
            } else {
                sb.append(k).append("=").append(attributes.get(k));
            }
        }
        return sb.toString();
    }
}
