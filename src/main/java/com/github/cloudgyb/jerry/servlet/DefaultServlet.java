package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author cloudgyb
 * @since 2025/2/10 21:07
 */
public class DefaultServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String resBody404 = "404 NOT FOUND!";
        try {
            resp.sendError(404, resBody404);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
