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
    final long mpoA;

    public WriterMulti(QuickQueueMulti qkq) {
        super(new BigBuffer("rw", Utils.PAGE_SIZE, new File(qkq.dir, qkq.name), "", Utils.DATA_EXT));
        this.index = new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dir, "", Utils.INDEX_EXT);
        try {
            try (RandomAccessFile rw = new RandomAccessFile(new File(qkq.dir, qkq.name + ".lk"), "rw")) {
                this.mpoC = rw.getChannel();
                mpoC.lock();
                this.mpoM = (MappedByteBuffer) mpoC.map(FileChannel.MapMode.READ_WRITE, 0, 16).order(Utils.NativeByteOrder);
                this.mpoA = Utils.getAddress(this.mpoM);
                long dataEnding = Utils.getLong(mpoA);
                data.offset(dataEnding);
            }
            long lastIx = Utils.getLastIx(qkq.dir);
            index.offset(lastIx < 0 ? 0 : lastIx + 16);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        b1 = (byte) qkq.name.charAt(0);
        b2 = (byte) qkq.name.charAt(1);
        b3 = (byte) qkq.name.charAt(2);
    }

    @Override
    public void force0() {
        mpoM.force();
        index.force();
    }

    private final byte b1;
    private final byte b2;
    private final byte b3;

    @Override
    public long writeMessage0(int length) {
        return index.atomAppend(begin, Utils.toLong(length, b1, b2, b3, (byte) 0), Utils.FLAG)
                .offset() - 16;
    }

    public void close() {
        force();
        try {
            mpoC.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
