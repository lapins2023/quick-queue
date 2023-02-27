package io.github.lapins2023.quickqueue;

import java.io.File;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuickQueueReaderMulti implements AutoCloseable, Iterable<QuickQueueMessage> {


    private final BigBuffer index;
    private final QuickQueueMulti qkq;
    final Map<Integer, Data> data = new ConcurrentHashMap<>();

    public class Data {
        final BigBuffer buffer;
        private final int mpn;
        final QuickQueueMessage message;
        final FileChannel mpoC;
        int lastHit = (int) (System.currentTimeMillis() >> 10);

        public Data(int mpn) throws NotActiveException {
            String name = Utils.fromMPN(mpn);
            File dir = new File(qkq.dir, name);
            if (!dir.isDirectory()) {
                throw new NotActiveException("WriterNotFound=" + name);
            }
            buffer = new BigBuffer("r", Utils.PAGE_SIZE
                    , Utils.mkdir(dir)
                    , "", Utils.EXT_DATA);
            this.mpn = mpn;
            message = new QuickQueueMessage(buffer);
            try (RandomAccessFile rw = new RandomAccessFile(
                    new File(qkq.dir, Utils.fromMPN(mpn) + Utils.EXT_MP), "r")) {
                this.mpoC = rw.getChannel();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void check() throws NotActiveException {
            FileLock fileLock;
            try {
                fileLock = mpoC.tryLock();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (fileLock != null) {
                throw new NotActiveException("WriterNotActive=" + Utils.fromMPN(mpn));
            }
        }
    }

    QuickQueueReaderMulti(QuickQueueMulti qkq) {
        index = new BigBuffer("r", Utils.PAGE_SIZE, qkq.dir, "", Utils.EXT_INDEX);
        this.qkq = qkq;
    }

    public QuickQueueMessage setOffset(long offset) throws NotActiveException {
        if (offset < 0) {
            return null;
        } else {
            if ((offset >> 4) << 4 != offset) {
                throw new IllegalArgumentException("Offset=" + offset);
            }
            index.offset(offset);
            return next();
        }
    }

    private boolean mark = false;

    public QuickQueueMessage next() throws NotActiveException {
        byte b;
        if (mark) {
            b = index.getMark();
        } else {
            try {
                b = index.markGet(Utils.MNP_OFF);
                mark = true;
            } catch (BufferUnderflowException e) {
                return null;
            }
        }
        if (b > 0) {
            long offset = index.offset();
            long dataOffset = index.getLong();
            long stamp = index.getLong();
            int mpn = Utils.getMPN(stamp);
            Data mpd = data.get(mpn);
            if (mpd == null) data.put(mpn, mpd = new Data(mpn));
            if (Utils.notFlag(stamp)) {
                mpd.check();
                index.offset(offset);
                return null;
            }
            long len = Utils.getLength(stamp);
            QuickQueueMessage message =
                    mpd.message.reset(offset, dataOffset, len);
            mark = false;
            mpd.lastHit = (int) (System.currentTimeMillis() >> 10);
            return message;
        } else {
            return null;
        }
    }


    @Override
    public Iterator<QuickQueueMessage> iterator() {
        return new Iterator<QuickQueueMessage>() {
            private QuickQueueMessage quickQueueMessage;

            @Override
            public boolean hasNext() {
                try {
                    return (quickQueueMessage = QuickQueueReaderMulti.this.next()) != null;
                } catch (NotActiveException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public QuickQueueMessage next() {
                return quickQueueMessage;
            }
        };
    }

    public void close() {
        qkq.reads.remove(this.hashCode());
        index.clean();
        for (Data data : data.values()) {
            data.buffer.clean();
        }
    }
}
