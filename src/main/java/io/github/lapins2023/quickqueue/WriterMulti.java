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

    public WriterMulti(QuickQueueMulti qkq) {
        super(new BigBuffer("rw", Utils.PAGE_SIZE, Utils.mkdir(new File(qkq.dir, qkq.name)), "", Utils.DATA_EXT));
        this.index = new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dir, "", Utils.INDEX_EXT);
        this.mpn = qkq.mpn;
        try {
            try (RandomAccessFile rw = new RandomAccessFile(new File(qkq.dir, qkq.name + ".mp"), "rw")) {
                this.mpoC = rw.getChannel();
                mpoC.lock();
                this.mpoM = (MappedByteBuffer) mpoC.map(FileChannel.MapMode.READ_WRITE, 0, 32)
                        .order(Utils.NativeByteOrder);
                this.mpoA1 = Utils.getAddress(this.mpoM);
                this.mpoA2 = mpoA1 + 8;
                this.mpoAIx = mpoA2 + 8;
                long dataEnding = Math.max(Utils.getLong(mpoA1), Utils.getLong(mpoA2));
                data.offset(dataEnding);
            }
            long lastIx = Utils.getLong(mpoAIx);
            index.offset(lastIx);
            index.getLong();
            long stamp = index.getLong();
            if (!Utils.isFlag(stamp) && Utils.getMPN(stamp) == qkq.mpn) {
                //处理遗留写入
                index.offset(lastIx);
                index.putLong(Math.min(Utils.getLong(mpoA1), Utils.getLong(mpoA2)))
                        .putLong(Utils.toLong((int) Math.abs(Utils.getLong(mpoA1) - Utils.getLong(mpoA2)), mpn, Utils.FLAG));
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
        Utils.putLong(mpoA1, data.offset());
        index.atomAppend(begin, Utils.toLong(length, mpn, (byte) 0), Utils.FLAG);
        long l = index.offset() - Utils.IX_MSG_LEN;
        Utils.putLong(mpoAIx, l);
        return l;
    }

    public void close() {
        force();
        try {
            mpoC.close();
        } catch (IOException ignored) {
        }
    }
}
