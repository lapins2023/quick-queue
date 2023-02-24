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
        n1 = (byte) qkq.name.charAt(0);
        n2 = (byte) qkq.name.charAt(1);
        n3 = (byte) qkq.name.charAt(2);
    }

    @Override
    public void force0() {
        mpoM.force();
        index.force();
    }

    private final byte n1;
    private final byte n2;
    private final byte n3;

    @Override
    public long writeMessage0(int length) {
        while (true) {
            index.atomAppend( begin, Utils.toLong(length, n1, n2, n3, (byte) 0), Utils.FLAG);
        }
//        Utils.putLong(mpoA, data.offset());
//        Utils.putLong(mpoA + 8, offset);
//        long offset_ = offset;
//        offset = offset + 16;
        return offset_;
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
