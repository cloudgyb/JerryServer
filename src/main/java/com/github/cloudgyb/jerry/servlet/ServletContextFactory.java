package com.github.cloudgyb.jerry.servlet;

import com.github.cloudgyb.jerry.loader.WebAppClassLoader;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.jar.JarFile;
import java.util.stream.Stream;

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
    public static ServletContextImpl create(String contextPath, Path classesPath, Path libPath) throws ServletException {
        if (contextPath == null) {
            throw new IllegalArgumentException("contextPath cannot be null!");
        }
        if (classesPath == null) {
            throw new IllegalArgumentException("classesPath cannot be null!");
        }
        // libPath can be null
        /*if (libPath == null) {
            throw new IllegalArgumentException("libPath cannot be null!");
        }*/
        WebAppClassLoader webAppClassLoader = new WebAppClassLoader(classesPath, libPath);
        ServletContextImpl servletContext = new ServletContextImpl(contextPath, webAppClassLoader);
        scanAndRegisterServletComponents(servletContext, classesPath, libPath);

        try {
            servletContext.init();
        } catch (ServletException e) {
            try {
                webAppClassLoader.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            logger.error("Failed to initialize ServletContext: ", e);
            throw e;
        }
        return servletContext;
    }

    @SuppressWarnings("unchecked")
    private static void scanAndRegisterServletComponents(ServletContextImpl servletContext,
                                                         Path classesPath, Path libPath) {
        scanClasspath(servletContext, classesPath);
        boolean libPathExists = Files.exists(libPath);
        if (!libPathExists) {
            logger.warn("libPath not exists: {}, skip to scan!", libPath);
            return;
        }
        try (Stream<Path> list = Files.list(libPath)) {
            list.forEach(f -> {
                if (!f.endsWith(".jar")) {
                    return;
                }
                try (JarFile jarFile = new JarFile(f.toFile())) {
                    jarFile.stream().forEach(jarEntry -> {
                        if (!jarEntry.getName().endsWith(".class")) {
                            return;
                        }
                        String className = jarEntry.getName().replace('/', '.');
                        ClassLoader classLoader = servletContext.getClassLoader();
                        try {
                            Class<?> aClass = classLoader.loadClass(className);
                            if (Servlet.class.isAssignableFrom(aClass) && aClass.isAnnotationPresent(WebServlet.class)) {
                                logger.debug("Found Servlet from jar:{}", aClass.getName());
                                servletContext.addServlet(aClass.getSimpleName(), (Class<Servlet>) aClass);
                            } else if (Filter.class.isAssignableFrom(aClass) && aClass.isAnnotationPresent(WebFilter.class)) {
                                logger.debug("Found Filter from jar:{}", aClass.getName());
                                servletContext.addFilter(aClass.getSimpleName(), (Class<Filter>) aClass);
                            }
                        } catch (ClassNotFoundException e) {
                            logger.warn("Failed to load class: {}", className, e);
                        }
                    });
                } catch (IOException e) {
                    logger.warn("Failed to load jar file: {}, skipped!", f.getFileName(), e);
                }
            });
        } catch (IOException e) {
            logger.warn("Failed to list jar files in lib path: {}", libPath, e);
        }
    }

    private static void scanClasspath(ServletContextImpl servletContext, Path klass) {
        if (!Files.isDirectory(klass)) {
            String fileName = klass.toAbsolutePath().normalize().toString().replace('\\','/');
            if (fileName.endsWith(".class")) {
                int i = fileName.indexOf("WEB-INF/classes/");
                String replace = fileName.substring(i + "WEB-INF/classes/".length()).replace(".class", "");
                String className = replace.replace('/', '.');
                ClassLoader classLoader = servletContext.getClassLoader();
                Class<?> aClass;
                try {
                    aClass = classLoader.loadClass(className);
                    if (Servlet.class.isAssignableFrom(aClass) && aClass.isAnnotationPresent(WebServlet.class)) {
                        logger.debug("Found Servlet from classpath:{}", aClass.getName());
                        servletContext.addServlet(aClass.getSimpleName(), (Class<Servlet>) aClass);
                    }
                    if (Filter.class.isAssignableFrom(aClass) && aClass.isAnnotationPresent(WebFilter.class)) {
                        logger.debug("Found Filter from classpath:{}", aClass.getName());
                        servletContext.addFilter(aClass.getSimpleName(), (Class<Filter>) aClass);
                    }
                } catch (ClassNotFoundException e) {
                    logger.warn("Failed to load class: {}", className, e);
                }
            }
            return;
        }
        try (Stream<Path> list = Files.list(klass)) {
            list.forEach(f -> {
                scanClasspath(servletContext, f);
            });
        } catch (IOException e) {
            logger.warn("Failed to load classes from classpath: {}", klass, e);
        }
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
    public static ServletContextImpl create(Path warPath) throws ServletException {
        if (warPath == null) {
            throw new IllegalArgumentException("warPath cannot be null!");
        }
        if (!Files.exists(warPath)) {
            throw new IllegalArgumentException("War file not found: " + warPath);
        }
        String dirName = warPath.toFile().getName().replaceAll("\\.war$", "");
        Path unzipTargetPath = Paths.get(warPath.getParent().toAbsolutePath().normalize().toString(), dirName);
        try {
            extractWarFile(warPath, unzipTargetPath);
        } catch (IOException e) {
            logger.error("Failed to extract war file: ", e);
        }
        String WEB_INF_CLASSES = "WEB-INF/classes/";
        String WEB_INF_LIB = "WEB-INF/lib/";
        Path classesPath = unzipTargetPath.resolve(WEB_INF_CLASSES);
        Path libPath = unzipTargetPath.resolve(WEB_INF_LIB);
        String contextPath = dirName;
        if (contextPath.equals("ROOT")) {
            contextPath = "/";
        } else {
            contextPath = "/" + contextPath;
        }
        return create(contextPath, classesPath, libPath);
    }

    private static void extractWarFile(Path warPath, Path unzipTargetPath) throws IOException {
        if (!Files.exists(unzipTargetPath)) {
            Files.createDirectories(unzipTargetPath);
        }
        try (JarFile jarFile = new JarFile(warPath.toFile())) {
            jarFile.stream().forEach(jarEntry -> {
                try {
                    if (jarEntry.isDirectory()) {
                        Files.createDirectories(unzipTargetPath.resolve(jarEntry.getName()));
                        return;
                    }
                    Path parentPath = unzipTargetPath.resolve(jarEntry.getName()).getParent();
                    if (!Files.isDirectory(parentPath)) {
                        Files.createDirectories(parentPath);
                    }
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    Files.copy(inputStream, unzipTargetPath.resolve(jarEntry.getName()),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
