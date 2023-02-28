package io.github.lapins2023.quickqueue;

import java.io.File;
import java.nio.ReadOnlyBufferException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class QuickQueueMulti {
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

        Thread thread = new Thread(() -> {
            int time = (int) (System.currentTimeMillis() >> 10);
            for (Map.Entry<Integer, QuickQueueReaderMulti.Data> entry
                    : QuickQueueReaderMulti.DATA.entrySet()) {
                QuickQueueReaderMulti.Data data = entry.getValue();
                if (time - data.lastHit > TimeUnit.MINUTES.toMillis(10)) {
                    QuickQueueReaderMulti.DATA.remove(data.id);
                }
            }
        });
        thread.setName("QQMCn-" + thread.getId());
        thread.setDaemon(true);
        thread.start();
    }

    public QuickQueueWriter newMessage() {
        try {
            return writer.newMessage();
        } catch (NullPointerException e) {
            throw new ReadOnlyBufferException();
        }
    }

    public QuickQueueReaderMulti createReader() {
        return new QuickQueueReaderMulti(this);
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
