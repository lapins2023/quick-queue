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

    private final AtomicBoolean startedCleaner = new AtomicBoolean(false);
    private Cleaner cleaner;

    public QuickQueueReader createReader() {
        if (startedCleaner.compareAndSet(false, true)) {
            cleaner = new Cleaner();
        }
        return new QuickQueueReaderMulti(this);
    }

    @Override
    public String getPath() {
        return dir.getAbsolutePath();
    }

    class Cleaner {
        private final AtomicBoolean started = new AtomicBoolean(true);

        public Cleaner() {
            Thread thread = new Thread(() -> {
                while (started.get()) {
                    int time = (int) (System.currentTimeMillis() >> 10);
                    for (Map.Entry<Integer, QuickQueueReaderMulti.Data> entry
                            : QuickQueueReaderMulti.DATA.entrySet()) {
                        QuickQueueReaderMulti.Data data = entry.getValue();
                        if (time - data.lastHit > TimeUnit.MINUTES.toMillis(10)) {
                            QuickQueueReaderMulti.DATA.remove(data.id);
                        }
                    }
                    try {
                        //noinspection BusyWait
                        Thread.sleep(2_000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            thread.setName("QQMCn-" + QuickQueueMulti.this.hashCode());
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void close() {
        if (this.writer != null) {
            this.writer.close();
        }
        if (cleaner != null) {
            cleaner.started.set(false);
            cleaner = null;
        }
        startedCleaner.set(false);
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
