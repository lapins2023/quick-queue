package io.github.lapins2023.quickqueue;


import java.io.File;

public class QuickQueue {
    final File dataDir;
    private final QuickQueueWriter writer;


    public QuickQueue(File dataDir, String mode) {
        if (!(mode.equalsIgnoreCase("r") || mode.equalsIgnoreCase("rw")))
            throw new IllegalArgumentException("mode must r,rw");
        this.dataDir = dataDir;
        if (dataDir.exists()) {
            if (dataDir.isFile()) throw new IllegalArgumentException("NotDirFileExists=" + dataDir);
        } else {
            if (!dataDir.mkdirs()) throw new IllegalArgumentException("UnableMkdir=" + dataDir);
        }
        if (mode.equalsIgnoreCase("rw")) {
            this.writer = new QuickQueueWriter(this);
        } else {
            this.writer = null;
        }
    }

    public QuickQueueWriter newMessage() {
        try {
            return writer.newMessage();
        } catch (NullPointerException e) {
            throw new UnsupportedOperationException("ReadonlyQuickQueue");
        }
    }

    public QuickQueueReader createReader() {
        return new QuickQueueReader(this);
    }

    public void force() {
        writer.force();
    }

    public void clean() {
        if (this.writer != null) {
            this.writer.clean();
        }
    }


    //////////////////
    //////////////////
    //////////////////
    //////////////////
    public String getPath() {
        return dataDir.getAbsolutePath();
    }
}
