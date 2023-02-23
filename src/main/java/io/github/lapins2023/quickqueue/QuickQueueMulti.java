package io.github.lapins2023.quickqueue;

import java.io.File;

public class QuickQueueMulti {
    final File dataDir;
    private QuickQueueWriterMulti writer;

    public QuickQueueMulti(File dataDir) {
        this.dataDir = dataDir;
    }

    public QuickQueueWriterMulti openWrite() {
        if (this.writer != null) {
            throw new IllegalArgumentException("QuickQueueWriteIsOpened");
        }
        this.writer = new QuickQueueWriterMulti(this);
        return this.writer;
    }
}
