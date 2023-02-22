package io.github.lapins2023.quickqueue;


import java.io.File;

public class QuickQueue {
    final File dataDir;
    private QuickQueueWriter writer;


    public QuickQueue(String dataDir) {
        this(new File(dataDir));
    }

    public QuickQueue(File dataDir) {
        this.dataDir = dataDir;
        open();
    }

    private void open() {
        if (dataDir.exists()) {
            if (dataDir.isFile()) {
                throw new IllegalArgumentException("File " + dataDir + " exists and is not a directory. Unable to create directory.");
            }
        } else {
            if (!dataDir.mkdirs()) {
                throw new IllegalArgumentException("Unable to create directory " + dataDir);
            }
        }
    }

    public QuickQueueWriter openWrite() {
        if (this.writer != null) {
            throw new IllegalArgumentException("QuickQueueWriteIsOpened");
        }
        this.writer = new QuickQueueWriter(this);
        return this.writer;
    }

    public QuickQueueWriter getWriter() {
        return writer;
    }

    public QuickQueueWriter newMessage() {
        return writer.newMessage();
    }

    public QuickQueueReader createReader() {
        return new QuickQueueReader(this);
    }

    public void force() {
        writer.force();
    }

    public void close() {
        QuickQueueWriter writer_ = this.writer;
        this.writer = null;
        if (writer_ != null) {
            writer_.force();
            writer_.close();
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
