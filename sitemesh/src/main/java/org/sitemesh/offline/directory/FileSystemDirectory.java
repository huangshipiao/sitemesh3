package org.sitemesh.offline.directory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * Disk backed {@link Directory} implementation that uses java.io.File.
 *
 * @author Joe Walnes
 * @see Directory
 */
public class FileSystemDirectory implements Directory {

    private final File rootDir;
    private final Charset encoding;

    public FileSystemDirectory(File rootDir) {
        this(rootDir, Charset.defaultCharset());
    }

    public FileSystemDirectory(File rootDir, Charset encoding) {
        this.rootDir = rootDir;
        this.encoding = encoding;
    }

    public CharBuffer load(String path) throws IOException {
        File file = getFileByPath(path);
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
        try {
            CharBuffer contents = CharBuffer.allocate((int) file.length());
            reader.read(contents);
            contents.flip();
            return contents;
        } finally {
            reader.close();
        }
    }

    public void save(String path, CharBuffer contents) throws IOException {
        File file = getFileByPath(path);

        // If file already contains contents, don't bother saving.
        // This is to play nicely with incremental builds.
        if (file.exists()) {
            CharBuffer oldContents = load(path);
            if (oldContents.equals(contents)) {
                return;
            }
        }

        file.getParentFile().mkdirs();
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
        try {
            out.append(contents);
        } finally {
            out.close();
        }
    }

    public List<String> listAllFilePaths() throws IOException {
        List<String> result = new LinkedList<String>();
        listAllFilePaths("", result);
        return result;
    }

    private void listAllFilePaths(String path, List<String> result) {
        File file = getFileByPath(path);
        if (file.isDirectory()) {
            for (String child : file.list()) {
                String childPath = (path.length() == 0)
                        ? child : path + "/" + child;
                listAllFilePaths(childPath, result);
            }
        }
        if (file.isFile() && file.canRead()) {
            result.add(path);
        }
    }

    public void load(String path, WritableByteChannel channelToWriteTo) throws IOException {
        File file = getFileByPath(path);
        FileChannel channel = new FileInputStream(file).getChannel();
        try {
            channel.transferTo(0, Long.MAX_VALUE, channelToWriteTo);
        } finally {
            channel.close();
        }
    }

    public void save(String path, ReadableByteChannel channelToReadFrom, int length) throws IOException {
        File file = getFileByPath(path);
        file.getParentFile().mkdirs();
        FileChannel destinationChannel = new FileOutputStream(file).getChannel();
        try {
            destinationChannel.transferFrom(channelToReadFrom, 0, length);
        } finally {
            destinationChannel.close();
        }
    }

    public void copy(String path, Directory destinationDirectory, String destionationPath) throws IOException {
        FileChannel sourceChannel = new FileInputStream(getFileByPath(path)).getChannel();
        try {
            destinationDirectory.save(destionationPath, sourceChannel, (int)sourceChannel.size());
        } finally {
            sourceChannel.close();
        }
    }

    public File getFileByPath(String path) {
        return new File(rootDir, path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileSystemDirectory that = (FileSystemDirectory) o;

        if (encoding != null ? !encoding.equals(that.encoding) : that.encoding != null) return false;
        if (rootDir != null ? !rootDir.equals(that.rootDir) : that.rootDir != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = rootDir != null ? rootDir.hashCode() : 0;
        result = 31 * result + (encoding != null ? encoding.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("FileSystemDirectory{rootDir=").append(rootDir)
                .append(", encoding=").append(encoding).append('}').toString();
    }
}
