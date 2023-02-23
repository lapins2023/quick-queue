package io.github.lapins2023.quickqueue;


import java.io.File;
import java.nio.ReadOnlyBufferException;

public class QuickQueue {
    final File dir;
    private final WriterSingle writer;

    public QuickQueue(File dir, String mode) {
        Utils.assertMode(mode);
        this.dir = dir;
        if (dir.exists()) {
            if (dir.isFile()) throw new IllegalArgumentException("NotDirFileExists=" + dir);
        } else {
            if (!dir.mkdirs()) throw new IllegalArgumentException("UnableMkdir=" + dir);
        }
        if (mode.equalsIgnoreCase("rw")) {
            this.writer = new WriterSingle(this);
        } else {
            this.writer = null;
        }
    }

    public QuickQueueWriter newMessage() {
        try {
            return writer.newMessage();
        } catch (NullPointerException e) {
            throw new ReadOnlyBufferException();
        }
    }


    public void force() {
        try {
            writer.force();
        } catch (NullPointerException e) {
            throw new ReadOnlyBufferException();
        }
    }


    public void clean() {
        if (this.writer != null) {
            this.writer.clean();
        }
    }

    public QuickQueueReader createReader() {
        return new QuickQueueReader(this);
    }

    //////////////////
    //////////////////
    //////////////////
    //////////////////
    public String getPath() {
        return dir.getAbsolutePath();
    }
}
