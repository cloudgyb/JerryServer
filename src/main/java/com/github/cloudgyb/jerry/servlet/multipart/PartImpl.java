package com.github.cloudgyb.jerry.servlet.multipart;

import jakarta.servlet.http.Part;
import org.apache.commons.fileupload2.core.DiskFileItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author geng
 * @since 2025/02/24 14:46:03
 */
public class PartImpl implements Part {
    private final DiskFileItem diskFileItem;

    public PartImpl(DiskFileItem diskFileItem) {
        this.diskFileItem = diskFileItem;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return diskFileItem.getInputStream();
    }

    @Override
    public String getContentType() {
        return diskFileItem.getContentType();
    }

    @Override
    public String getName() {
        return diskFileItem.getName();
    }

    @Override
    public String getSubmittedFileName() {
        return diskFileItem.getFieldName();
    }

    @Override
    public long getSize() {
        return diskFileItem.getSize();
    }

    @Override
    public void write(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        diskFileItem.write(path);
    }

    @Override
    public void delete() throws IOException {
        diskFileItem.delete();
    }

    @Override
    public String getHeader(String name) {
        return diskFileItem.getHeaders().getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        ArrayList<String> arrayList = new ArrayList<>();
        diskFileItem.getHeaders().getHeaders(name)
                .forEachRemaining(arrayList::add);
        return arrayList;
    }

    @Override
    public Collection<String> getHeaderNames() {
        ArrayList<String> arrayList = new ArrayList<>();
        diskFileItem.getHeaders().getHeaderNames().forEachRemaining(arrayList::add);
        return arrayList;
    }
}
