package io.github.lapins2023.quickqueue;

import java.io.File;
import java.nio.ReadOnlyBufferException;

public class QuickQueueMulti {
    final File dir;
    private final WriterMulti writer;
    final int mpn;

    public QuickQueueMulti(File dir, String mode, String name) {
        Utils.assertMode(mode);
        this.dir = dir;
        this.mpn = Utils.toMPN(name);
        if (mode.equalsIgnoreCase("rw")) {
            this.writer = new WriterMulti(this);
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
}
