package io.github.lapins2023.quickqueue;


import java.io.File;
import java.nio.ReadOnlyBufferException;

public class QuickQueueSingle extends QuickQueue{
    final File dir;
    private final WriterSingle writer;

    public QuickQueueSingle(File dir, String mode) {
        Utils.assertMode(mode);
        this.dir = Utils.mkdir(dir);
        if (mode.equalsIgnoreCase("rw")) {
            this.writer = new WriterSingle(this);
        } else {
            this.writer = null;
        }
    }

    public QuickQueueWriter getWriter() {
        return writer;
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


    public void close() {
        if (this.writer != null) {
            this.writer.clean();
        }
    }

    public QuickQueueReaderSingle createReader() {
        return new QuickQueueReaderSingle(this);
    }

    //////////////////
    //////////////////
    //////////////////
    //////////////////
    public String getPath() {
        return dir.getAbsolutePath();
    }

    @Override
    public String toString() {
        return "QuickQueue{" +
                "dir=" + dir +
                ", writer=" + writer +
                '}';
    }
}
