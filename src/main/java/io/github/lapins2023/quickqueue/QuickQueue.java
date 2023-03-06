package io.github.lapins2023.quickqueue;


import java.io.File;

public abstract class QuickQueue {
    public abstract QuickQueueWriter getWriter();

    public abstract QuickQueueWriter newMessage();

    public abstract void force();

    public abstract void close();

    public abstract QuickQueueReader createReader();

    public abstract String getPath();

    public static QuickQueue createReadonlySingle(File dir) {
        return createSingle(dir, "r");
    }

    public static QuickQueue createSingle(File dir, String mode) {
        return new QuickQueueSingle(dir, mode);
    }

    public static QuickQueue createMulti(File dir, String mode, String producerName) {
        return new QuickQueueMulti(dir, mode, producerName);
    }

    public static QuickQueue createReadonlyMulti(File dir) {
        return new QuickQueueMulti(dir);
    }
}
