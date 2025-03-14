package com.github.cloudgyb.jerry.servlet;

import com.sun.net.httpserver.HttpExchange;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author cloudgyb
 * @since 2025/2/15 22:24
 */
public class ServletOutputStreamImpl extends ServletOutputStream {
    private WriteListener writeListener;
    private final HttpExchange exchange;
    private final HttpServletResponseImpl response;
    private final OutputStream outputStream;

    public ServletOutputStreamImpl(HttpExchange exchange, HttpServletResponseImpl response) {
        this.exchange = exchange;
        this.outputStream = exchange.getResponseBody();
        this.response = response;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        try {
            this.writeListener = writeListener;
            writeListener.onWritePossible();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (!response.isCommitted()) {
            if (response.getContentLength() == -1) {
                response.setContentLength(0);
            }
            response.commit();
        }
        try {
            outputStream.write(b);
        } catch (IOException e) {
            if (writeListener != null) {
                writeListener.onError(e);
            }
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
        exchange.close();
    }
}
