package io.github.lapins2023.quickqueue;

import java.io.File;
import java.nio.ReadOnlyBufferException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuickQueueMulti extends QuickQueue {
    final File dir;
    private final WriterMulti writer;
    final int mpn;

    public QuickQueueMulti(File dir) {
        this(dir, "r", null);
    }

    public QuickQueueMulti(File dir, String mode, String producerName) {
        Utils.assertMode(mode);
        this.dir = dir;
        if (mode.equalsIgnoreCase("rw")) {
            this.mpn = Utils.toMPN(producerName);
            this.writer = new WriterMulti(this);
        } else {
            this.writer = null;
            this.mpn = 0;
        }


    }

    @Override
    public QuickQueueWriter getWriter() {
        return null;
    }

    public QuickQueueWriter newMessage() {
        try {
            return writer.newMessage();
        } catch (NullPointerException e) {
            throw new ReadOnlyBufferException();
        }
    }

    @Override
    public void force() {
        if (writer != null) {
            writer.force();
        }
    }


    public QuickQueueReader createReader() {
        return new QuickQueueReaderMulti(this);
    }

    @Override
    public String getPath() {
        return dir.getAbsolutePath();
    }

    public void close() {
        if (this.writer != null) {
            this.writer.close();
        }
    }

    @Override
    public String toString() {
        return "QuickQueueMulti{" +
                "dir=" + dir +
                ", name=" + Utils.fromMPN(mpn) +
                ", writer=" + writer +
                '}';
    }
}
