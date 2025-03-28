package com.github.cloudgyb.jerry.servlet.buffer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author cloudgyb
 * @since 2025/3/24 20:35
 */
public class OutputBuffer extends OutputStream {
    private volatile byte[] buffer;
    private int count;
    private int size;
    private final OutputStream outputStream;
    private boolean isCosed;
    private BufferFlushLister bufferFlushLister;

    public OutputBuffer(int size, OutputStream outputStream) {
        if (size <= 0) {
            throw new IllegalArgumentException("size < 0");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream cannot be null!");
        }
        this.outputStream = outputStream;
        this.size = size;
    }

    public void setSize(int size) {
        if (isCosed) {
            throw new IllegalStateException("OutputBuffer has been closed!");
        }
        if (buffer != null) {
            throw new IllegalStateException("This OutputBuffer has been written," +
                    "and its size cannot be modified!");
        }
        this.size = size;
    }

    /**
     * Writes the specified <code>byte</code> to this buffer.
     *
     * @param b the <code>byte</code>.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(int b) throws IOException {
        if (buffer == null) {
            synchronized (this) {
                if (buffer == null) {
                    buffer = new byte[size];
                }
            }
        }
        buffer[count++] = (byte) b;
        if (count == size) {
            flush();
        }
    }

    public int getCount() {
        return count;
    }

    public void clear() {
        count = 0;
    }

    @Override
    public void flush() throws IOException {
        if (isCosed) {
            throw new IOException("OutputBuffer has been closed!");
        }
        if (bufferFlushLister != null) {
            bufferFlushLister.onBufferFlush();
        }
        if (count > 0) {
            outputStream.write(buffer, 0, count);
            outputStream.flush();
        }
        count = 0;
    }

    @Override
    public void close() throws IOException {
        if (isCosed) {
            return;
        }
        flush();
        isCosed = true;
        buffer = null;
        outputStream.close();
    }

    public void setBufferFlushLister(BufferFlushLister bufferFlushLister) {
        this.bufferFlushLister = bufferFlushLister;
    }

    @FunctionalInterface
    public interface BufferFlushLister {
        void onBufferFlush();
    }
}
