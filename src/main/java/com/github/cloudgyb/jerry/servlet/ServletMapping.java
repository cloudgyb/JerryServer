package com.github.cloudgyb.jerry.servlet;

import com.github.cloudgyb.jerry.servlet.mapping.AbstractMapping;
import jakarta.servlet.Servlet;

/**
 * @author cloudgyb
 * @since 2025/2/16 19:39
 */
public class ServletMapping extends AbstractMapping implements Comparable<AbstractMapping> {
    final Servlet servlet;
    final ServletRegistrationImpl servletRegistration;

    public ServletMapping(ServletRegistrationImpl servletRegistration, Servlet servlet, String urlPattern) {
        super(urlPattern);
        this.servletRegistration = servletRegistration;
        this.servlet = servlet;
    }

    public String getServletName() {
        return servletRegistration.getName();
    }

}
