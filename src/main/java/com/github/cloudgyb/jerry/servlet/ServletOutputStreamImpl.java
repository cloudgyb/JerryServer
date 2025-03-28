package com.github.cloudgyb.jerry.servlet;

import com.github.cloudgyb.jerry.servlet.buffer.OutputBuffer;
import com.sun.net.httpserver.HttpExchange;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A standard ServletOutputStream implementation.
 *
 * @author cloudgyb
 * @since 2025/2/15 22:24
 */
public class ServletOutputStreamImpl extends ServletOutputStream {
    private WriteListener writeListener;
    private final OutputBuffer outputBuffer;
    private boolean isClosed = false;

    public ServletOutputStreamImpl(OutputBuffer outputBuffer) {
        this.outputBuffer = outputBuffer;
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
        if (isClosed) {
            throw new IOException("ServletOutputStream is closed");
        }
        try {
            outputBuffer.write(b);
        } catch (IOException e) {
            if (writeListener != null) {
                writeListener.onError(e);
            }
            throw e;
        }
    }

    @Override
    public void flush() throws IOException {
        if (isClosed) {
            throw new IOException("ServletOutputStream is closed");
        }
        outputBuffer.flush();
    }

    @Override
    public void close() {
        isClosed = true;
    }
}
