package io.github.lapins2023.quickqueue;

import java.io.File;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class QuickQueueReaderMulti extends QuickQueueReader {
    static final Map<Integer, Data> DATA = new ConcurrentHashMap<>();


    private final BigBuffer index;
    private final QuickQueueMulti qkq;
    private final HashMap<Integer, Integer> data;


    static class Data {
        static final AtomicInteger ID = new AtomicInteger();
        final int id = ID.getAndIncrement();

        final BigBuffer buffer;
        private final int mpn;
        final QuickQueueMessage message;
        final FileChannel mpoC;
        int lastHit = (int) (System.currentTimeMillis() >> 10);

        public Data(File qkqDir, int mpn) throws NotActiveException {
            String name = Utils.fromMPN(mpn);
            File dir = new File(qkqDir, name);
            if (!dir.isDirectory()) {
                throw new NotActiveException("WriterNotFound=" + name);
            }
            buffer = new BigBuffer("r", Utils.PAGE_SIZE, dir, "", Utils.EXT_DATA);
            this.mpn = mpn;
            message = new QuickQueueMessage(buffer);
            try (RandomAccessFile rw = new RandomAccessFile(
                    new File(qkqDir, Utils.fromMPN(mpn) + Utils.EXT_MP), "r")) {
                this.mpoC = rw.getChannel();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            DATA.put(id, this);
        }

        public void check() throws NotActiveException {
            FileLock fileLock;
            try {
                fileLock = mpoC.tryLock();
                if (fileLock != null) {
                    fileLock.release();
                    throw new NotActiveException("WriterNotActive=" + Utils.fromMPN(mpn));
                }
            } catch (NotActiveException e) {
                throw e;
            } catch (OverlappingFileLockException ignored) {

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void close() {
            DATA.remove(id);
            buffer.clean();
        }
    }

    QuickQueueReaderMulti(QuickQueueMulti qkq) {
        index = new BigBuffer("r", Utils.PAGE_SIZE, qkq.dir, "", Utils.EXT_M_INDEX);
        this.qkq = qkq;
        this.data = new HashMap<>();
    }

    public QuickQueueMessage offset(long offset) throws NotActiveException {
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
            Integer mpdId = data.get(mpn);
            Data mpd;
            if (mpdId == null || (mpd = DATA.get(mpdId)) == null) {
                mpd = new Data(qkq.dir, mpn);
                data.put(mpn, mpd.id);
            }
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
    public QuickQueueMessage last() throws IOException {
        long lastIx = Utils.getLastIx(index, false);
        return offset(lastIx);
    }

    public void close() {
        index.clean();
        for (Integer mpdId : data.values()) {
            Data remove = DATA.remove(mpdId);
            if (remove != null) {
                remove.close();
            }
        }
    }

}
