package com.github.cloudgyb.jerry.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Web application classloader
 *
 * @author geng
 * @since 2025/03/04 11:19:11
 */
public class WebAppClassLoader extends URLClassLoader {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public WebAppClassLoader(URL[] urls) {
        super(urls);
    }

    public WebAppClassLoader(Path classesPath, Path libPath) {
        super(createUrls(classesPath, libPath));
    }

    private static URL[] createUrls(Path classesPath, Path libPath) {
        if (classesPath == null || !classesPath.toFile().isDirectory()) {
            throw new IllegalArgumentException("classesPath cannot be null and it must be a directory!");
        }
        ArrayList<URL> urls = new ArrayList<>();
        try {
            String absolutePath = classesPath.toAbsolutePath().normalize().toString().replace('\\', '/');
            if (!absolutePath.endsWith("/")) {
                absolutePath += "/";
            }
            urls.add(URI.create("file:///" + absolutePath).toURL());
            if (libPath == null || !libPath.toFile().isDirectory()) {
                return urls.toArray(new URL[0]);
            }
            try (Stream<Path> list = Files.list(libPath)) {
                list.filter(f -> f.getFileName().toString().endsWith(".jar")).sorted().forEach(f1 -> {
                    if (Files.isRegularFile(f1)) {
                        String path = f1.toAbsolutePath().normalize().toString().replace('\\', '/');
                        URL url;
                        try {
                            url = URI.create("file:///" + path).toURL();
                            urls.add(url);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return urls.toArray(new URL[0]);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (logger.isDebugEnabled()) {
            logger.debug("Load class {}", name);
        }
        return super.loadClass(name, resolve);
    }
}
