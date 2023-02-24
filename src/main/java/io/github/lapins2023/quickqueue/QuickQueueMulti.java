package io.github.lapins2023.quickqueue;

import java.io.File;
import java.nio.ReadOnlyBufferException;

public class QuickQueueMulti {
    final File dir;
    final String name;
    private final WriterMulti writer;

    public QuickQueueMulti(File dir, String mode, String name) {
        Utils.assertMode(mode);
        this.dir = dir;
        if (name.length() != 3) {
            throw new IllegalArgumentException("nameMushLength=3");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c != (byte) c) throw new IllegalArgumentException("nameMustAscii");
        }
        this.name = name;
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
