package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author cloudgyb
 * @since 2025/2/10 21:07
 */
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String name = req.getParameter("name");
        if (name == null) {
            name = "world";
        }
        String resBody = String.format("Hello,%s!", name);
        HttpSession session = req.getSession();
        session.setAttribute("test", "123");
        try {
            resp.setContentType("text/html");
            resp.setContentLength(resBody.getBytes(StandardCharsets.UTF_8).length);
            resp.getWriter().println(resBody);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
