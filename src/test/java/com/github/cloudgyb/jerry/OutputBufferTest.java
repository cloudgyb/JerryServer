package com.github.cloudgyb.jerry;

import com.github.cloudgyb.jerry.servlet.buffer.OutputBuffer;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author cloudgyb
 * @since 2025/3/24 21:44
 */
public class OutputBufferTest extends TestCase {

    public void testOutputBuffer() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputBuffer outputBuffer = new OutputBuffer(1024, byteArrayOutputStream);
        outputBuffer.setBufferFlushLister(() -> {
            System.out.println("buffer full");
        });

        for (int i = 0; i < 1024; i++) {
            outputBuffer.write(i);
        }
        outputBuffer.flush();
        System.out.println(byteArrayOutputStream.toString());
        outputBuffer.close();
    }
}
