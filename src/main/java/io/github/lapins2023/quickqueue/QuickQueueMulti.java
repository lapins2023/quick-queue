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
    private final ConcurrentHashMap<Integer, QuickQueueReaderMulti> reads = new ConcurrentHashMap<>();

    public QuickQueueMulti(File dir, String mode, String name) {
        Utils.assertMode(mode);
        this.dir = dir;
        this.mpn = Utils.toMPN(name);
        if (mode.equalsIgnoreCase("rw")) {
            this.writer = new WriterMulti(this);
        } else {
            this.writer = null;
        }

        Thread thread = new Thread(() -> {
            int time = (int) (System.currentTimeMillis() >> 10);
            for (Map.Entry<Integer, QuickQueueReaderMulti> entry : reads.entrySet()) {
                Map<Integer, QuickQueueReaderMulti.Data> data = entry.getValue().data;
                for (Map.Entry<Integer, QuickQueueReaderMulti.Data> e : data.entrySet()) {
                    if (time - e.getValue().lastHit > TimeUnit.MINUTES.toMillis(5)) {
                        QuickQueueReaderMulti.Data remove = data.remove(e.getKey());
                        remove.buffer.clean();
                    }
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
        QuickQueueReaderMulti quickQueueMessages = new QuickQueueReaderMulti(this);
        reads.put(quickQueueMessages.hashCode(), quickQueueMessages);
        return quickQueueMessages;
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
