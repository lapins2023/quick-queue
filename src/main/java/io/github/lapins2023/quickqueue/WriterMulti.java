package io.github.lapins2023.quickqueue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

class WriterMulti extends QuickQueueWriter {
    final BigBuffer index;
    final FileChannel mpoC;
    final MappedByteBuffer mpoM;
    final long mpoA1;
    private final long mpoAIx;
    private final long mpoA2;
    private final int mpn;
    private final RandomAccessFile raf;
    private boolean nextMpoA;

    public WriterMulti(QuickQueueMulti qkq) {
        super(new BigBuffer("rw", Utils.PAGE_SIZE, Utils.mkdir(new File(qkq.dir, Utils.fromMPN(qkq.mpn))), "", Utils.EXT_DATA));
        this.index = new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dir, "", Utils.EXT_M_INDEX);
        this.mpn = qkq.mpn;
        long o1;
        long o2;
        try {
            this.raf = new RandomAccessFile(new File(qkq.dir, Utils.fromMPN(qkq.mpn) + Utils.EXT_MP), "rw");
            this.mpoC = raf.getChannel();
            mpoC.lock();
            this.mpoM = (MappedByteBuffer) mpoC.map(FileChannel.MapMode.READ_WRITE, 0, 32)
                    .order(Utils.NativeByteOrder);
            this.mpoA1 = Utils.getAddress(this.mpoM);
            this.mpoA2 = mpoA1 + 8;
            this.mpoAIx = mpoA2 + 8;
            o1 = Utils.getLong(mpoA1);
            o2 = Utils.getLong(mpoA2);
            long dataEnding = Math.max(o1, o2);
            data.offset(dataEnding);
            nextMpoA = o1 <= o2;
            long lastIx = Utils.getLong(mpoAIx);
            if ((lastIx >> 4) << 4 != lastIx) {
                index.offset(lastIx);
                index.getLong();
                long stamp = index.getLong();
                if (Utils.notFlag(stamp) && Utils.getMPN(stamp) == qkq.mpn) {
                    //处理遗留写入
                    index.offset(lastIx);
                    index.putLong(Math.min(o1, o2))
                            .putLong(Utils.toStamp((int) Math.abs(o1 - o2), mpn, Utils.FLAG));
                }
            }
            long latestIx = Utils.getLastIx(qkq.dir, false);
            index.offset(latestIx < 0 ? 0 : latestIx + 16);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void force0() {
        mpoM.force();
        index.force();
    }


    @Override
    public long writeMessage0(int length) {
        if (nextMpoA) {
            Utils.putLong(mpoA2, data.offset());
            nextMpoA = false;
        } else {
            Utils.putLong(mpoA1, data.offset());
            nextMpoA = true;
        }
        long stamp = Utils.toStamp(length, mpn, (byte) 0);
        while (true) {
            long offset = index.offset();
            if (index.skip(Long.BYTES).compareAndSwapLong(0, stamp)) {
                Utils.putLong(mpoAIx, offset);
                index.offset(offset)
                        .putLong(begin)
                        .skip(Long.BYTES - 1)
                        .put(Utils.FLAG);
                return offset;
            } else {
                index.skip(Long.BYTES);
            }
        }
    }

    public void close() {
        force();
        index.clean();
        data.clean();
        try {
            raf.close();
        } catch (IOException ignored) {
        }
    }
}
