package com.github.cloudgyb.jerry.servlet;

import com.github.cloudgyb.jerry.loader.WebAppClassLoader;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

/**
 * ServletContext factory for creating ServletContext instance.
 *
 * @author geng
 * @since 2025/03/10 16:08:07
 */
public class ServletContextFactory {
    private final static Logger logger = LoggerFactory.getLogger(ServletContextFactory.class);

    /**
     * Create a specific contextPath ServletContext instance.
     * <p>
     * Scans and registers all servlet components(eg:Servlet、Filter、Listener...)
     * annotated with Servlet annotation (eg: @WebServlet,@WebFilter and @WebListener)
     * located in classesPath and libPath to a ServletContext instance.
     * </p>
     *
     * @param contextPath context path
     * @param classesPath classes path of the war file
     * @param libPath     lib path of the war file
     * @return ServletContext that has already been initialized.
     */
    public static ServletContextImpl create(String contextPath, Path classesPath, Path libPath) {
        if (contextPath == null) {
            throw new IllegalArgumentException("contextPath cannot be null!");
        }
        WebAppClassLoader webAppClassLoader = new WebAppClassLoader(classesPath, libPath);
        ServletContextImpl servletContext = new ServletContextImpl(contextPath, webAppClassLoader);
        scanAndRegisterServletComponents(servletContext, classesPath, libPath);
        try {
            servletContext.init();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return servletContext;
    }

    private static void scanAndRegisterServletComponents(ServletContextImpl servletContext,
                                                         Path classesPath, Path libPath) {

    }

    /**
     * Create a ServletContext by war file.
     * <p>
     * The method decompresses war file to the parent directory of the war file, then scans and registers
     * all servlet components (eg:Servlet、Filter、Listener...) to a ServletContext instance.
     * </p>
     *
     * @param warPath a war file
     * @return ServletContext that has already been initialized.
     */
    public static ServletContextImpl create(Path warPath) {
        if (warPath == null) {
            throw new IllegalArgumentException("warPath cannot be null!");
        }
        if (!Files.exists(warPath)) {
            throw new IllegalArgumentException("War file not found: " + warPath);
        }
        Path warFileName = warPath.getFileName();
        Path unzipTargetPath = warPath.getParent().resolve(warFileName);
        try {
            extractWarFile(warPath, unzipTargetPath);
        } catch (IOException e) {
            logger.error("Failed to extract war file: ", e);
        }
        String WEB_INF_CLASSES = "WEB-INF/classes/";
        String WEB_INF_LIB = "WEB-INF/lib/";
        Path classesPath = unzipTargetPath.resolve(WEB_INF_CLASSES);
        Path libPath = unzipTargetPath.resolve(WEB_INF_LIB);
        String contextPath = warFileName.toString();
        if (contextPath.equals("ROOT")) {
            contextPath = "/";
        }
        return create(contextPath, classesPath, libPath);
    }

    private static void extractWarFile(Path warPath, Path unzipTargetPath) throws IOException {
        if (!Files.exists(unzipTargetPath)) {
            Files.createDirectories(unzipTargetPath);
        }
        try (JarFile jarFile = new JarFile(warPath.toFile())) {
            jarFile.stream().forEach(f -> {
                try {
                    if (f.getName().endsWith(".class")) {
                        InputStream inputStream = jarFile.getInputStream(f);
                        Files.copy(inputStream, unzipTargetPath.resolve(f.getName()));
                    } else if (f.isDirectory()) {
                        Files.createDirectories(unzipTargetPath.resolve(f.getName()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
