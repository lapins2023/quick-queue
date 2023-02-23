package io.github.lapins2023.quickqueue;

import java.io.File;

public class QuickQueueMulti {
    final File dir;
    final String name;
    private WriterMulti writer;

    public QuickQueueMulti(File dir, String mode, String name) {
        Utils.assertMode(mode);
        this.dir = dir;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c != (byte) c) throw new IllegalArgumentException("nameMustAscii");
            if (i > 3) throw new IllegalArgumentException("nameMaxLengthIs3");
        }
        this.name = name;
        if (mode.equalsIgnoreCase("rw")) {
            this.writer = new WriterMulti(this);
        } else {
            this.writer = null;
        }
    }

}
