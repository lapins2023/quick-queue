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
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class QuickQueueReaderMulti extends QuickQueueReader {
    private final BigBuffer index;
    private final QuickQueueMulti qkq;
    private final HashMap<Integer, Data> data = new HashMap<>();


    static class Data {
        final BigBuffer buffer;
        final QuickQueueMessage message;
        final FileChannel mpoC;
        private final int mpn;
        private final RandomAccessFile raf;
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
            try {
                this.raf = new RandomAccessFile(
                        new File(qkqDir, Utils.fromMPN(mpn) + Utils.EXT_MP), "rw");
                this.mpoC = raf.getChannel();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
            } catch (OverlappingFileLockException e) {
//                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void close() {
            buffer.clean();
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    QuickQueueReaderMulti(QuickQueueMulti qkq) {
        index = new BigBuffer("r", Utils.PAGE_SIZE, qkq.dir, "", Utils.EXT_M_INDEX);
        this.qkq = qkq;
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

    private long t = -1;

    public QuickQueueMessage next() throws NotActiveException {
        byte b;
        if (t < 0) {
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
                t = System.currentTimeMillis();
                mark = false;
                return next();
            } else {
                return null;
            }
        }
        if (mark) {
            b = index.getMark();
        } else {
            try {
                b = index.markGet(Utils.FLAG_OFF);
                mark = true;
            } catch (BufferUnderflowException e) {
                return null;
            }
        }
        if (b != Utils.FLAG) {
            if (System.currentTimeMillis() - t > 1) {
                index.skip(Long.BYTES);
                int mpn = Utils.getMPN(index.getLong());
                Data mpd = data.get(mpn);
                if (mpd == null) {
                    data.put(mpn, (mpd = new Data(qkq.dir, mpn)));
                }
                mpd.check();
                index.skip(-2 * Long.BYTES);
            }
            return null;
        }
        long offset = index.offset();
        long dataOffset = index.getLong();
        long stamp = index.getLong();
        int mpn = Utils.getMPN(stamp);
        Data mpd = data.get(mpn);
        if (mpd == null) {
            data.put(mpn, (mpd = new Data(qkq.dir, mpn)));
        }
        long len = Utils.getLength(stamp);
        QuickQueueMessage message =
                mpd.message.reset(offset, dataOffset, len);
        mark = false;
        t = -1;
        mpd.lastHit = (int) (System.currentTimeMillis() >> 10);
        return message;
    }

    @Override
    public QuickQueueMessage last() throws IOException {
        long lastIx = Utils.getLastIx(index, false);
        return offset(lastIx);
    }

    public void clean() {
        int now = (int) (System.currentTimeMillis() >> 10);
        for (Integer key : new HashSet<>(data.keySet())) {
            Data d = data.get(key);
            if (now - d.lastHit > TimeUnit.MINUTES.toSeconds(5)) {
                data.remove(key).close();
            }
        }
    }

    public void close() {
        index.clean();
        for (Data d : data.values()) {
            d.close();
        }
    }

}
