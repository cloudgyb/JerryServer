package com.github.cloudgyb.jerry.servlet;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author geng
 * @since 2025/02/12 15:41:36
 */
public class ServletInputStreamImpl extends ServletInputStream {
    private final InputStream in;
    private boolean isFinished;
    private ReadListener readListener;

    public ServletInputStreamImpl(InputStream in) {
        this.in = in;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        this.readListener = readListener;
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }
}
